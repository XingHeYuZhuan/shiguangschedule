package com.xingheyuzhuan.shiguangschedule.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.model.ScheduleGridStyle
import com.xingheyuzhuan.shiguangschedule.ui.components.BottomNavigationBar
import com.xingheyuzhuan.shiguangschedule.ui.theme.LocalIsDarkTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScheduleScreen(
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit,
    viewModel: TodayScheduleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val gridStyle by viewModel.gridStyle.collectAsState()
    val isDark = LocalIsDarkTheme.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_today_schedule),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        },
        bottomBar = {
            BottomNavigationBar(
                currentDestination = Destination.TodaySchedule,
                onTabSelected = { dest -> onNavigate(dest) }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val state = uiState) {
                is TodayUiState.Loading -> { /* 可放置圆圈加载 */ }
                is TodayUiState.Success -> {
                    TodayContent(state, gridStyle, isDark)
                }
            }
        }
    }
}

@Composable
fun TodayContent(
    state: TodayUiState.Success,
    gridStyle: ScheduleGridStyle,
    isDark: Boolean
) {
    val currentTime = LocalTime.now()

    val targetScrollIndex = remember(state.courses, currentTime) {
        val firstActiveIndex = state.courses.indexOfFirst { model ->
            try {
                !LocalTime.parse(model.endTime ?: "00:00").isBefore(currentTime)
            } catch (e: Exception) {
                true
            }
        }

        if (firstActiveIndex == -1) {
            (state.courses.size - 1).coerceAtLeast(0)
        } else {
            firstActiveIndex
        }
    }

    val scrollState = rememberLazyListState(
        initialFirstVisibleItemIndex = targetScrollIndex
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        val dateStr = remember(state.today) {
            val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
                .withLocale(Locale.getDefault())
            val weekFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())
            "${state.today.format(formatter)} ${state.today.format(weekFormatter)}"
        }

        val subTitle = when (state.status) {
            TodayStatus.NoSemesterConfig -> stringResource(R.string.title_semester_not_set)

            TodayStatus.Vacation -> {
                val days = if (state.startDate != null) {
                    ChronoUnit.DAYS.between(state.today, state.startDate).toString()
                } else "0"
                stringResource(R.string.title_vacation_until_start, days)
            }

            TodayStatus.SemesterEnded -> {
                val totalDays = if (state.startDate != null) {
                    ChronoUnit.DAYS.between(state.startDate, state.today).toInt()
                } else 0
                val overdueWeeks = (totalDays / 7) - state.weekIndex + 1
                stringResource(R.string.status_semester_ended, overdueWeeks.coerceAtLeast(1).toString())
            }

            TodayStatus.Normal -> stringResource(R.string.title_current_week, state.weekIndex.toString())
        }

        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(text = dateStr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = subTitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (state.courses.isEmpty()) {
            EmptyStateView()
        } else {
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                itemsIndexed(state.courses) { _, model ->
                    CourseTimelineItem(model, gridStyle, isDark)
                }
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun CourseTimelineItem(
    model: CourseDisplayModel,
    gridStyle: ScheduleGridStyle,
    isDark: Boolean
) {
    val currentTime = LocalTime.now()
    val isFinished = remember(model.endTime) {
        try {
            LocalTime.parse(model.endTime ?: "00:00").isBefore(currentTime)
        } catch (e: Exception) { false }
    }

    val colorPair = gridStyle.courseColorMaps.getOrElse(model.course.colorInt) {
        ScheduleGridStyle.DEFAULT_COLOR_MAPS[0]
    }
    val themeColor = if (isDark) colorPair.dark else colorPair.light

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = if (isFinished) 0.5f else 1f)
    ) {
        Column(
            modifier = Modifier.width(65.dp).padding(top = 4.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = model.startTime ?: "--:--",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    textDecoration = if (isFinished) TextDecoration.LineThrough else null
                ),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = model.endTime ?: "--:--",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = themeColor
                ),
                shape = MaterialTheme.shapes.medium,
                elevation = if (isFinished) CardDefaults.cardElevation(defaultElevation = 0.dp)
                else CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = model.course.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            textDecoration = if (isFinished) TextDecoration.LineThrough else null
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (model.course.position.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.course_position_prefix, model.course.position),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    if (model.course.teacher.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.course_teacher_prefix, model.course.teacher),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            model.course.remark?.takeIf { it.isNotBlank() }?.let { remark ->
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp, start = 4.dp)
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(8.dp)
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.label_remark),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = remark,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.text_no_courses_today),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline
        )
    }
}