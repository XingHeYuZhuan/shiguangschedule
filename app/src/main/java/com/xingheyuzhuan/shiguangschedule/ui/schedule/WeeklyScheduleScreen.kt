package com.xingheyuzhuan.shiguangschedule.ui.schedule

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTable
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.navigation.AddEditCourseChannel
import com.xingheyuzhuan.shiguangschedule.navigation.PresetCourseData
import com.xingheyuzhuan.shiguangschedule.ui.components.BottomNavigationBar
import com.xingheyuzhuan.shiguangschedule.ui.components.CourseTablePickerDialog
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.OverlapCourseBottomSheet
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.ScheduleGrid
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.ScheduleGridStyleComposed
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.WeekSelectorBottomSheet
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/**
 * 无限时间轴的中值锚点。
 */
private const val INFINITE_PAGER_CENTER = Int.MAX_VALUE / 2

/**
 * 周课表主屏幕组件。
 * 支持三周滑动窗口预加载，消除滑动残留与加载闪烁。
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

    val snackbarMsg = stringResource(id = R.string.snackbar_add_course_after_start)

    val pagerState = rememberPagerState(
        initialPage = INFINITE_PAGER_CENTER,
        pageCount = { Int.MAX_VALUE }
    )

    // 同步 Pager 状态到 ViewModel
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

    // UI 交互控制
    var showWeekSelector by remember { mutableStateOf(false) }
    var showOverlapBottomSheet by remember { mutableStateOf(false) }
    var showTableSwitcher by remember { mutableStateOf(false) }
    var overlapCoursesToShow by remember { mutableStateOf(emptyList<CourseWithWeeks>()) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val composedStyle by remember(uiState.style) {
        derivedStateOf { with(ScheduleGridStyleComposed) { uiState.style.toComposedStyle() } }
    }
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
            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
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
                BottomNavigationBar(
                    currentDestination = Destination.CourseSchedule,
                    onTabSelected = { dest -> onNavigate(dest) },
                    isTransparent = composedStyle.backgroundImagePath.isNotEmpty(),
                    contentColor = customTextColor
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                // [预加载] 强制渲染相邻页面，确保滑动时目标页已就绪
                beyondViewportPageCount = 1
            ) { pageIndex ->

                // 去中心化：每一页根据索引独立计算自己的周一日期
                val pageMondayDate = remember(pageIndex, uiState.firstDayOfWeek) {
                    val offsetWeeks = (pageIndex - INFINITE_PAGER_CENTER).toLong()
                    val firstDay = DayOfWeek.of(uiState.firstDayOfWeek)
                    today.with(TemporalAdjusters.previousOrSame(firstDay)).plusWeeks(offsetWeeks)
                }

                val pageYearString = remember(pageMondayDate) {
                    pageMondayDate.year.toString()
                }

                // 独立日期列表：计算该页显示的日期文本
                val pageDateStrings = remember(pageMondayDate) {
                    val formatter = DateTimeFormatter.ofPattern("MM-dd")
                    (0..6).map { pageMondayDate.plusDays(it.toLong()).format(formatter) }
                }

                // 独立高亮：计算“今天”在该页的位置
                val pageTodayIndex = remember(pageMondayDate) {
                    val weekDates = (0..6).map { pageMondayDate.plusDays(it.toLong()) }
                    weekDates.indexOf(today)
                }

                // 从三周缓存 Map 中获取该页日期对应的数据
                val pageCourses = uiState.courseCache[pageMondayDate.toString()] ?: emptyList()

                ScheduleGrid(
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
                        // 如果有超过一门课（无论是否本周），都打开重叠弹窗
                        if (mergedBlock.courses.size > 1) {
                            overlapCoursesToShow = mergedBlock.courses
                            showOverlapBottomSheet = true
                        } else {
                            // 只有一门课时直接进入编辑
                            mergedBlock.courses.firstOrNull()?.course?.id?.let {
                                onNavigate(Destination.AddEditCourse(courseId = it))
                            }
                        }
                    },
                    onGridCellClicked = { day, section ->
                        if (uiState.semesterStartDate != null && !today.isBefore(uiState.semesterStartDate)) {
                            coroutineScope.launch {
                                AddEditCourseChannel.sendEvent(PresetCourseData(day, section, section))
                                onNavigate(Destination.AddEditCourse())
                            }
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(snackbarMsg)
                            }
                        }
                    },
                    onTimeSlotClicked = {
                        onNavigate(Destination.TimeSlotSettings)
                    }
                )
            }
        }
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

    // 重叠课程处理弹窗
    if (showOverlapBottomSheet) {
        OverlapCourseBottomSheet(
            style = composedStyle,
            courses = overlapCoursesToShow,
            timeSlots = uiState.timeSlots,
            currentWeek = uiState.weekIndexInPager,
            onCourseClicked = { course ->
                showOverlapBottomSheet = false
                onNavigate(Destination.AddEditCourse(courseId = course.course.id))
            },
            onDismissRequest = { showOverlapBottomSheet = false }
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