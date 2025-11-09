// TweakScheduleScreen.kt
package com.xingheyuzhuan.shiguangschedule.ui.settings.tweaks

import android.content.res.Configuration
import android.text.format.DateFormat
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
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.ui.components.CourseTablePickerDialog
import com.xingheyuzhuan.shiguangschedule.ui.components.DatePickerModal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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

    val titleTweakSchedule = stringResource(R.string.title_tweak_schedule)
    val a11yBack = stringResource(R.string.a11y_back)
    val a11ySaveTweak = stringResource(R.string.a11y_save_tweak)
    val labelSelectTweakTable = stringResource(R.string.label_select_tweak_table)
    val actionSelectTable = stringResource(R.string.action_select_table)
    val labelTweakFromDate = stringResource(R.string.label_tweak_from_date)
    val labelTweakToDate = stringResource(R.string.label_tweak_to_date)
    val textTweakHint = stringResource(R.string.text_tweak_hint)
    val titleTweakFromCourse = stringResource(R.string.title_tweak_from_course)
    val titleTweakToCourse = stringResource(R.string.title_tweak_to_course)
    val dialogTitleSelectExportTable = stringResource(R.string.dialog_title_select_export_table)
    val a11yArrow = stringResource(R.string.a11y_arrow)

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
                title = { Text(titleTweakSchedule) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = a11yBack
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.moveCourses() }) {
                        Icon(imageVector = Icons.Default.Done, contentDescription = a11ySaveTweak)
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
                        text = labelSelectTweakTable,
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(
                        onClick = { showCourseTablePicker = true }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = uiState.selectedCourseTable?.name ?: actionSelectTable)
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = actionSelectTable,
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
                        label = labelTweakFromDate,
                        date = uiState.fromDate,
                        onClick = { showFromDatePicker = true }
                    )
                    DateButton(
                        label = labelTweakToDate,
                        date = uiState.toDate,
                        onClick = { showToDatePicker = true }
                    )
                }
            }

            item {
                Text(
                    text = textTweakHint,
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
                            title = titleTweakFromCourse,
                            courses = uiState.fromCourses
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = a11yArrow,
                            modifier = Modifier.size(24.dp)
                        )
                        CourseDisplayCard(
                            modifier = Modifier.weight(1f),
                            title = titleTweakToCourse,
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
                            title = titleTweakFromCourse,
                            courses = uiState.fromCourses
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = a11yArrow,
                            modifier = Modifier.size(24.dp)
                        )
                        CourseDisplayCard(
                            title = titleTweakToCourse,
                            courses = uiState.toCourses
                        )
                    }
                }
            }
        }
    }

    if (showCourseTablePicker) {
        CourseTablePickerDialog(
            title = dialogTitleSelectExportTable,
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
 */
@Composable
fun CourseDisplayCard(title: String, courses: List<CourseWithWeeks>, modifier: Modifier = Modifier) {
    val textNoCourse = stringResource(R.string.text_no_course)
    val courseTimeDaySectionDetailsTweak = stringResource(R.string.course_time_day_section_details_tweak)
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
                            text = textNoCourse,
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
                                text = stringResource(
                                    R.string.course_time_day_section_details_tweak,
                                    getLocalizedDayString(course.day),
                                    course.startSection,
                                    course.endSection
                                ),
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
    val configuration = LocalConfiguration.current
    val currentLocale: Locale = configuration.locales.get(0)
    val dateDisplay: String = remember(date, currentLocale) {
        val bestPattern: String = DateFormat.getBestDateTimePattern(currentLocale, "Md")
        val finalFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(bestPattern, currentLocale)
        date.format(finalFormatter)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
        TextButton(onClick = onClick) {
            Text(
                text = dateDisplay,
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
 * 辅助 Composable 函数：将数字星期转换为本地化的字符串资源。
 * 复用 strings.xml 中的 `week_days_full_names` 数组。
 */
@Composable
private fun getLocalizedDayString(day: Int): String {
    // 星期从 1 (周一) 到 7 (周日)。数组索引从 0 开始。
    val weekDays = stringArrayResource(R.array.week_days_full_names)

    // 校验 day 的有效范围 (1-7)
    return if (day in 1..7) {
        weekDays[day - 1] // 1 -> index 0, 7 -> index 6
    } else {
        stringResource(R.string.text_error)
    }
}