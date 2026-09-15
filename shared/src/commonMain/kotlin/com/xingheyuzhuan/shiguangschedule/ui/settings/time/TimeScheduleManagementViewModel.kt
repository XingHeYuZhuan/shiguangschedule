package com.xingheyuzhuan.shiguangschedule.ui.settings.time

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTimeBinding.TargetType
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeTable
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeTableCombo
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeTableComboRule
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.TimeScheduleRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// UI 数据模型定义

enum class ScheduleType {
    EXCLUSIVE, // 课表专属作息
    PUBLIC,    // 公共作息
    COMBO      // 组合作息
}

data class TimeScheduleItemUiModel(
    val id: String,
    val name: String?,
    val type: ScheduleType,
    val isSelected: Boolean,
    val isEditable: Boolean = true,
    val createdAtFormatted: String? = null
)

// 作息方案管理主页 ViewModel

data class ScheduleManagementUiState(
    val currentCourseTableId: String = "",
    val items: List<TimeScheduleItemUiModel> = emptyList(),
    val isDataLoaded: Boolean = false,
    val errorMessage: String? = null
)

private fun formatTimestamp(timestamp: Long?): String? {
    if (timestamp == null || timestamp <= 0) return null
    return runCatching {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val year = localDateTime.year
        val month = localDateTime.month.number.toString().padStart(2, '0')
        val day = localDateTime.day.toString().padStart(2, '0')
        val hour = localDateTime.hour.toString().padStart(2, '0')
        val minute = localDateTime.minute.toString().padStart(2, '0')

        "$year-$month-$day $hour:$minute"
    }.getOrNull()
}

@KoinViewModel
class TimeScheduleManagementViewModel(
    private val appSettingsRepository: AppSettingsRepository,
    private val timeScheduleRepository: TimeScheduleRepository
) : ViewModel() {

    private val refreshTrigger = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ScheduleManagementUiState> = appSettingsRepository.getAppSettings()
        .flatMapLatest { settings ->
            val tableId = settings.currentCourseTableId

            combine(
                refreshTrigger,
                timeScheduleRepository.getAllPublicTimeTables(),
                timeScheduleRepository.getAllCombos()
            ) { _, publicTables, combos ->
                val binding = runCatching {
                    timeScheduleRepository.getBinding(tableId)
                }.getOrNull()

                val items = mutableListOf<TimeScheduleItemUiModel>()

                val isExclusiveSelected = binding == null ||
                        (binding.targetType == TargetType.SINGLE && binding.targetId == tableId)

                items.add(
                    TimeScheduleItemUiModel(
                        id = tableId,
                        name = null,
                        type = ScheduleType.EXCLUSIVE,
                        isSelected = isExclusiveSelected,
                        isEditable = true,
                        createdAtFormatted = null
                    )
                )

                publicTables.forEach { publicTable ->
                    val isSelected = binding?.targetType == TargetType.SINGLE && binding.targetId == publicTable.id
                    items.add(
                        TimeScheduleItemUiModel(
                            id = publicTable.id,
                            name = publicTable.name,
                            type = ScheduleType.PUBLIC,
                            isSelected = isSelected,
                            isEditable = true,
                            createdAtFormatted = formatTimestamp(publicTable.createdAt)
                        )
                    )
                }

                combos.forEach { combo ->
                    val isSelected = binding?.targetType == TargetType.COMBO && binding.targetId == combo.id
                    items.add(
                        TimeScheduleItemUiModel(
                            id = combo.id,
                            name = combo.name,
                            type = ScheduleType.COMBO,
                            isSelected = isSelected,
                            isEditable = true,
                            createdAtFormatted = formatTimestamp(combo.createdAt)
                        )
                    )
                }

                ScheduleManagementUiState(
                    currentCourseTableId = tableId,
                    items = items,
                    isDataLoaded = true
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ScheduleManagementUiState()
        )

    fun bindTimeSchedule(item: TimeScheduleItemUiModel) {
        viewModelScope.launch {
            val tableId = uiState.value.currentCourseTableId
            if (tableId.isBlank()) return@launch

            val targetType = if (item.type == ScheduleType.COMBO) TargetType.COMBO else TargetType.SINGLE
            val targetId = if (item.type == ScheduleType.EXCLUSIVE) tableId else item.id

            runCatching {
                timeScheduleRepository.bindCourseTableToTimeSchedule(tableId, targetType, targetId)
            }.onSuccess {
                refreshTrigger.value += 1
            }
        }
    }

    fun deleteSchedules(selectedKeys: Set<String>) {
        viewModelScope.launch {
            if (selectedKeys.isEmpty()) return@launch

            val currentState = uiState.value
            val itemsToDelete = currentState.items.filter { item ->
                val key = "${item.type.name}_${item.id}"
                key in selectedKeys && item.type != ScheduleType.EXCLUSIVE
            }

            runCatching {
                itemsToDelete.forEach { item ->
                    when (item.type) {
                        ScheduleType.PUBLIC -> timeScheduleRepository.deletePublicTimeTable(item.id)
                        ScheduleType.COMBO -> timeScheduleRepository.deleteCombo(item.id)
                        ScheduleType.EXCLUSIVE -> {}
                    }
                }
            }.onSuccess {
                refreshTrigger.value += 1
            }
        }
    }
}

// 单一/公共/专属作息编辑 ViewModel

data class SingleScheduleEditUiState(
    val name: String = "",
    val isPublic: Boolean = false,
    val slots: List<TimeSlot> = emptyList(),
    val defaultClassDuration: Int = 45,
    val defaultBreakDuration: Int = 10,
    val isDataLoaded: Boolean = false,
    val isSaved: Boolean = false,
    val errorMsg: String? = null
)

@OptIn(ExperimentalUuidApi::class)
@KoinViewModel
class SingleScheduleEditViewModel(
    @InjectedParam private val rawTableId: String?,
    @InjectedParam val isPublic: Boolean,
    @InjectedParam private val rawCopyFromId: String? = null,
    private val appSettingsRepository: AppSettingsRepository,
    private val timeScheduleRepository: TimeScheduleRepository
) : ViewModel() {

    private val parsedTableId: String? = rawTableId?.takeIf { it.isNotBlank() && it != "null" }
    private val copyFromId: String? = rawCopyFromId?.takeIf { it.isNotBlank() && it != "null" }

    private val _uiState = MutableStateFlow(SingleScheduleEditUiState(isPublic = isPublic))
    val uiState: StateFlow<SingleScheduleEditUiState> = _uiState.asStateFlow()

    private var currentTargetId: String = ""

    init {
        viewModelScope.launch {
            runCatching {
                val currentCourseTableId = appSettingsRepository.getAppSettings().first().currentCourseTableId

                currentTargetId = if (isPublic) {
                    if (copyFromId != null) {
                        Uuid.random().toString()
                    } else parsedTableId ?: Uuid.random().toString()
                } else {
                    parsedTableId ?: currentCourseTableId
                }

                check(currentTargetId.isNotBlank()) { "目标作息表ID不能为空" }

                val sourceTimeTableId = copyFromId ?: parsedTableId ?: currentCourseTableId

                val rawSlots = timeScheduleRepository.getTimeSlotsByTimeTableId(sourceTimeTableId).first()
                val slots = rawSlots.map { it.copy(timeTableId = currentTargetId) }

                val timeTable = timeScheduleRepository.getTimeTableById(sourceTimeTableId).first()
                val classDuration = timeTable?.defaultClassDuration ?: 45
                val breakDuration = timeTable?.defaultBreakDuration ?: 10

                val tableName = if (copyFromId != null) {
                    val baseName = timeTable?.name.orEmpty()
                    if (baseName.isNotBlank()) "$baseName.1" else ""
                } else {
                    timeTable?.name.orEmpty()
                }

                _uiState.update {
                    it.copy(
                        name = tableName,
                        slots = slots,
                        defaultClassDuration = classDuration,
                        defaultBreakDuration = breakDuration,
                        isDataLoaded = true
                    )
                }
            }.onFailure { e ->
                e.printStackTrace()
                _uiState.update { it.copy(errorMsg = e.message, isDataLoaded = true) }
            }
        }
    }

    fun onNameChange(newName: String) {
        _uiState.update { it.copy(name = newName) }
    }

    fun onDefaultDurationChange(classDuration: Int, breakDuration: Int) {
        _uiState.update {
            it.copy(
                defaultClassDuration = classDuration,
                defaultBreakDuration = breakDuration
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMsg = null) }
    }

    fun saveWithData(
        newName: String,
        classDuration: Int,
        breakDuration: Int,
        slots: List<TimeSlot>
    ) {
        viewModelScope.launch {
            val currentCourseTableId = appSettingsRepository.getAppSettings().first().currentCourseTableId
            val slotsToSave = slots.map { it.copy(timeTableId = currentTargetId) }

            val timeTable = TimeTable(
                id = currentTargetId,
                name = if (isPublic) newName.trim().ifBlank { null } else null,
                createdAt = Clock.System.now().toEpochMilliseconds(),
                defaultClassDuration = classDuration,
                defaultBreakDuration = breakDuration
            )

            runCatching {
                if (isPublic) {
                    timeScheduleRepository.savePublicTimeTable(timeTable, slotsToSave)
                } else {
                    timeScheduleRepository.saveExclusiveTimeTable(timeTable, slotsToSave)

                    timeScheduleRepository.bindCourseTableToTimeSchedule(
                        courseTableId = currentCourseTableId,
                        targetType = TargetType.SINGLE,
                        targetId = currentCourseTableId
                    )
                }
            }.onSuccess {
                _uiState.update { it.copy(isSaved = true) }
            }.onFailure { e ->
                e.printStackTrace()
                _uiState.update { it.copy(errorMsg = e.message ?: "保存失败，请重试") }
            }
        }
    }
}

// 组合作息编辑 ViewModel

data class ComboScheduleEditUiState(
    val name: String = "",
    val baseTimeTableId: String? = null,
    val rules: List<TimeTableComboRule> = emptyList(),
    val availablePublicTables: List<TimeTable> = emptyList(),
    val isDataLoaded: Boolean = false,
    val isSaved: Boolean = false,
    val errorMsg: String? = null
)

@OptIn(ExperimentalUuidApi::class)
@KoinViewModel
class ComboScheduleEditViewModel(
    @InjectedParam private val rawComboId: String?,
    @InjectedParam private val rawCopyFromId: String? = null,
    private val timeScheduleRepository: TimeScheduleRepository
) : ViewModel() {

    private val comboId: String? = rawComboId?.takeIf { it.isNotBlank() && it != "null" }
    private val copyFromId: String? = rawCopyFromId?.takeIf { it.isNotBlank() && it != "null" }

    private val currentComboId: String = if (copyFromId != null) Uuid.random().toString() else (comboId ?: Uuid.random().toString())

    private val _uiState = MutableStateFlow(ComboScheduleEditUiState())
    val uiState: StateFlow<ComboScheduleEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                val publicTables = timeScheduleRepository.getAllPublicTimeTables().first()

                val sourceComboId = copyFromId ?: comboId

                if (sourceComboId != null) {
                    val combos = timeScheduleRepository.getAllCombos().first()
                    val sourceCombo = combos.find { it.id == sourceComboId }
                    val dbRules = timeScheduleRepository.getComboRules(sourceComboId).first()

                    val rulesToPreFill = dbRules.map { rule ->
                        rule.copy(
                            id = if (copyFromId != null) Uuid.random().toString() else rule.id,
                            comboId = currentComboId
                        )
                    }

                    val baseName = sourceCombo?.name.orEmpty()
                    val initialName = if (copyFromId != null) {
                        if (baseName.isNotBlank()) "$baseName.1" else ""
                    } else {
                        baseName
                    }

                    _uiState.update {
                        it.copy(
                            name = initialName,
                            baseTimeTableId = sourceCombo?.baseTimeTableId,
                            rules = rulesToPreFill,
                            availablePublicTables = publicTables,
                            isDataLoaded = true
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            baseTimeTableId = null,
                            availablePublicTables = publicTables,
                            isDataLoaded = true
                        )
                    }
                }
            }.onFailure { e ->
                _uiState.update { it.copy(errorMsg = e.message, isDataLoaded = true) }
            }
        }
    }

    fun onNameChange(newName: String) {
        _uiState.update { it.copy(name = newName) }
    }

    fun onBaseTableChange(baseId: String?) {
        _uiState.update { it.copy(baseTimeTableId = baseId) }
    }

    fun addRule() {
        val defaultTable = uiState.value.availablePublicTables.firstOrNull() ?: return
        val currentDateStr = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        _uiState.update { state ->
            val newRule = TimeTableComboRule(
                id = Uuid.random().toString(),
                comboId = currentComboId,
                targetTimeTableId = defaultTable.id,
                startDate = currentDateStr,
                endDate = currentDateStr
            )
            state.copy(rules = state.rules + newRule)
        }
    }

    fun updateRule(index: Int, updatedRule: TimeTableComboRule) {
        _uiState.update { state ->
            val list = state.rules.toMutableList().apply {
                if (index in indices) this[index] = updatedRule
            }
            state.copy(rules = list)
        }
    }

    fun removeRule(index: Int) {
        _uiState.update { state ->
            val list = state.rules.toMutableList().apply {
                if (index in indices) removeAt(index)
            }
            state.copy(rules = list)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMsg = null) }
    }

    fun save() {
        viewModelScope.launch {
            val state = uiState.value

            val combo = TimeTableCombo(
                id = currentComboId,
                name = state.name.trim(),
                baseTimeTableId = state.baseTimeTableId,
                createdAt = Clock.System.now().toEpochMilliseconds()
            )
            val rulesToSave = state.rules.map { t -> t.copy(comboId = currentComboId) }

            runCatching {
                timeScheduleRepository.saveTimeTableCombo(combo, rulesToSave)
            }.onSuccess {
                _uiState.update { it.copy(isSaved = true) }
            }.onFailure { e ->
                _uiState.update { it.copy(errorMsg = e.message) }
            }
        }
    }
}