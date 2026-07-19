// TweakScheduleScreen.kt
package com.xingheyuzhuan.shiguangschedule.ui.settings.quickactions.tweaks

import android.content.res.Configuration
import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoubleArrow
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseTableRepository.TweakMode
import com.xingheyuzhuan.shiguangschedule.ui.components.CourseTablePickerDialog
import com.xingheyuzhuan.shiguangschedule.ui.components.DatePickerModal
import org.koin.compose.viewmodel.koinViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TweakScheduleScreen(
    onBack: () -> Unit,
    viewModel: TweakScheduleViewModel = koinViewModel()
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
                    IconButton(onClick = onBack) {
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
                    Text(text = labelSelectTweakTable, style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { showCourseTablePicker = true }) {
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
                    DateButton(label = labelTweakFromDate, date = uiState.fromDate, onClick = { showFromDatePicker = true })
                    DateButton(label = labelTweakToDate, date = uiState.toDate, onClick = { showToDatePicker = true })
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
                val (modeIcon, _) = getTweakModeDisplayInfo(uiState.tweakMode)

                if (isLandscape) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CourseDisplayCard(modifier = Modifier.weight(1f), title = titleTweakFromCourse, courses = uiState.fromCourses)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            TweakModeSelector(currentMode = uiState.tweakMode, onModeSelected = { viewModel.onTweakModeChanged(it) })
                            Icon(
                                imageVector = modeIcon,
                                contentDescription = a11yArrow,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        CourseDisplayCard(modifier = Modifier.weight(1f), title = titleTweakToCourse, courses = uiState.toCourses)
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CourseDisplayCard(title = titleTweakFromCourse, courses = uiState.fromCourses)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            val verticalIcon = when (uiState.tweakMode) {
                                TweakMode.EXCHANGE -> Icons.Default.SyncAlt
                                TweakMode.OVERWRITE -> Icons.Default.DoubleArrow
                                TweakMode.MERGE -> Icons.Default.ArrowDownward
                            }

                            val rotationAngle = if (uiState.tweakMode != TweakMode.MERGE) 90f else 0f

                            Icon(
                                imageVector = verticalIcon,
                                contentDescription = a11yArrow,
                                modifier = Modifier
                                    .size(32.dp)
                                    .rotate(rotationAngle),
                                tint = MaterialTheme.colorScheme.primary
                            )

                            TweakModeSelector(
                                currentMode = uiState.tweakMode,
                                onModeSelected = { viewModel.onTweakModeChanged(it) }
                            )
                        }

                        CourseDisplayCard(title = titleTweakToCourse, courses = uiState.toCourses)
                    }
                }
            }
        }
    }

    if (showCourseTablePicker) {
        CourseTablePickerDialog(
            title = dialogTitleSelectExportTable,
            onDismissRequest = { showCourseTablePicker = false },
            onTableSelected = { viewModel.onCourseTableSelected(it); showCourseTablePicker = false }
        )
    }

    if (showFromDatePicker) {
        DatePickerModal(onDateSelected = { it?.let { viewModel.onFromDateSelected(it.toLocalDate()) } }, onDismiss = { showFromDatePicker = false })
    }

    if (showToDatePicker) {
        DatePickerModal(onDateSelected = { it?.let { viewModel.onToDateSelected(it.toLocalDate()) } }, onDismiss = { showToDatePicker = false })
    }
}

@Composable
private fun TweakModeSelector(currentMode: TweakMode, onModeSelected: (TweakMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val (_, label) = getTweakModeDisplayInfo(currentMode)

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TweakMode.entries.forEach { mode ->
                val (mIcon, mLabel) = getTweakModeDisplayInfo(mode)
                DropdownMenuItem(
                    leadingIcon = { Icon(mIcon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    text = { Text(mLabel) },
                    onClick = { onModeSelected(mode); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun getTweakModeDisplayInfo(mode: TweakMode): Pair<ImageVector, String> {
    return when (mode) {
        TweakMode.MERGE -> Icons.AutoMirrored.Filled.ArrowForward to stringResource(R.string.tweak_mode_merge)
        TweakMode.OVERWRITE -> Icons.Default.DoubleArrow to stringResource(R.string.tweak_mode_overwrite)
        TweakMode.EXCHANGE -> Icons.Default.SyncAlt to stringResource(R.string.tweak_mode_exchange)
    }
}

@Composable
fun CourseDisplayCard(title: String, courses: List<CourseWithWeeks>, modifier: Modifier = Modifier) {
    val textNoCourse = stringResource(R.string.text_no_course)
    val sectionFormatRes = R.string.course_time_day_section_details_tweak
    val customTimeFormatRes = R.string.course_time_day_time_details_tweak

    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp)) {
                if (courses.isEmpty()) {
                    item { Text(text = textNoCourse, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(8.dp)) }
                } else {
                    items(courses) { courseWithWeeks ->
                        val course = courseWithWeeks.course
                        val dayString = getLocalizedDayString(course.day)
                        val detailsText = if (course.isCustomTime) {
                            stringResource(id = customTimeFormatRes, dayString, course.customStartTime ?: "??:??", course.customEndTime ?: "??:??")
                        } else {
                            stringResource(id = sectionFormatRes, dayString, course.startSection.toString(), course.endSection.toString())
                        }
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Text(text = course.name, style = MaterialTheme.typography.bodyLarge)
                            Text(text = detailsText, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

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
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = onClick) { Text(text = dateDisplay, style = MaterialTheme.typography.titleMedium) }
    }
}

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

@Composable
private fun getLocalizedDayString(day: Int): String {
    val weekDays = stringArrayResource(R.array.week_days_full_names)
    return if (day in 1..7) weekDays[day - 1] else stringResource(R.string.text_error)
}