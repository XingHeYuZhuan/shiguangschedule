package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> AlphabetIndexerList(
    data: List<T>,
    getInitial: (T) -> String,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    headerContent: (@Composable LazyItemScope.() -> Unit)? = null,
    itemContent: @Composable (T) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // 1. 提取去重后的首字母
    val initials = remember(data) {
        data.map { getInitial(it).uppercase() }.distinct().sorted()
    }

    // 2. 位置映射表：计算每个字母 Header 对应的 LazyColumn 索引
    val indexMap = remember(data, initials, headerContent) {
        val map = mutableMapOf<String, Int>()
        var currentIndex = 0
        if (headerContent != null) currentIndex++
        var lastInitial = ""
        data.forEach { item ->
            val initial = getInitial(item).uppercase()
            if (initial != lastInitial) {
                map[initial] = currentIndex
                lastInitial = initial
                currentIndex++
            }
            currentIndex++
        }
        map
    }

    Row(modifier = modifier.fillMaxSize()) {
        // --- 左侧：主列表 ---
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            headerContent?.let { item { it() } }
            var currentInitial = ""
            data.forEach { item ->
                val initial = getInitial(item).uppercase()
                if (initial != currentInitial) {
                    stickyHeader { DefaultAlphabetHeader(initial) }
                    currentInitial = initial
                }
                item { itemContent(item) }
            }
        }

        // --- 右侧：通讯录标准索引条 ---
        if (initials.isNotEmpty()) {
            var barHeight by remember { mutableIntStateOf(0) }

            // 处理跳转逻辑的辅助函数
            val handleSelection: (Float) -> Unit = { yPosition ->
                if (barHeight > 0) {
                    // 计算当前手指在哪个字母区间：坐标 / 总高度 * 字母总数
                    val index = (yPosition / barHeight * initials.size)
                        .toInt()
                        .coerceIn(0, initials.lastIndex)

                    val target = initials[index]
                    coroutineScope.launch {
                        indexMap[target]?.let { actualIndex ->
                            // 快速跳转到对应位置
                            lazyListState.scrollToItem(actualIndex)
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(32.dp)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center // 这里控制索引条在垂直方向居中
            ) {
                Column(
                    modifier = Modifier
                        .wrapContentHeight() // 关键：高度根据内容自适应，不再强制撑满
                        .onGloballyPositioned { coordinates ->
                            barHeight = coordinates.size.height // 获取自适应后的实际高度
                        }
                        .pointerInput(initials) {
                            // 支持点击跳转
                            detectTapGestures { offset -> handleSelection(offset.y) }
                        }
                        .pointerInput(initials) {
                            // 支持滑动跳转（标准通讯录效果）
                            detectDragGestures { change, _ ->
                                handleSelection(change.position.y)
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp) // 字母间的微调间距
                ) {
                    initials.forEach { initial ->
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DefaultAlphabetHeader(initial: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}