package com.xingheyuzhuan.shiguangschedule.ui.settings.coursemanagement

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.model.DualColor
import com.xingheyuzhuan.shiguangschedule.navigation.AddEditCourseChannel
import com.xingheyuzhuan.shiguangschedule.navigation.PresetCourseData
import com.xingheyuzhuan.shiguangschedule.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel


/**
 * 二级页面：展示特定课程名称下的所有实例，使用两列网格 (Detail View)。
 * @param courseName 从导航参数中接收，用于 ViewModel 过滤。
 * @param onNavigateBack 导航回上一级
 * @param onNavigate 用于在 Composable 内部处理 FAB 和卡片点击时的导航。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CourseInstanceListScreen(
    courseName: String,
    onNavigateBack: () -> Unit,
    onNavigate: (Destination) -> Unit,
    viewModel: CourseInstanceListViewModel = koinViewModel()
) {
    LaunchedEffect(courseName) {
        viewModel.initCourseName(courseName)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val courseInstances by viewModel.courseInstances.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedCourseIds by viewModel.selectedCourseIds.collectAsState()
    val scope = rememberCoroutineScope()

    val onNavigateToAddNewCourse: () -> Unit = {
        scope.launch {
            val presetData = PresetCourseData(
                name = courseName,
                startSection = 1,
                endSection = 2
            )
            AddEditCourseChannel.sendEvent(presetData)
            onNavigate(Destination.AddEditCourse(courseId = null))
        }
    }

    // 编辑课程逻辑
    val onNavigateToEditCourse: (courseId: String) -> Unit = { courseId ->
        onNavigate(Destination.AddEditCourse(courseId = courseId))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isSelectionMode) {
                            stringResource(R.string.title_selected_items_count, selectedCourseIds.size)
                        } else {
                            courseName
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = if (isSelectionMode) viewModel::toggleSelectionMode else onNavigateBack) {
                        Icon(
                            if (isSelectionMode) Icons.Filled.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        val totalCount = courseInstances.size
                        val selectedCount = selectedCourseIds.size
                        val isAllSelected = totalCount > 0 && selectedCount == totalCount

                        IconButton(onClick = viewModel::toggleSelectAll, enabled = totalCount > 0) {
                            val selectAllStringRes = if (isAllSelected) R.string.action_deselect_all else R.string.action_select_all
                            Icon(Icons.Filled.Check, contentDescription = stringResource(selectAllStringRes))
                        }

                        IconButton(onClick = {
                            scope.launch { viewModel.deleteSelectedCourses() }
                        }, enabled = selectedCount > 0) {
                            Icon(Icons.Filled.Delete, contentDescription = null)
                        }
                    }

                    IconButton(
                        onClick = viewModel::toggleSelectionMode,
                        enabled = courseInstances.isNotEmpty() || isSelectionMode
                    ) {
                        val descriptionRes = if (isSelectionMode) R.string.a11y_exit_selection_mode else R.string.a11y_enter_selection_mode
                        Icon(Icons.AutoMirrored.Filled.MenuOpen, contentDescription = stringResource(descriptionRes))
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(onClick = onNavigateToAddNewCourse) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_add))
                }
            }
        }
    ) { paddingValues ->
        // 列表部分保持原样，仅将 onNavigateToEditCourse 回调传入卡片
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(courseInstances, key = { it.course.id }) { courseWithWeeks ->
                CourseInstanceCard(
                    courseWithWeeks = courseWithWeeks,
                    isSelectionMode = isSelectionMode,
                    isSelected = selectedCourseIds.contains(courseWithWeeks.course.id),
                    colorMaps = uiState.courseColorMaps,
                    onCourseClick = { courseId ->
                        if (isSelectionMode) {
                            viewModel.toggleCourseSelection(courseId)
                        } else {
                            onNavigateToEditCourse(courseId)
                        }
                    },
                    onCourseLongClick = { courseId ->
                        if (!isSelectionMode) viewModel.toggleSelectionMode()
                        viewModel.toggleCourseSelection(courseId)
                    }
                )
            }
        }
    }
}

/**
 * 课程实例卡片 Composable (两列网格中的单个卡片)。
 */
@Composable
fun CourseInstanceCard(
    courseWithWeeks: CourseWithWeeks,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    colorMaps: List<DualColor>,
    onCourseClick: (courseId: String) -> Unit,
    onCourseLongClick: (courseId: String) -> Unit
) {
    val course = courseWithWeeks.course
    val courseId = course.id

    val isDarkTheme = LocalIsDarkTheme.current

    // 如果索引不存在，则取列表第一项；如果列表为空，则使用 MaterialTheme 的 SurfaceVariant 颜色兜底
    val fallbackColor = DualColor(
        light = MaterialTheme.colorScheme.surfaceVariant,
        dark = MaterialTheme.colorScheme.surfaceVariant
    )
    val courseColorDual = colorMaps.getOrNull(course.colorInt) ?: colorMaps.firstOrNull() ?: fallbackColor

    // 根据主题获取课程背景色
    val courseBackgroundColor = if (isDarkTheme) courseColorDual.dark else courseColorDual.light

    val weekDays = stringArrayResource(R.array.week_days_full_names)
    val dayName = weekDays.getOrElse(course.day - 1) { "?" }

    // 卡片颜色：始终使用课程颜色作为背景
    val cardColors = CardDefaults.cardColors(
        containerColor = courseBackgroundColor,
        contentColor = MaterialTheme.colorScheme.onSurface
    )

    Card(
        modifier = Modifier
            .height(IntrinsicSize.Max)
            .combinedClickable(
                onClick = { onCourseClick(courseId) },
                onLongClick = { onCourseLongClick(courseId) }
            )
            .then(
                // 选中状态
                if (isSelected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
                } else Modifier
            ),
        colors = cardColors
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // 教师信息
            Text(
                text = course.teacher,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            // 地点信息
            Text(
                text = course.position,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 上课时间/节次
            val timeText = if (course.isCustomTime) {
                stringResource(
                    R.string.course_time_day_time_details_tweak,
                    dayName,
                    course.customStartTime ?: "?",
                    course.customEndTime ?: "?"
                )
            } else {
                stringResource(
                    R.string.course_time_day_section_details_tweak,
                    dayName,
                    course.startSection ?: "?",
                    course.endSection ?: "?"
                )
            }

            Text(
                text = timeText,
                style = MaterialTheme.typography.bodyMedium
            )

            // 周次信息
            Text(
                text = stringResource(
                    R.string.label_weeks_format,
                    courseWithWeeks.weeks.map { it.weekNumber }.joinToString(", ")
                ),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}