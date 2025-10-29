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

    var selectedWeek by remember { mutableStateOf<Int?>(null) } // Pager 偏移量 (0 = 当前周)

    // 计算初始页码，基于 ViewModel 的当前周数 (currentWeekNumber)
    val today = LocalDate.now()
    val initialPage = remember(uiState.currentWeekNumber) {
        val currentWeek = uiState.currentWeekNumber ?: 1
        // 如果 ViewModel 计算出当前是第 X 周，则偏移量为 X-1，Pager 初始页为 INITIAL_PAGER_INDEX + (X-1)
        val initialOffset = currentWeek - 1
        INITIAL_PAGER_INDEX + initialOffset
    }

    // 初始化 pagerState，使用基于 ViewModel 数据的初始页码
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { TOTAL_PAGER_WEEKS }
    )

    // 当 Pager 状态改变时，更新 selectedWeek
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { pageIndex ->
                // 将 Pager 页码映射到实际周偏移量
                val newSelectedWeek = pageIndex - INITIAL_PAGER_INDEX
                selectedWeek = newSelectedWeek
            }
    }

    // 根据 UI 状态计算衍生数据
    val dates by remember(selectedWeek, uiState.firstDayOfWeek) {
        derivedStateOf {
            val formatter = DateTimeFormatter.ofPattern("MM-dd")
            selectedWeek?.let { weekOffset ->
                calculateDatesForPager(weekOffset, uiState.firstDayOfWeek).map { it.format(formatter) }
            } ?: emptyList()
        }
    }

    val todayIndex by remember(selectedWeek, uiState.firstDayOfWeek) {
        derivedStateOf {
            selectedWeek?.let { weekOffset ->
                calculateDatesForPager(weekOffset, uiState.firstDayOfWeek).indexOf(today)
            } ?: -1
        }
    }

    // 根据 ViewModel 数据和 selectedWeek 动态计算当前周的课程
    val currentCourses by remember(uiState.allCourses, uiState.timeSlots, selectedWeek, uiState.currentWeekNumber) { // 【修改 3.1：添加依赖 currentWeekNumber】
        derivedStateOf {
            val currentWeek = uiState.currentWeekNumber // 使用 ViewModel 计算的当前周数

            if (currentWeek == null || uiState.semesterStartDate == null) {
                // 如果当前学期周数未知，则显示空课表
                emptyList()
            } else {
                selectedWeek?.let { weekOffset ->
                    val targetWeekNumber = currentWeek + weekOffset

                    val coursesForWeek = uiState.allCourses.filter { courseWithWeeks ->
                        courseWithWeeks.weeks.any { it.weekNumber == targetWeekNumber }
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
            val currentWeek = uiState.currentWeekNumber // 使用 ViewModel 计算的当前学期周数

            when {
                // 学期未设置
                !uiState.isSemesterSet -> "请设置开学日期"

                // 学期已设置，但开学日期在未来
                semesterStartDate != null && today.isBefore(semesterStartDate) -> {
                    val daysUntilStart = ChronoUnit.DAYS.between(today, semesterStartDate)
                    "假期中（距离开学还有${daysUntilStart}天）"
                }

                // 学期已设置，并且在学期内
                uiState.isSemesterSet && selectedWeek != null && currentWeek != null -> {
                    val targetWeekNumber = currentWeek + selectedWeek!!
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
    val isSemesterStarted = uiState.semesterStartDate != null && !today.isBefore(uiState.semesterStartDate)

    val isTopBarClickable = !uiState.isSemesterSet || isSemesterStarted

    val topBarClickAction: () -> Unit = {
        if (!uiState.isSemesterSet) {
            navController.navigate(Screen.Settings.route) {
                popUpTo(navController.graph.id) {
                    inclusive = true
                }
            }
        } else if (isSemesterStarted) {
            showWeekSelector = true
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
                            enabled = isTopBarClickable,
                            onClick = topBarClickAction
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

                // 确保只有当前页面的 ScheduleGrid 接收到最新的数据
                if (pageIndex - INITIAL_PAGER_INDEX == selectedWeek) {
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
                        firstDayOfWeek = uiState.firstDayOfWeek,
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
                } else {
                    // 非当前页面的占位符
                    Column(modifier = Modifier.fillMaxSize()) { }
                }
            }
        }
    }

    // 周选择器 BottomSheet
    if (showWeekSelector && selectedWeek != null) {
        val currentWeek = uiState.currentWeekNumber ?: 1
        val selectedTargetWeek = currentWeek + selectedWeek!!

        WeekSelectorBottomSheet(
            totalWeeks = uiState.totalWeeks,
            currentWeek = currentWeek,
            selectedWeek = selectedTargetWeek,
            onWeekSelected = { week ->
                val newSelectedWeekOffset = week - currentWeek
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
 * 根据 Pager 的偏移量和设置的一周起始日计算一周的所有日期。
 */
private fun calculateDatesForPager(weekOffset: Int, firstDayOfWeek: Int): List<LocalDate> {
    val today = LocalDate.now()
    // 1. 将设置的 Int (1=周一, 7=周日) 转换为 java.time.DayOfWeek
    val dayOfWeekStart = DayOfWeek.of(firstDayOfWeek)

    // 2. 找到当前日期所在周的起始日
    // 使用用户设置的起始日替换硬编码的 DayOfWeek.MONDAY
    val startDayOfCurrentWeek = today.with(TemporalAdjusters.previousOrSame(dayOfWeekStart))

    // 3. 加上偏移量，得到目标周的起始日
    val firstDayOfTargetWeek = startDayOfCurrentWeek.plusWeeks(weekOffset.toLong())

    // 4. 生成目标周的 7 天日期
    return (0..6).map { firstDayOfTargetWeek.plusDays(it.toLong()) }
}