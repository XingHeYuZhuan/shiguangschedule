package com.xingheyuzhuan.shiguangschedule.ui.schedule

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTable
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleModeProto
import com.xingheyuzhuan.shiguangschedule.navigation.AddEditCourseChannel
import com.xingheyuzhuan.shiguangschedule.navigation.PresetCourseData
import com.xingheyuzhuan.shiguangschedule.ui.components.BottomNavigationBar
import com.xingheyuzhuan.shiguangschedule.ui.components.CourseTablePickerDialog
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.FloatingCourseBar
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.ScheduleGrid
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.ScheduleGridStyleComposed
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.WeekSelectorBottomSheet
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * 无限时间轴的中值锚点。
 */
private const val INFINITE_PAGER_CENTER = Int.MAX_VALUE / 2

/**
 * 周课表主屏幕组件 - 支持长按手势拉伸调整时间并创建新课程
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WeeklyScheduleScreen(
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit,
    viewModel: WeeklyScheduleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val snackbarMsg = stringResource(id = R.string.snackbar_add_course_within_semester)

    val pagerState = rememberPagerState(
        initialPage = INFINITE_PAGER_CENTER,
        pageCount = { Int.MAX_VALUE }
    )

    LaunchedEffect(pagerState.currentPage, uiState.firstDayOfWeek) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { pageIndex ->
                val offsetWeeks = (pageIndex - INFINITE_PAGER_CENTER).toLong()
                val firstDay = DayOfWeek.of(uiState.firstDayOfWeek)
                val thisMonday = today.with(TemporalAdjusters.previousOrSame(firstDay))
                val targetMonday = thisMonday.plusWeeks(offsetWeeks)
                viewModel.updatePagerDate(targetMonday)
            }
    }

    // UI 交互控制弹窗标志位
    var showWeekSelector by remember { mutableStateOf(false) }
    var showTableSwitcher by remember { mutableStateOf(false) }
    var isGridHolding by remember { mutableStateOf(false) }

    val composedStyle by remember(uiState.style) {
        derivedStateOf { with(ScheduleGridStyleComposed) { uiState.style.toComposedStyle() } }
    }

    val floatingCourse = uiState.floatingCourse

    val floatingDuration by remember(floatingCourse, composedStyle.scheduleMode) {
        derivedStateOf {
            if (floatingCourse != null) {
                val start = floatingCourse.course.startSection?.toFloat() ?: 1f
                val end = floatingCourse.course.endSection?.toFloat() ?: 1f

                if (composedStyle.scheduleMode == ScheduleModeProto.TIME_24H_MODE) {
                    (end - start).coerceAtLeast(1.0f)
                } else {
                    (end - start + 1f).coerceAtLeast(1.0f)
                }
            } else {
                1.0f
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val gridScrollState = rememberScrollState()


    val customTextColor = composedStyle.pageTextColor ?: MaterialTheme.colorScheme.onSurface
    val customSubTextColor = customTextColor.copy(alpha = 0.7f)

    val displayTitle = when {
        !uiState.isSemesterSet || uiState.semesterStartDate == null -> {
            stringResource(R.string.title_semester_not_set)
        }
        uiState.daysUntilStart > 0 -> {
            stringResource(R.string.title_vacation_until_start, uiState.daysUntilStart.toString())
        }
        uiState.weekIndexInPager != null && uiState.weekIndexInPager!! in 1..uiState.totalWeeks -> {
            stringResource(R.string.title_current_week, uiState.weekIndexInPager.toString())
        }
        else -> {
            stringResource(R.string.title_vacation)
        }
    }

    val collapseFraction = scrollBehavior.state.collapsedFraction

    Box(modifier = Modifier.fillMaxSize()) {
        if (composedStyle.backgroundImagePath.isNotEmpty()) {
            AsyncImage(
                model = composedStyle.backgroundImagePath,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    if (!uiState.isSemesterSet || uiState.semesterStartDate == null) {
                                        onNavigate(Destination.Settings)
                                    } else {
                                        showWeekSelector = true
                                    }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = displayTitle,
                                style = MaterialTheme.typography.titleLarge,
                                color = customTextColor
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(20.dp)
                                    .offset(y = (-4).dp),
                                tint = customSubTextColor
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showTableSwitcher = true }) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = stringResource(R.string.action_select_table),
                                tint = customTextColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    ),
                    scrollBehavior = scrollBehavior
                )
            },
            bottomBar = {
                if (floatingCourse == null) {
                    BottomNavigationBar(
                        currentDestination = Destination.CourseSchedule,
                        onTabSelected = { dest -> onNavigate(dest) },
                        isTransparent = composedStyle.backgroundImagePath.isNotEmpty(),
                        contentColor = customTextColor,
                        modifier = Modifier.graphicsLayer {
                            translationY = size.height * collapseFraction
                            alpha = 1f - collapseFraction
                        }
                    )
                }
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->

            val dynamicBottomPadding = remember(innerPadding, collapseFraction, floatingCourse) {
                if (floatingCourse != null) {
                    0.dp
                } else {
                    val systemWindowInsetBottom = innerPadding.calculateBottomPadding() - 80.dp
                    val safeSystemBottom = systemWindowInsetBottom.coerceAtLeast(0.dp)
                    val expandableHeight = innerPadding.calculateBottomPadding() - safeSystemBottom
                    safeSystemBottom + (expandableHeight * (1f - collapseFraction))
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .padding(
                        start = innerPadding.calculateStartPadding(LayoutDirection.Ltr),
                        top = innerPadding.calculateTopPadding(),
                        end = innerPadding.calculateEndPadding(LayoutDirection.Ltr),
                        bottom = dynamicBottomPadding
                    )
                    .fillMaxSize(),
                beyondViewportPageCount = 1,
                userScrollEnabled = !isGridHolding
            ) { pageIndex ->

                val pageMondayDate = remember(pageIndex, uiState.firstDayOfWeek) {
                    val offsetWeeks = (pageIndex - INFINITE_PAGER_CENTER).toLong()
                    val firstDay = DayOfWeek.of(uiState.firstDayOfWeek)
                    today.with(TemporalAdjusters.previousOrSame(firstDay)).plusWeeks(offsetWeeks)
                }

                val pageYearString = remember(pageMondayDate) {
                    pageMondayDate.year.toString()
                }

                val pageDateStrings = remember(pageMondayDate) {
                    val formatter = DateTimeFormatter.ofPattern("MM-dd")
                    (0..6).map { pageMondayDate.plusDays(it.toLong()).format(formatter) }
                }

                val pageTodayIndex = remember(pageMondayDate) {
                    val weekDates = (0..6).map { pageMondayDate.plusDays(it.toLong()) }
                    weekDates.indexOf(today)
                }

                val pageCourses = uiState.courseCache[pageMondayDate.toString()] ?: emptyList()

                ScheduleGrid(
                    gridScrollState = gridScrollState,
                    style = composedStyle,
                    dates = pageDateStrings,
                    currentYear = pageYearString,
                    timeSlots = uiState.timeSlots,
                    mergedCourses = pageCourses,
                    showWeekends = uiState.showWeekends,
                    todayIndex = pageTodayIndex,
                    firstDayOfWeek = uiState.firstDayOfWeek,
                    currentSectionIndex = if (pageTodayIndex >= 0) uiState.currentSectionIndex else -1,
                    onCourseBlockClicked = { mergedBlock ->
                        mergedBlock.courses.firstOrNull()?.course?.id?.let {
                            onNavigate(Destination.AddEditCourse(courseId = it))
                        }
                    },
                    onGridCellClicked = { day, sectionOrHour ->
                        if (floatingCourse != null) {
                            val targetWeek = uiState.weekIndexInPager ?: uiState.currentWeekNumber ?: return@ScheduleGrid
                            val startSec = sectionOrHour.toFloat()
                            val endSec = if (composedStyle.scheduleMode == ScheduleModeProto.TIME_24H_MODE) {
                                startSec + floatingDuration
                            } else {
                                startSec + floatingDuration - 1f
                            }

                            coroutineScope.launch {
                                viewModel.updateCourseTimeByFloatingGesture(
                                    targetWeek = targetWeek,
                                    targetDay = day,
                                    startSection = startSec,
                                    endSection = endSec
                                )
                            }
                        } else {
                            val currentWeek = uiState.weekIndexInPager ?: 0
                            val isCurrentPageValid = currentWeek in 1..uiState.totalWeeks

                            if (isCurrentPageValid) {
                                coroutineScope.launch {
                                    val currentWeekSet = setOf(currentWeek)

                                    val presetData = if (composedStyle.scheduleMode == ScheduleModeProto.TIME_24H_MODE) {
                                        val startHour = sectionOrHour.coerceIn(0, 23)
                                        val endHour = (startHour + 1) % 24

                                        val startTimeStr = String.format(Locale.US, "%02d:00", startHour)
                                        val endTimeStr = String.format(Locale.US, "%02d:00", endHour)

                                        PresetCourseData(
                                            day = day,
                                            isCustomTime = true,
                                            customStartTime = startTimeStr,
                                            customEndTime = endTimeStr,
                                            presetWeeks = currentWeekSet
                                        )
                                    } else {
                                        PresetCourseData(
                                            day = day,
                                            startSection = sectionOrHour,
                                            endSection = sectionOrHour,
                                            isCustomTime = false,
                                            presetWeeks = currentWeekSet
                                        )
                                    }

                                    AddEditCourseChannel.sendEvent(presetData)
                                    onNavigate(Destination.AddEditCourse())
                                }
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(snackbarMsg)
                                }
                            }
                        }
                    },
                    onTimeSlotClicked = {
                        onNavigate(Destination.TimeSlotSettings)
                    },
                    onHoldStateChanged = { isHolding ->
                        isGridHolding = isHolding
                    },
                    onCourseMovedWithinGrid = { clickedBlock, newDay, newStartSection, newEndSection ->
                        val currentWeek = uiState.weekIndexInPager ?: 0
                        val isCurrentPageValid = currentWeek in 1..uiState.totalWeeks

                        if (isCurrentPageValid) {
                            val courseId = clickedBlock.courses.firstOrNull()?.course?.id
                            if (courseId != null) {
                                coroutineScope.launch {
                                    viewModel.updateCourseTimeByGesture(
                                        courseId = courseId,
                                        targetDay = newDay,
                                        startSection = newStartSection,
                                        endSection = newEndSection
                                    )
                                }
                            }
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(snackbarMsg)
                            }
                        }
                    },
                    onCourseTimeAdjusted = { clickedBlock, newStart, newEnd ->
                        val currentWeek = uiState.weekIndexInPager ?: 0
                        val isCurrentPageValid = currentWeek in 1..uiState.totalWeeks

                        if (isCurrentPageValid) {
                            val courseId = clickedBlock.courses.firstOrNull()?.course?.id
                            if (courseId != null) {
                                coroutineScope.launch {
                                    viewModel.updateCourseTimeByGesture(
                                        courseId = courseId,
                                        targetDay = clickedBlock.day,
                                        startSection = newStart,
                                        endSection = newEnd
                                    )
                                }
                            }
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(snackbarMsg)
                            }
                        }
                    },
                    onInitiateFloatingMode = { block ->
                        val targetCourseWrapper = block.courses.firstOrNull()
                        val currentWeek = uiState.weekIndexInPager ?: uiState.currentWeekNumber
                        if (targetCourseWrapper != null && currentWeek != null) {
                            viewModel.enterFloatingMode(
                                course = targetCourseWrapper,
                                sourceWeek = currentWeek
                            )
                        }
                    }
                )
            }
        }
        FloatingCourseBar(
            floatingCourse = floatingCourse,
            onCancelClick = { viewModel.exitFloatingMode() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }

    // 周次选择弹窗
    if (showWeekSelector) {
        WeekSelectorBottomSheet(
            totalWeeks = uiState.totalWeeks,
            currentWeek = uiState.currentWeekNumber ?: 1,
            selectedWeek = uiState.weekIndexInPager ?: (uiState.currentWeekNumber ?: 1),
            onWeekSelected = { week ->
                val currentWeekAtPage = uiState.weekIndexInPager ?: 1
                val offset = week - currentWeekAtPage
                coroutineScope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage + offset)
                }
                showWeekSelector = false
            },
            onDismissRequest = { showWeekSelector = false }
        )
    }

    // 课表切换弹窗
    if (showTableSwitcher) {
        CourseTablePickerDialog(
            title = stringResource(R.string.action_select_table),
            onDismissRequest = { showTableSwitcher = false },
            onTableSelected = { table: CourseTable ->
                viewModel.switchCourseTable(table.id)
                showTableSwitcher = false
            }
        )
    }
}