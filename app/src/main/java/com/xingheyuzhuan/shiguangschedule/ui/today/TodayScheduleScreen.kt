package com.xingheyuzhuan.shiguangschedule.ui.today

import android.app.Application
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.ui.res.stringResource
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.model.ScheduleGridStyle
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScheduleScreen(
    navController: NavHostController,
    viewModel: TodayScheduleViewModel = viewModel(
        factory = TodayScheduleViewModel.TodayScheduleViewModelFactory(
            application = LocalContext.current.applicationContext as Application
        )
    )
) {
    val semesterStatus by viewModel.semesterStatus.collectAsState()
    val todayCourses by viewModel.todayCourses.collectAsState()
    // 1. 获取全局样式配置
    val gridStyle by viewModel.gridStyle.collectAsState()
    // 2. 监测系统深色模式状态
    val isDark = isSystemInDarkTheme()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.title_today_schedule)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            val today = LocalDate.now()
            val todayDateString = remember(today) {
                today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
            }

            val todayDayOfWeekString = remember(today) {
                today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            }

            Text(
                text = "$todayDateString $todayDayOfWeekString",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = semesterStatus,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (todayCourses.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.text_no_courses_today),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val currentTime = LocalTime.now()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    todayCourses.forEach { course ->
                        val isCourseFinished = remember(currentTime, course) {
                            try {
                                val courseEndTime = LocalTime.parse(course.endTime)
                                currentTime.isAfter(courseEndTime)
                            } catch (e: Exception) {
                                false
                            }
                        }

                        // 3. 根据课程的 colorInt 获取对应的配色对
                        val colorPair = gridStyle.courseColorMaps.getOrElse(course.colorInt) {
                            ScheduleGridStyle.DEFAULT_COLOR_MAPS[0]
                        }

                        // 4. 根据当前深色/浅色模式选择颜色
                        val baseColor = if (isDark) colorPair.dark else colorPair.light

                        // 5. 计算卡片容器颜色（如果已结束，降低透明度）
                        val cardColor = if (isCourseFinished) {
                            baseColor.copy(alpha = 0.4f)
                        } else {
                            baseColor
                        }

                        // 计算文字颜色（深色模式下用白色，浅色通常用黑色或深色，取决于背景）
                        val contentColor = if (isDark) Color.White else Color.Black

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = cardColor,
                                contentColor = contentColor // 自动应用到卡片内的文字
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (isCourseFinished) 0.dp else 2.dp
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = course.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        textDecoration = if (isCourseFinished) TextDecoration.LineThrough else TextDecoration.None
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    color = contentColor
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Text(
                                        text = "${course.startTime} - ${course.endTime}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = contentColor
                                    )

                                    course.position.takeIf { it.isNotBlank() }?.let { position ->
                                        Text(" | ", style = MaterialTheme.typography.bodySmall, color = contentColor)
                                        Text(
                                            text = position,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = contentColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    course.teacher.takeIf { it.isNotBlank() }?.let { teacher ->
                                        Text(" | ", style = MaterialTheme.typography.bodySmall, color = contentColor)
                                        Text(
                                            text = teacher,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = contentColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}