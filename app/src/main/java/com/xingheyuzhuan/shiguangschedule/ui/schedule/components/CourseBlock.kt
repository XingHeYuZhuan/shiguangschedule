package com.xingheyuzhuan.shiguangschedule.ui.schedule.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.xingheyuzhuan.shiguangschedule.ui.schedule.MergedCourseBlock
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.ScheduleGridDefaults.getDarkerColor

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

    // 从配置文件中获取颜色值并应用透明度
    val blockColor = if (mergedBlock.isConflict) {
        // 冲突课程使用配置文件中的颜色和透明度
        ScheduleGridDefaults.ConflictCourseColor.copy(alpha = ScheduleGridDefaults.CourseBlockAlpha)
    } else {
        // 非冲突课程，从课程颜色获取并应用透明度，如果颜色不存在则使用配置文件中的默认颜色
        firstCourse?.course?.colorInt?.let { Color(it).copy(alpha = ScheduleGridDefaults.CourseBlockAlpha) }
            ?: ScheduleGridDefaults.DefaultCourseColor.copy(alpha = ScheduleGridDefaults.CourseBlockAlpha)
    }

    // 根据背景色计算文本颜色，实现深浅对比
    //val textColor = getDarkerColor(blockColor, factor = ScheduleGridDefaults.TextDarkenFactor)
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
                // 冲突课程：按比例分配空间
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
                    text = "（冲突）",
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