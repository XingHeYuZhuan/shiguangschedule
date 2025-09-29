package com.xingheyuzhuan.shiguangschedule.ui.settings.tweaks

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.ui.components.CourseTablePickerDialog
import com.xingheyuzhuan.shiguangschedule.ui.components.DatePickerModal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 调课页面主界面。
 *
 * @param viewModel 通过工厂方法注入的 TweakScheduleViewModel。
 * @param navController 用于处理导航的 NavController 实例。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TweakScheduleScreen(
    viewModel: TweakScheduleViewModel = viewModel(factory = TweakScheduleViewModelFactory),
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showCourseTablePicker by remember { mutableStateOf(false) }
    var showFromDatePicker by remember { mutableStateOf(false) }
    var showToDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.resetMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.resetMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("调课") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.moveCourses() }) {
                        Icon(imageVector = Icons.Default.Done, contentDescription = "保存")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "选择要调整的课表",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(
                        onClick = { showCourseTablePicker = true }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = uiState.selectedCourseTable?.name ?: "选择课表")
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "选择课表",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    DateButton(
                        label = "被调整日期",
                        date = uiState.fromDate,
                        onClick = { showFromDatePicker = true }
                    )
                    DateButton(
                        label = "调整到日期",
                        date = uiState.toDate,
                        onClick = { showToDatePicker = true }
                    )
                }
            }

            item {
                Text(
                    text = "请确认以下两个日期的课程信息，点击右上角保存即可完成调课。",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
                if (isLandscape) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CourseDisplayCard(
                            modifier = Modifier.weight(1f),
                            title = "被调整的课程",
                            courses = uiState.fromCourses
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "箭头",
                            modifier = Modifier.size(24.dp)
                        )
                        CourseDisplayCard(
                            modifier = Modifier.weight(1f),
                            title = "调整到日期的课程",
                            courses = uiState.toCourses
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CourseDisplayCard(
                            title = "被调整的课程",
                            courses = uiState.fromCourses
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "箭头",
                            modifier = Modifier.size(24.dp)
                        )
                        CourseDisplayCard(
                            title = "调整到日期的课程",
                            courses = uiState.toCourses
                        )
                    }
                }
            }
        }
    }

    if (showCourseTablePicker) {
        CourseTablePickerDialog(
            title = "选择课表",
            onDismissRequest = { showCourseTablePicker = false },
            onTableSelected = {
                viewModel.onCourseTableSelected(it)
                showCourseTablePicker = false
            }
        )
    }

    if (showFromDatePicker) {
        DatePickerModal(
            onDateSelected = {
                it?.let {
                    viewModel.onFromDateSelected(it.toLocalDate())
                }
            },
            onDismiss = { showFromDatePicker = false }
        )
    }

    if (showToDatePicker) {
        DatePickerModal(
            onDateSelected = {
                it?.let {
                    viewModel.onToDateSelected(it.toLocalDate())
                }
            },
            onDismiss = { showToDatePicker = false }
        )
    }
}

/**
 * 用于显示单个课程列表的卡片。
 *
 * **修改点：为内部的 LazyColumn 添加了 heightIn(max = 250.dp) 以避免无限高度约束导致的崩溃。**
 */
@Composable
fun CourseDisplayCard(title: String, courses: List<CourseWithWeeks>, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 250.dp)
            ) {
                if (courses.isEmpty()) {
                    item {
                        Text(
                            text = "无课程",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                } else {
                    items(courses) { courseWithWeeks ->
                        val course = courseWithWeeks.course
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text(text = course.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "星期 ${course.day.toChineseDay()} | 节次 ${course.startSection}-${course.endSection}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 日期选择按钮。
 */
@Composable
private fun DateButton(label: String, date: LocalDate, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
        TextButton(onClick = onClick) {
            Text(text = date.format(DateTimeFormatter.ofPattern("M月d日")),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

/**
 * 辅助函数：将 Long 毫秒转换为 LocalDate
 */
private fun Long.toLocalDate(): LocalDate =
    java.time.Instant.ofEpochMilli(this).atZone(java.time.ZoneId.systemDefault()).toLocalDate()

/**
 * 辅助函数：将数字星期转换为中文字符
 */
private fun Int.toChineseDay(): String {
    return when (this) {
        1 -> "一"
        2 -> "二"
        3 -> "三"
        4 -> "四"
        5 -> "五"
        6 -> "六"
        7 -> "日"
        else -> ""
    }
}