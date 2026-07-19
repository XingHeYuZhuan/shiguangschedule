package com.xingheyuzhuan.shiguangschedule.ui.settings.quickactions.tweaks

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTable
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseTableRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
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
    val tweakMode: CourseTableRepository.TweakMode = CourseTableRepository.TweakMode.MERGE,

    // 业务逻辑和状态管理所需的数据
    val isSemesterSet: Boolean = false,
    val semesterStartDate: LocalDate? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * 课程调动页面的 ViewModel。
 */
@KoinViewModel
class TweakScheduleViewModel(
    private val appSettingsRepository: AppSettingsRepository,
    private val courseTableRepository: CourseTableRepository,
    private val application: Application
) : ViewModel() {

    // UI 暴露的状态
    private val _uiState = MutableStateFlow(TweakScheduleUiState())
    val uiState: StateFlow<TweakScheduleUiState> = _uiState.asStateFlow()

    // 内部存储用户选择的私有 Flow
    private val _fromDate = MutableStateFlow(LocalDate.now())
    private val _toDate = MutableStateFlow(LocalDate.now())
    private val _selectedCourseTableByUser = MutableStateFlow<CourseTable?>(null)

    init {
        viewModelScope.launch {
            refreshUiState(isInitialLoad = true)
        }
    }

    /**
     * 刷新 UI 状态：加载配置、课表以及预览区域的课程。
     */
    private suspend fun refreshUiState(isInitialLoad: Boolean = false) {
        val settings = appSettingsRepository.getAppSettings().first()
        val allTables = courseTableRepository.getAllCourseTables().first()

        val selectedTable = if (isInitialLoad) {
            val defaultSelectedTable = allTables.find { it.id == settings.currentCourseTableId }
            _selectedCourseTableByUser.value = defaultSelectedTable
            defaultSelectedTable
        } else {
            _selectedCourseTableByUser.value
        }

        val currentFromDate = _fromDate.value
        val currentToDate = _toDate.value

        val currentTableId = selectedTable?.id
        val courseConfig = if (currentTableId != null) {
            appSettingsRepository.getCourseConfigOnce(currentTableId)
        } else {
            null
        }

        val semesterStartDateString = courseConfig?.semesterStartDate
        val semesterStartDate: LocalDate? = try {
            semesterStartDateString?.let { LocalDate.parse(it) }
        } catch (e: DateTimeParseException) {
            null
        }
        val isSemesterSet = semesterStartDate != null

        var fromCourses = emptyList<CourseWithWeeks>()
        var toCourses = emptyList<CourseWithWeeks>()

        if (isSemesterSet && selectedTable != null) {
            val fromWeekNumber = ChronoUnit.WEEKS.between(semesterStartDate, currentFromDate).toInt() + 1
            val fromDay = currentFromDate.dayOfWeek.value
            val toWeekNumber = ChronoUnit.WEEKS.between(semesterStartDate, currentToDate).toInt() + 1
            val toDay = currentToDate.dayOfWeek.value

            fromCourses = courseTableRepository.getCoursesForDay(selectedTable.id, fromWeekNumber, fromDay).first()
            toCourses = courseTableRepository.getCoursesForDay(selectedTable.id, toWeekNumber, toDay).first()
        }

        _uiState.update {
            it.copy(
                allCourseTables = allTables,
                isSemesterSet = isSemesterSet,
                selectedCourseTable = selectedTable,
                fromDate = currentFromDate,
                toDate = currentToDate,
                fromCourses = fromCourses,
                toCourses = toCourses,
                semesterStartDate = semesterStartDate,
                isLoading = false
            )
        }
    }

    // 响应 UI 层更改调课模式
    fun onTweakModeChanged(mode: CourseTableRepository.TweakMode) {
        _uiState.update { it.copy(tweakMode = mode) }
    }

    fun onCourseTableSelected(courseTable: CourseTable) {
        _selectedCourseTableByUser.value = courseTable
        viewModelScope.launch { refreshUiState() }
    }

    fun onFromDateSelected(date: LocalDate) {
        _fromDate.value = date
        viewModelScope.launch { refreshUiState() }
    }

    fun onToDateSelected(date: LocalDate) {
        _toDate.value = date
        viewModelScope.launch { refreshUiState() }
    }

    /**
     * 执行课程调动操作。
     */
    fun moveCourses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

            val state = _uiState.value
            val resources = application.resources

            if (state.selectedCourseTable == null || state.semesterStartDate == null) {
                val errorMsg = resources.getString(R.string.error_tweak_no_table_or_semester)
                _uiState.update { it.copy(isLoading = false, errorMessage = errorMsg) }
                return@launch
            }

            if (state.fromDate == state.toDate) {
                val errorMsg = resources.getString(R.string.error_tweak_same_day)
                _uiState.update { it.copy(isLoading = false, errorMessage = errorMsg) }
                return@launch
            }

            try {
                val semesterStartDate = state.semesterStartDate
                val fromWeek = ChronoUnit.WEEKS.between(semesterStartDate, state.fromDate).toInt() + 1
                val fromDay = state.fromDate.dayOfWeek.value
                val toWeek = ChronoUnit.WEEKS.between(semesterStartDate, state.toDate).toInt() + 1
                val toDay = state.toDate.dayOfWeek.value

                courseTableRepository.tweakCoursesOnDate(
                    mode = state.tweakMode, // 传入当前选中的模式
                    courseTableId = state.selectedCourseTable.id,
                    fromWeek = fromWeek,
                    fromDay = fromDay,
                    toWeek = toWeek,
                    toDay = toDay
                )

                // 操作成功后刷新预览
                refreshUiState()

                val successMsg = resources.getString(R.string.toast_tweak_success)
                _uiState.update { it.copy(isLoading = false, successMessage = successMsg) }

            } catch (e: Exception) {
                val errorMsgFormat = resources.getString(R.string.error_tweak_failed)
                val errorMsg = String.format(errorMsgFormat, e.message)
                _uiState.update { it.copy(isLoading = false, errorMessage = errorMsg) }
            }
        }
    }

    fun resetMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}