// src/main/java/com/xingheyuzhuan/shiguangschedule/ui/schedule/components/WeekSelectorBottomSheet.kt

package com.xingheyuzhuan.shiguangschedule.ui.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 周选择器底部动作条。
 *
 * @param totalWeeks 学期总周数。
 * @param currentWeek 当前的自然周数。
 * @param onWeekSelected 当用户选择周次时触发的回调。
 * @param onDismissRequest 当底部动作条被关闭时触发的回调。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekSelectorBottomSheet(
    totalWeeks: Int,
    currentWeek: Int?,
    selectedWeek: Int?,
    onWeekSelected: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    val gridState = rememberLazyGridState()

    // 默认滚动到当前周
    LaunchedEffect(currentWeek) {
        if (currentWeek != null && currentWeek > 0) {
            // 注意：网格索引同样从 0 开始
            gridState.animateScrollToItem(currentWeek - 1)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "选择周次",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )

            // 网格状的周次选择器
            LazyVerticalGrid(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                columns = GridCells.Adaptive(minSize = 60.dp),
                state = gridState,
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(totalWeeks) { weekIndex ->
                    val weekNumber = weekIndex + 1
                    val isCurrentWeek = weekNumber == currentWeek
                    val isSelectedWeek = weekNumber == selectedWeek

                    // 根据周次状态决定颜色
                    val backgroundColor = when {
                        isSelectedWeek -> MaterialTheme.colorScheme.primary
                        isCurrentWeek -> MaterialTheme.colorScheme.primaryContainer
                        else -> Color.Transparent
                    }
                    val textColor = when {
                        isSelectedWeek -> MaterialTheme.colorScheme.onPrimary
                        isCurrentWeek -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(CircleShape)
                            .background(backgroundColor)
                            .clickable { onWeekSelected(weekNumber) }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$weekNumber",
                            color = textColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}