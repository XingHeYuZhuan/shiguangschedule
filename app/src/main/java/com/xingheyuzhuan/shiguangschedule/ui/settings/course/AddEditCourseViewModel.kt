package com.xingheyuzhuan.shiguangschedule.ui.settings.course

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.xingheyuzhuan.shiguangschedule.data.db.main.Course
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseTableRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.TimeSlotRepository
import com.xingheyuzhuan.shiguangschedule.MyApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class AddEditCourseViewModel(
    private val courseTableRepository: CourseTableRepository,
    private val timeSlotRepository: TimeSlotRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val courseId: String?,
    private val initialDay: Int?,
    private val initialSection: Int?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditCourseUiState())
    val uiState: StateFlow<AddEditCourseUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        viewModelScope.launch {
            // 获取 appSettings 的数据流
            val appSettingsFlow = appSettingsRepository.getAppSettings()

            // 使用 flatMapLatest 监听 appSettings 的变化，并根据当前课表 ID 获取对应的时间段
            @OptIn(ExperimentalCoroutinesApi::class)
            val timeSlotsFlow = appSettingsFlow.flatMapLatest { settings ->
                val courseTableId = settings.currentCourseTableId
                if (courseTableId != null) {
                    timeSlotRepository.getTimeSlotsByCourseTableId(courseTableId)
                } else {
                    // 如果没有设置课表 ID，则返回空列表
                    flowOf(emptyList())
                }
            }

            @OptIn(ExperimentalCoroutinesApi::class)
            val courseConfigFlow = appSettingsFlow.flatMapLatest { settings ->
                val courseTableId = settings.currentCourseTableId
                if (courseTableId != null) {
                    appSettingsRepository.getCourseTableConfigFlow(courseTableId)
                } else {
                    // 如果没有设置课表 ID，则返回 null
                    flowOf(null)
                }
            }

            combine(
                timeSlotsFlow,
                appSettingsFlow,
                courseConfigFlow,
                if (courseId != null) {
                    courseTableRepository.getCoursesWithWeeksByTableId(appSettingsRepository.getAppSettings().first().currentCourseTableId.orEmpty())
                        .map { courses ->
                            courses.find { it.course.id == courseId }
                        }
                } else {
                    MutableStateFlow(null)
                }
            ) { timeSlots, appSettings, courseConfig, courseWithWeeks ->
                _uiState.update { currentState ->

                    val totalWeeks = courseConfig?.semesterTotalWeeks ?: 20

                    val course = currentState.course ?: if (courseId == null) {
                        Course(
                            id = UUID.randomUUID().toString(),
                            courseTableId = appSettings.currentCourseTableId.orEmpty(),
                            name = "", teacher = "", position = "",
                            day = initialDay ?: 1,
                            startSection = initialSection ?: 1,
                            endSection = initialSection ?: 1,
                            colorInt = getRandomColor().toArgb()
                        )
                    } else {
                        courseWithWeeks?.course
                    }

                    val weeks = currentState.weeks.takeIf { it.isNotEmpty() } ?: if(courseId == null) {
                        (1..totalWeeks).toSet()
                    } else {
                        courseWithWeeks?.weeks?.map { it.weekNumber }?.toSet() ?: emptySet()
                    }

                    currentState.copy(
                        isEditing = courseId != null,
                        course = course,
                        name = course?.name.orEmpty(),
                        teacher = course?.teacher.orEmpty(),
                        position = course?.position.orEmpty(),
                        day = course?.day ?: 1,
                        startSection = course?.startSection ?: 1,
                        endSection = course?.endSection ?: 1,
                        color = Color(course?.colorInt ?: Color.Unspecified.toArgb()),
                        weeks = weeks,
                        timeSlots = timeSlots,
                        currentCourseTableId = appSettings.currentCourseTableId,
                        semesterTotalWeeks = totalWeeks // 【修改：使用 totalWeeks 代替 appSettings.semesterTotalWeeks】
                    )
                }
            }.collect()
        }
    }

    // UI 事件处理函数
    fun onNameChange(name: String) { _uiState.update { it.copy(name = name) } }
    fun onTeacherChange(teacher: String) { _uiState.update { it.copy(teacher = teacher) } }
    fun onPositionChange(position: String) { _uiState.update { it.copy(position = position) } }
    fun onDayChange(day: Int) { _uiState.update { it.copy(day = day) } }

    fun onStartSectionChange(startSection: Int) {
        _uiState.update { it.copy(startSection = startSection) }
    }

    fun onEndSectionChange(endSection: Int) { _uiState.update { it.copy(endSection = endSection) } }

    fun onWeeksChange(newWeeks: Set<Int>) {
        _uiState.update { it.copy(weeks = newWeeks) }
    }
    fun onColorChange(color: Color) { _uiState.update { it.copy(color = color) } }

    // 统一的保存函数
    fun onSave() {
        viewModelScope.launch {
            val state = uiState.value
            val courseToSave = state.course?.copy(
                name = state.name,
                teacher = state.teacher,
                position = state.position,
                day = state.day,
                startSection = state.startSection,
                endSection = state.endSection,
                colorInt = state.color.toArgb(),
                courseTableId = state.currentCourseTableId.orEmpty()
            )
            if (courseToSave != null) {
                courseTableRepository.upsertCourse(courseToSave, state.weeks.toList())
                _uiEvent.send(UiEvent.SaveSuccess) // 发送保存成功事件
            }
        }
    }

    // 统一的删除函数
    fun onDelete() {
        viewModelScope.launch {
            uiState.value.course?.let { course ->
                courseTableRepository.deleteCourse(course)
                _uiEvent.send(UiEvent.DeleteSuccess) // 发送删除成功事件
            }
        }
    }

    // 统一的取消函数
    fun onCancel() {
        viewModelScope.launch {
            _uiEvent.send(UiEvent.Cancel) // 发送取消事件
        }
    }

    private fun getRandomColor(): Color {
        val colors = listOf(
            Color(0xFFF44336), Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7),
            Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF03A9F4), Color(0xFF00BCD4),
            Color(0xFF009688), Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFCDDC39),
            Color(0xFFFFEB3B), Color(0xFFFFC107), Color(0xFFFF9800), Color(0xFFFF5722)
        )
        return colors[Random.nextInt(colors.size)]
    }

    companion object {
        fun Factory(courseId: String?, initialDay: Int?, initialSection: Int?): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                    if (modelClass.isAssignableFrom(AddEditCourseViewModel::class.java)) {
                        val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as MyApplication
                        return AddEditCourseViewModel(
                            courseTableRepository = application.courseTableRepository,
                            timeSlotRepository = application.timeSlotRepository,
                            appSettingsRepository = application.appSettingsRepository,
                            courseId = courseId,
                            initialDay = initialDay,
                            initialSection = initialSection
                        ) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
    }
}

// 用于通知 UI 的事件
sealed interface UiEvent {
    object SaveSuccess : UiEvent
    object DeleteSuccess : UiEvent
    object Cancel : UiEvent
}

data class AddEditCourseUiState(
    val isEditing: Boolean = false,
    val course: Course? = null,
    val name: String = "",
    val teacher: String = "",
    val position: String = "",
    val day: Int = 1,
    val startSection: Int = 1,
    val endSection: Int = 2,
    val color: Color = Color.Unspecified,
    val weeks: Set<Int> = emptySet(),
    val timeSlots: List<TimeSlot> = emptyList(),
    val currentCourseTableId: String? = null,
    val semesterTotalWeeks: Int = 20,
    val courseColors: List<Color> = listOf(
        Color(0xFFF44336), Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7),
        Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF03A9F4), Color(0xFF00BCD4),
        Color(0xFF009688), Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFCDDC39),
        Color(0xFFFFEB3B), Color(0xFFFFC107), Color(0xFFFF9800), Color(0xFFFF5722)
    )
)