package com.xingheyuzhuan.shiguangschedule.ui.today

import android.app.Application
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.xingheyuzhuan.shiguangschedule.ui.components.BottomNavigationBar
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

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
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val semesterStatus by viewModel.semesterStatus.collectAsState()
    val todayCourses by viewModel.todayCourses.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "今日课表") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController, currentRoute = currentRoute)
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
                today.format(DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.getDefault()))
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
                        text = "今天没有课程",
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

                        val cardColor = if (isCourseFinished) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.tertiaryContainer
                        }

                        val contentColor = if (isCourseFinished) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = cardColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isCourseFinished) 1.dp else 2.dp)
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