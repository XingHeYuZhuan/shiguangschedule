// TweakScheduleViewModel.kt
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first // 引入 first() 来获取 Flow 的当前快照值
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

/**
 * 调课页面 UI 状态。
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
 * 采用命令式状态管理模式，所有状态更新都由显式函数调用触发。
 */
class TweakScheduleViewModel(
    private val appSettingsRepository: AppSettingsRepository,
    private val courseTableRepository: CourseTableRepository
) : ViewModel() {

    // UI 暴露的状态
    private val _uiState = MutableStateFlow(TweakScheduleUiState())
    val uiState: StateFlow<TweakScheduleUiState> = _uiState.asStateFlow()

    // 内部 Flow 用于存储用户选择，但它们不再参与 combine 管道，仅作为稳定数据源
    private val _fromDate = MutableStateFlow(LocalDate.now())
    private val _toDate = MutableStateFlow(LocalDate.now())
    private val _selectedCourseTableByUser = MutableStateFlow<CourseTable?>(null)

    init {
        // ViewModel 初始化时，仅加载一次初始数据
        viewModelScope.launch {
            refreshUiState(isInitialLoad = true)
        }
    }

    /**
     * 【新的核心函数】
     * 显式地加载所有依赖数据（配置、课表、课程）并一次性更新 UI 状态。
     * 这完全取代了之前的 combine 逻辑。
     *
     * @param isInitialLoad 是否为初始化加载，用于确定默认选中的课表。
     */
    private suspend fun refreshUiState(isInitialLoad: Boolean = false) {
        // 1. 获取所有依赖数据的快照（不再持续监听）
        val settings = appSettingsRepository.getAppSettings().first()
        val allTables = courseTableRepository.getAllCourseTables().first()

        // 2. 确定当前选中的课表
        val selectedTable = if (isInitialLoad) {
            val defaultSelectedTable = if (settings.currentCourseTableId != null) {
                allTables.find { it.id == settings.currentCourseTableId }
            } else {
                allTables.firstOrNull()
            }
            _selectedCourseTableByUser.value = defaultSelectedTable
            defaultSelectedTable
        } else {
            _selectedCourseTableByUser.value
        }

        // 3. 确定日期，使用内部 Flow 的当前值 (用户设置的稳定值)
        val currentFromDate = _fromDate.value
        val currentToDate = _toDate.value

        val semesterStartDate = try {
            settings.semesterStartDate?.let { LocalDate.parse(it) }
        } catch (e: DateTimeParseException) {
            null
        }
        val isSemesterSet = semesterStartDate != null

        var fromCourses = emptyList<CourseWithWeeks>()
        var toCourses = emptyList<CourseWithWeeks>()

        // 4. 查询课程
        if (isSemesterSet && selectedTable != null) {
            val fromWeekNumber = ChronoUnit.WEEKS.between(semesterStartDate, currentFromDate).toInt() + 1
            val fromDay = currentFromDate.dayOfWeek.value
            val toWeekNumber = ChronoUnit.WEEKS.between(semesterStartDate, currentToDate).toInt() + 1
            val toDay = currentToDate.dayOfWeek.value

            // 显式获取课程数据快照
            fromCourses = courseTableRepository.getCoursesForDay(selectedTable.id, fromWeekNumber, fromDay).first()
            toCourses = courseTableRepository.getCoursesForDay(selectedTable.id, toWeekNumber, toDay).first()
        }

        // 5. 一次性原子更新 UI 状态
        _uiState.update {
            it.copy(
                allCourseTables = allTables,
                isSemesterSet = isSemesterSet,
                selectedCourseTable = selectedTable,
                fromDate = currentFromDate, // 使用稳定日期
                toDate = currentToDate,     // 使用稳定日期
                fromCourses = fromCourses,
                toCourses = toCourses,
                semesterStartDate = semesterStartDate,
                isLoading = false
            )
        }
    }


    // 处理用户交互的函数：更新内部 Flow 后，显式调用 refreshUiState()
    fun onCourseTableSelected(courseTable: CourseTable) {
        _selectedCourseTableByUser.value = courseTable
        viewModelScope.launch {
            refreshUiState()
        }
    }

    fun onFromDateSelected(date: LocalDate) {
        _fromDate.value = date
        viewModelScope.launch {
            refreshUiState()
        }
    }

    fun onToDateSelected(date: LocalDate) {
        _toDate.value = date
        viewModelScope.launch {
            refreshUiState()
        }
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

                // 使用 UI 状态中稳定的日期快照进行计算
                val currentFromDate = state.fromDate
                val currentToDate = state.toDate

                try {
                    val fromWeekNumber = ChronoUnit.WEEKS.between(state.semesterStartDate, currentFromDate).toInt() + 1
                    val fromDay = currentFromDate.dayOfWeek.value
                    val toWeekNumber = ChronoUnit.WEEKS.between(state.semesterStartDate, currentToDate).toInt() + 1
                    val toDay = currentToDate.dayOfWeek.value

                    // 1. 执行数据库操作
                    courseTableRepository.moveCoursesOnDate(
                        courseTableId = state.selectedCourseTable.id,
                        fromWeekNumber = fromWeekNumber,
                        fromDay = fromDay,
                        toWeekNumber = toWeekNumber,
                        toDay = toDay
                    )

                    // 2. 数据库更新后，立即手动刷新 UI 状态：
                    //    这将读取新的课程数据，并使用稳定日期更新 _uiState，日期绝不会被重置。
                    refreshUiState()

                    // 3. 更新成功消息
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "调课成功！"
                        )
                    }

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
        _uiState.update { currentState ->
            currentState.copy(errorMessage = null, successMessage = null)
        }
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