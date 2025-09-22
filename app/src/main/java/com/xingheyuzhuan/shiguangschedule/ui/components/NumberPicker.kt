package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.*
import kotlin.math.abs

@Composable
fun <T> NativeNumberPicker(
    values: List<T>,
    selectedValue: T,
    onValueChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 48.dp,
    visibleItemsCount: Int = 3,
    itemTextOffsetY: Dp = 0.dp,
    dividerColor: Color = MaterialTheme.colorScheme.primary,
    dividerSize: Dp = 1.dp,
) {
    // 校验可见项数量必须为≥3的奇数
    require(visibleItemsCount >= 3 && visibleItemsCount % 2 != 0) {
        "visibleItemsCount must be an odd number and at least 3"
    }

    // 计算初始选中项的索引位置
    val initialSelectedIndex = remember(values, selectedValue) {
        values.indexOf(selectedValue).coerceAtLeast(0)
    }

    // 列表滚动状态管理
    val listState = rememberLazyListState(initialSelectedIndex)
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }

    // 视觉中心索引状态（用于文字样式变化）
    var visuallyCenteredIndex by remember { mutableIntStateOf(initialSelectedIndex) }

    // 初始化时滚动到选中位置
    LaunchedEffect(initialSelectedIndex) {
        listState.animateScrollToItem(initialSelectedIndex)
    }

    // 处理拖拽结束后的吸附逻辑
    LaunchedEffect(listState) {
        listState.interactionSource.interactions
            .filterIsInstance<DragInteraction.Stop>()
            .combine(snapshotFlow { listState.isScrollInProgress }) { _, inProgress ->
                !inProgress
            }
            .filter { it }
            .collectLatest {
                // 计算当前滚动位置
                val (firstIndex, offset) = listState.run {
                    firstVisibleItemIndex to firstVisibleItemScrollOffset
                }

                // 根据滚动偏移量确定目标索引
                val targetIndex = if (offset > itemHeightPx / 2)
                    (firstIndex + 1).coerceAtMost(values.lastIndex)
                else firstIndex

                // 更新选中状态并触发回调
                visuallyCenteredIndex = targetIndex
                if (targetIndex in values.indices) onValueChange(values[targetIndex])
                listState.animateScrollToItem(targetIndex)
            }
    }

    // 实时更新视觉中心索引（用于滚动时动态样式）
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            snapshotFlow {
                listState.run {
                    if (firstVisibleItemScrollOffset > itemHeightPx / 2)
                        (firstVisibleItemIndex + 1).coerceAtMost(values.lastIndex)
                    else
                        firstVisibleItemIndex
                }
            }.distinctUntilChanged().collect { visuallyCenteredIndex = it }
        }
    }

    // 主布局容器
    Box(
        modifier = modifier
            .height(itemHeight * visibleItemsCount)
            .clipToBounds()
    ) {
        // 绘制上下两条指示线
        listOf(-1, 1).forEach { direction ->
            HorizontalDivider(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = itemHeight / 2 * direction),
                color = dividerColor,
                thickness = dividerSize
            )
        }

        // 可滚动列表
        LazyColumn(
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight * visibleItemsCount),
            contentPadding = PaddingValues(vertical = itemHeight * (visibleItemsCount / 2))
        ) {
            itemsIndexed(values) { index, item ->
                // 根据与中心项的距离计算文字样式
                val distance = abs(index - visuallyCenteredIndex)
                val (fontSize, textColor) = when (distance) {
                    0 -> 30.sp to MaterialTheme.colorScheme.primary
                    1 -> 25.sp to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    else -> 20.sp to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                }

                Text(
                    text = item.toString(), // 使用 toString() 来处理任何类型
                    fontSize = fontSize,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth()
                        .offset(y = itemTextOffsetY)
                )
            }
        }
    }
}