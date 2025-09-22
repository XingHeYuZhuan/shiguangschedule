package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.MyApplication
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CourseTablePickerDialog(
    title: String,
    onDismissRequest: () -> Unit,
    onTableSelected: (CourseTable) -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as MyApplication
    val courseTableRepository = app.courseTableRepository
    val appSettingsRepository = app.appSettingsRepository

    val courseTables by courseTableRepository.getAllCourseTables().collectAsState(initial = emptyList())
    val appSettings by appSettingsRepository.getAppSettings().collectAsState(initial = null)

    var selectedTable by remember { mutableStateOf<CourseTable?>(null) }

    LaunchedEffect(courseTables, appSettings) {
        if (selectedTable == null && appSettings?.currentCourseTableId != null) {
            selectedTable = courseTables.find { it.id == appSettings?.currentCourseTableId }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = {
            Column {
                if (courseTables.isEmpty()) {
                    Text(text = "暂无课表可选择。", style = MaterialTheme.typography.bodyLarge)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(courseTables) { courseTable ->
                            val isCurrentActive = courseTable.id == appSettings?.currentCourseTableId
                            val isSelectedForDialog = courseTable.id == selectedTable?.id

                            CourseTablePickerCard(
                                courseTable = courseTable, // 直接传入 CourseTable
                                isSelected = isSelectedForDialog,
                                isCurrentActive = isCurrentActive,
                                onCardClick = {
                                    selectedTable = it
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedTable?.let { onTableSelected(it) }
                    onDismissRequest()
                },
                enabled = selectedTable != null
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            Button(onClick = onDismissRequest) {
                Text("取消")
            }
        }
    )
}

@Composable
fun CourseTablePickerCard(
    courseTable: CourseTable, // 直接接收 CourseTable
    isSelected: Boolean,
    isCurrentActive: Boolean,
    onCardClick: (CourseTable) -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick(courseTable) },
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                isCurrentActive -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = courseTable.name, style = MaterialTheme.typography.titleMedium)
                Text(text = "ID: ${courseTable.id.substring(0, 8)}...", style = MaterialTheme.typography.bodySmall)
                Text(
                    text = "创建于: ${dateFormatter.format(Date(courseTable.createdAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            if (isCurrentActive) {
                Text("当前", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}