package com.xingheyuzhuan.shiguangschedule.ui.settings.tweaks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.xingheyuzhuan.shiguangschedule.MyApplication
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTable
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseTableRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

/**
 * 调课页面 UI 状态。
 * 包含所有需要展示给用户的数据和业务逻辑所需的数据快照。
 */
data class TweakScheduleUiState(
    // UI 显示所需的数据
    val allCourseTables: List<CourseTable> = emptyList(),
    val selectedCourseTable: CourseTable? = null,
    val fromDate: LocalDate = LocalDate.now(),
    val toDate: LocalDate = LocalDate.now(),
    val fromCourses: List<CourseWithWeeks> = emptyList(),
    val toCourses: List<CourseWithWeeks> = emptyList(),

    // 业务逻辑和状态管理所需的数据
    val isSemesterSet: Boolean = false,
    val semesterStartDate: LocalDate? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * 课程调动页面的 ViewModel。
 * 负责处理调动逻辑，包括日期转换和调用 Repository。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TweakScheduleViewModel(
    private val appSettingsRepository: AppSettingsRepository,
    private val courseTableRepository: CourseTableRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TweakScheduleUiState())
    val uiState: StateFlow<TweakScheduleUiState> = _uiState.asStateFlow()

    private val _fromDate = MutableStateFlow(LocalDate.now())
    private val _toDate = MutableStateFlow(LocalDate.now())
    private val _selectedCourseTableByUser = MutableStateFlow<CourseTable?>(null)

    init {
        viewModelScope.launch {
            combine(
                appSettingsRepository.getAppSettings(),
                courseTableRepository.getAllCourseTables(),
                _selectedCourseTableByUser,
                _fromDate,
                _toDate
            ) { settings, allTables, selectedTableByUser, fromDate, toDate ->
                val defaultSelectedTable = if (settings.currentCourseTableId != null) {
                    allTables.find { it.id == settings.currentCourseTableId }
                } else {
                    allTables.firstOrNull()
                }
                val selectedTable = selectedTableByUser ?: defaultSelectedTable

                val semesterStartDate = try {
                    settings.semesterStartDate?.let { LocalDate.parse(it) }
                } catch (e: DateTimeParseException) {
                    null
                }
                val isSemesterSet = semesterStartDate != null

                // 如果学期未设置，则不进行课程查询
                if (!isSemesterSet || selectedTable == null) {
                    _uiState.value = TweakScheduleUiState(
                        allCourseTables = allTables,
                        isSemesterSet = isSemesterSet,
                        selectedCourseTable = selectedTable,
                        fromDate = fromDate,
                        toDate = toDate,
                        semesterStartDate = semesterStartDate
                    )
                } else {
                    val fromWeekNumber = ChronoUnit.WEEKS.between(semesterStartDate, fromDate).toInt() + 1
                    val fromDay = fromDate.dayOfWeek.value
                    val toWeekNumber = ChronoUnit.WEEKS.between(semesterStartDate, toDate).toInt() + 1
                    val toDay = toDate.dayOfWeek.value

                    combine(
                        courseTableRepository.getCoursesForDay(selectedTable.id, fromWeekNumber, fromDay),
                        courseTableRepository.getCoursesForDay(selectedTable.id, toWeekNumber, toDay)
                    ) { fromCourses, toCourses ->
                        _uiState.value = TweakScheduleUiState(
                            allCourseTables = allTables,
                            isSemesterSet = isSemesterSet,
                            selectedCourseTable = selectedTable,
                            fromDate = fromDate,
                            toDate = toDate,
                            fromCourses = fromCourses,
                            toCourses = toCourses,
                            semesterStartDate = semesterStartDate
                        )
                    }.launchIn(viewModelScope)
                }
            }.launchIn(viewModelScope)
        }
    }

    // 处理用户交互的函数
    fun onCourseTableSelected(courseTable: CourseTable) {
        _selectedCourseTableByUser.value = courseTable
    }

    fun onFromDateSelected(date: LocalDate) {
        _fromDate.value = date
    }

    fun onToDateSelected(date: LocalDate) {
        _toDate.value = date
    }

    /**
     * 执行课程调动操作。
     */
    fun moveCourses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            _uiState.value.let { state ->
                if (state.selectedCourseTable == null || !state.isSemesterSet || state.semesterStartDate == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "请先选择课表并设置学期开始日期") }
                    return@launch
                }

                if (state.fromDate == state.toDate) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "被调整日期和调整到日期不能是同一天") }
                    return@launch
                }

                try {
                    val fromWeekNumber = ChronoUnit.WEEKS.between(state.semesterStartDate, state.fromDate).toInt() + 1
                    val fromDay = state.fromDate.dayOfWeek.value
                    val toWeekNumber = ChronoUnit.WEEKS.between(state.semesterStartDate, state.toDate).toInt() + 1
                    val toDay = state.toDate.dayOfWeek.value

                    courseTableRepository.moveCoursesOnDate(
                        courseTableId = state.selectedCourseTable.id,
                        fromWeekNumber = fromWeekNumber,
                        fromDay = fromDay,
                        toWeekNumber = toWeekNumber,
                        toDay = toDay
                    )

                    _uiState.update { it.copy(isLoading = false, successMessage = "调课成功！") }
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "调课失败：${e.message}") }
                }
            }
        }
    }

    /**
     * 重置消息状态，用于UI显示Toast后清除。
     */
    fun resetMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}

/**
 * TweakScheduleViewModel 的工厂类，用于依赖注入。
 */
object TweakScheduleViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])

        if (modelClass.isAssignableFrom(TweakScheduleViewModel::class.java)) {
            val myApplication = application as MyApplication
            @Suppress("UNCHECKED_CAST")
            return TweakScheduleViewModel(
                appSettingsRepository = myApplication.appSettingsRepository,
                courseTableRepository = myApplication.courseTableRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}