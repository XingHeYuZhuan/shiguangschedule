package com.xingheyuzhuan.shiguangschedule.ui.schedule

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.xingheyuzhuan.shiguangschedule.Screen
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.ui.components.BottomNavigationBar
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.ConflictCourseBottomSheet
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.ScheduleGrid
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.WeekSelectorBottomSheet
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

// Pager 的总页数和初始中心页码作为常量，便于管理
private const val TOTAL_PAGER_WEEKS = 1000
private const val INITIAL_PAGER_INDEX = TOTAL_PAGER_WEEKS / 2

/**
 * 周课表主页面，负责组合所有 UI 组件和处理状态。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WeeklyScheduleScreen(
    navController: NavHostController,
    viewModel: WeeklyScheduleViewModel = viewModel(factory = WeeklyScheduleViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showWeekSelector by remember { mutableStateOf(false) }
    var showConflictBottomSheet by remember { mutableStateOf(false) }
    var conflictCoursesToShow by remember { mutableStateOf(emptyList<CourseWithWeeks>()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var selectedWeek by remember { mutableStateOf<Int?>(null) }

    // 计算初始页码，始终基于当前日期
    val today = LocalDate.now()
    val semesterStartDate = uiState.semesterStartDate
    val initialPage = remember(semesterStartDate) {
        if (semesterStartDate != null) {
            val weekOffset = ChronoUnit.WEEKS.between(semesterStartDate, today).toInt()
            INITIAL_PAGER_INDEX + weekOffset
        } else {
            INITIAL_PAGER_INDEX
        }
    }

    // 初始化 pagerState，使用基于当前日期的初始页码
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { TOTAL_PAGER_WEEKS }
    )

    // 当 Pager 状态改变时，更新 selectedWeek
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { pageIndex ->
                // 将 Pager 页码映射到实际周数
                val newSelectedWeek = pageIndex - INITIAL_PAGER_INDEX
                selectedWeek = newSelectedWeek
            }
    }

    // 根据 UI 状态计算衍生数据
    val dates by remember(selectedWeek) {
        derivedStateOf {
            val formatter = DateTimeFormatter.ofPattern("MM-dd")
            selectedWeek?.let { weekOffset ->
                calculateDatesForPager(weekOffset).map { it.format(formatter) }
            } ?: emptyList()
        }
    }

    val todayIndex by remember(selectedWeek) {
        derivedStateOf {
            selectedWeek?.let { weekOffset ->
                calculateDatesForPager(weekOffset).indexOf(LocalDate.now())
            } ?: -1
        }
    }

    // 根据 ViewModel 数据和 selectedWeek 动态计算当前周的课程
    val currentCourses by remember(uiState.allCourses, uiState.timeSlots, selectedWeek) {
        derivedStateOf {
            val semesterStartDate = uiState.semesterStartDate
            if (semesterStartDate == null) {
                // 如果学期未设置，不显示任何课程
                emptyList()
            } else {
                selectedWeek?.let { weekOffset ->
                    val weekNumber = ChronoUnit.WEEKS.between(semesterStartDate, LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(weekOffset.toLong())).toInt() + 1
                    val coursesForWeek = uiState.allCourses.filter { courseWithWeeks ->
                        courseWithWeeks.weeks.any { it.weekNumber == weekNumber }
                    }
                    mergeCourses(coursesForWeek, uiState.timeSlots)
                } ?: emptyList()
            }
        }
    }

    val topBarTitle by remember(uiState, selectedWeek) {
        derivedStateOf {
            val today = LocalDate.now()
            val semesterStartDate = uiState.semesterStartDate

            when {
                // 学期未设置
                !uiState.isSemesterSet -> "请设置开学日期"

                // 学期已设置，但开学日期在未来
                semesterStartDate != null && today.isBefore(semesterStartDate) -> {
                    val daysUntilStart = ChronoUnit.DAYS.between(today, semesterStartDate)
                    "假期中（距离开学还有${daysUntilStart}天）"
                }

                // 学期已设置，并且在学期内
                uiState.isSemesterSet && selectedWeek != null -> {
                    val currentSemesterWeek = ChronoUnit.WEEKS.between(semesterStartDate, today).toInt() + 1
                    val targetWeekNumber = currentSemesterWeek + selectedWeek!!
                    if (targetWeekNumber in 1..uiState.totalWeeks) {
                        "第 $targetWeekNumber 周"
                    } else {
                        "假期中"
                    }
                }
                else -> "加载中..."
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = topBarTitle,
                        modifier = Modifier.clickable(
                            enabled = uiState.isSemesterSet && (uiState.semesterStartDate?.isBefore(LocalDate.now()) == true || uiState.semesterStartDate?.isEqual(LocalDate.now()) == true),
                            onClick = {
                                showWeekSelector = true
                            }
                        )
                    )
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController, currentRoute = currentRoute)
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                val semesterStartDate = uiState.semesterStartDate

                val onGridCellClicked: (Int, Int) -> Unit = { day, section ->
                    val isSemesterStarted = semesterStartDate != null && !today.isBefore(semesterStartDate)
                    if (isSemesterStarted) {
                        navController.navigate(Screen.AddEditCourse.createRouteForNewCourse(day, section))
                    } else {
                        viewModel.viewModelScope.launch {
                            snackbarHostState.showSnackbar("请在开学后添加课程")
                        }
                    }
                }

                // 直接调用 ScheduleGrid，让它处理内部的滚动
                ScheduleGrid(
                    dates = dates,
                    timeSlots = uiState.timeSlots,
                    mergedCourses = currentCourses,
                    showWeekends = uiState.showWeekends,
                    todayIndex = todayIndex,
                    onCourseBlockClicked = { mergedBlock ->
                        if (mergedBlock.isConflict) {
                            conflictCoursesToShow = mergedBlock.courses
                            showConflictBottomSheet = true
                        } else {
                            val courseId = mergedBlock.courses.firstOrNull()?.course?.id
                            if (courseId != null) {
                                navController.navigate(Screen.AddEditCourse.createRouteWithCourseId(courseId))
                            }
                        }
                    },
                    onGridCellClicked = onGridCellClicked,
                    onTimeSlotClicked = {
                        navController.navigate(Screen.TimeSlotSettings.route)
                    }
                )
            }
        }
    }

    // 周选择器 BottomSheet
    if (showWeekSelector && selectedWeek != null) {
        // 计算当前学期的周数
        val today = LocalDate.now()
        val semesterStartDate = uiState.semesterStartDate
        val currentSemesterWeek = if (semesterStartDate != null && today.isAfter(semesterStartDate)) {
            ChronoUnit.WEEKS.between(semesterStartDate, today).toInt() + 1
        } else {
            1
        }

        WeekSelectorBottomSheet(
            totalWeeks = uiState.totalWeeks,
            currentWeek = currentSemesterWeek,
            selectedWeek = currentSemesterWeek + selectedWeek!!,
            onWeekSelected = { week ->
                val newSelectedWeekOffset = week - currentSemesterWeek
                val targetPage = INITIAL_PAGER_INDEX + newSelectedWeekOffset
                viewModel.viewModelScope.launch {
                    pagerState.scrollToPage(targetPage)
                }
                showWeekSelector = false
            },
            onDismissRequest = { showWeekSelector = false }
        )
    }

    // 冲突课程列表 BottomSheet
    if (showConflictBottomSheet) {
        ConflictCourseBottomSheet(
            courses = conflictCoursesToShow,
            timeSlots = uiState.timeSlots,
            onCourseClicked = { course ->
                showConflictBottomSheet = false
                val courseId = course.course.id
                navController.navigate(Screen.AddEditCourse.createRouteWithCourseId(courseId))
            },
            onDismissRequest = { showConflictBottomSheet = false }
        )
    }
}

/**
 * 根据 Pager 的偏移量计算一周的所有日期。
 */
private fun calculateDatesForPager(weekOffset: Int): List<LocalDate> {
    val today = LocalDate.now()
    val mondayOfToday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val firstDayOfTargetWeek = mondayOfToday.plusWeeks(weekOffset.toLong())
    return (0..6).map { firstDayOfTargetWeek.plusDays(it.toLong()) }
}