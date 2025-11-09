package com.xingheyuzhuan.shiguangschedule.ui.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseImportExport.COURSE_COLOR_MAPS
import com.xingheyuzhuan.shiguangschedule.ui.schedule.MergedCourseBlock

/**
 * 渲染单个课程块的 UI 组件。
 * 它负责展示课程信息、颜色，并处理冲突标记。
 */
@Composable
fun CourseBlock(
    mergedBlock: MergedCourseBlock,
    modifier: Modifier = Modifier
) {
    val firstCourse = mergedBlock.courses.firstOrNull()
    val isDarkTheme = isSystemInDarkTheme() // 获取当前主题模式

    val conflictColorAdapted = if (isDarkTheme) {
        ScheduleGridDefaults.ConflictCourseColorDark // 使用深色冲突色
    } else {
        ScheduleGridDefaults.ConflictCourseColor // 使用浅色冲突色
    }

    // 尝试获取颜色索引 (colorInt)
    val colorIndex = firstCourse?.course?.colorInt
        // 检查索引是否在映射表范围内，否则返回 null
        ?.takeIf { it in COURSE_COLOR_MAPS.indices }

    // 适配后的课程颜色，如果 colorIndex 存在
    val courseColorAdapted: Color? = colorIndex?.let { index ->
        val baseColorMap = COURSE_COLOR_MAPS[index]
        if (isDarkTheme) {
            baseColorMap.dark
        } else {
            baseColorMap.light
        }
    }

    val fallbackColorAdapted: Color = if (isDarkTheme) {
        COURSE_COLOR_MAPS.first().dark
    } else {
        COURSE_COLOR_MAPS.first().light
    }

    val blockColor = if (mergedBlock.isConflict) {
        conflictColorAdapted.copy(alpha = ScheduleGridDefaults.CourseBlockAlpha)
    } else {
        (courseColorAdapted ?: fallbackColorAdapted)
            .copy(alpha = ScheduleGridDefaults.CourseBlockAlpha)
    }

    // 根据背景色计算文本颜色，实现深浅对比
    // val textColor = getDarkerColor(blockColor, factor = ScheduleGridDefaults.TextDarkenFactor)
    val textColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .padding(ScheduleGridDefaults.CourseBlockOuterPadding)
            .clip(RoundedCornerShape(ScheduleGridDefaults.CourseBlockCornerRadius))
            .background(color = blockColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(ScheduleGridDefaults.CourseBlockInnerPadding)
        ) {
            if (mergedBlock.isConflict) {
                mergedBlock.courses.forEach { course ->
                    Text(
                        text = course.course.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                Text(
                    text = stringResource(R.string.label_conflict),
                    fontSize = 10.sp,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp)
                )
            } else {
                // 非冲突课程：按比例分配空间
                Text(
                    text = firstCourse?.course?.name ?: "",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                    style = TextStyle(lineHeight = 1.2.em)
                )
                Text(
                    text = firstCourse?.course?.teacher ?: "",
                    fontSize = 10.sp,
                    color = textColor,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(lineHeight = 1.em)
                )
                Text(
                    text = "@${firstCourse?.course?.position ?: ""}",
                    fontSize = 10.sp,
                    color = textColor,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(lineHeight = 1.em)
                )
            }
        }
    }
}