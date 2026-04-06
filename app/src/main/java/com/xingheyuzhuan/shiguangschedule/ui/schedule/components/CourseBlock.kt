package com.xingheyuzhuan.shiguangschedule.ui.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.BorderTypeProto
import com.xingheyuzhuan.shiguangschedule.ui.schedule.MergedCourseBlock

/**
 * 渲染单个课程块的 UI 组件。
 */
@Composable
fun CourseBlock(
    mergedBlock: MergedCourseBlock,
    style: ScheduleGridStyleComposed,
    modifier: Modifier = Modifier,
    startTime: String? = null
) {
    val firstCourse = mergedBlock.courses.firstOrNull()
    val isDarkTheme = isSystemInDarkTheme() // 获取当前主题模式

    // 尝试获取颜色索引 (colorInt)
    val colorIndex = firstCourse?.course?.colorInt
        // 检查索引是否在映射表范围内，否则返回 null
        ?.takeIf { it in style.courseColorMaps.indices }

    // 适配后的课程颜色，如果 colorIndex 存在
    val courseColorAdapted: Color? = colorIndex?.let { index ->
        val baseColorMap = style.courseColorMaps[index]
        if (isDarkTheme) {
            baseColorMap.dark
        } else {
            baseColorMap.light
        }
    }

    val fallbackColorAdapted: Color = if (isDarkTheme) {
        style.courseColorMaps.first().dark
    } else {
        style.courseColorMaps.first().light
    }

    // --- 冲突颜色列表提取 ---
    val conflictColors = remember(mergedBlock.courses, isDarkTheme) {
        mergedBlock.courses.map { cw ->
            val idx = cw.course.colorInt.coerceIn(style.courseColorMaps.indices)
            if (isDarkTheme) style.courseColorMaps[idx].dark else style.courseColorMaps[idx].light
        }
    }

    val blockColor = if (mergedBlock.isConflict) {
        (if (isDarkTheme) Color.Black else Color.White).copy(alpha = style.courseBlockAlpha)
    } else {
        (courseColorAdapted ?: fallbackColorAdapted).copy(alpha = style.courseBlockAlpha)
    }

    val textColor = MaterialTheme.colorScheme.onSurface

    // 字体大小计算逻辑
    val s13 = (13 * style.fontScale).sp
    val s12 = (12 * style.fontScale).sp
    val s10 = (10 * style.fontScale).sp

    val customStartTime = firstCourse?.course?.customStartTime
    val customEndTime = firstCourse?.course?.customEndTime
    val customTimeString = if (customStartTime != null && customEndTime != null) {
        "$customStartTime - $customEndTime"
    } else {
        null
    }
    val isCustomTimeCourse = customTimeString != null

    // 使用系统标准轮廓色
    val themeBorderColor = MaterialTheme.colorScheme.outline

    // 边框逻辑处理
    val shape = RoundedCornerShape(style.courseBlockCornerRadius)
    val borderModifier = when (style.borderType) {
        BorderTypeProto.BORDER_TYPE_SOLID -> {
            Modifier.border(1.dp, themeBorderColor.copy(alpha = style.courseBlockAlpha), shape)
        }
        BorderTypeProto.BORDER_TYPE_DASHED -> {
            Modifier.drawBehind {
                val strokeWidth = 1.dp.toPx()
                val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
                drawOutline(
                    outline = shape.createOutline(size, layoutDirection, this),
                    color = themeBorderColor.copy(alpha = style.courseBlockAlpha),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = strokeWidth,
                        pathEffect = dashPathEffect
                    )
                )
            }
        }
        else -> Modifier
    }

    // 对齐逻辑处理
    val horizontalAlignment = if (style.textAlignCenterHorizontal) Alignment.CenterHorizontally else Alignment.Start
    val verticalArrangement = if (style.textAlignCenterVertical) Arrangement.Center else Arrangement.Top
    val textAlign = if (style.textAlignCenterHorizontal) TextAlign.Center else TextAlign.Start

    Box(
        modifier = modifier
            .padding(style.courseBlockOuterPadding)
            .then(borderModifier) // 应用边框
            .clip(shape)
            .background(color = blockColor)
            .then(
                if (mergedBlock.isConflict) {
                    Modifier.drawBehind {
                        val stripeBrush = Brush.linearGradient(
                            colors = if (conflictColors.size > 1) {
                                val list = mutableListOf<Color>()
                                conflictColors.forEach { color ->
                                    val adjustedColor = color.copy(alpha = style.courseBlockAlpha)
                                    list.add(adjustedColor)
                                    list.add(adjustedColor)
                                }
                                list
                            } else {
                                listOf(conflictColors[0].copy(alpha = style.courseBlockAlpha), conflictColors[0].copy(alpha = style.courseBlockAlpha))
                            },
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height),
                            tileMode = TileMode.Repeated
                        )
                        drawRect(brush = stripeBrush)
                    }
                } else Modifier
            )
    ) {
        // 原始内容层
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(style.courseBlockInnerPadding),
            horizontalAlignment = horizontalAlignment, // 水平对齐
            verticalArrangement = verticalArrangement   // 垂直对齐
        ) {
            if (mergedBlock.isConflict && !style.overlapStyleToggle) {
                // 未开启样式切换：居中显示大字提示
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.label_courses_overlap, mergedBlock.courses.size),
                        fontSize = s13,
                        fontWeight = FontWeight.Black,
                        color = textColor,
                        style = TextStyle(
                            shadow = Shadow(
                                color = (if (isDarkTheme) Color.Black else Color.White).copy(alpha = 0.6f),
                                offset = Offset(2f, 2f),
                                blurRadius = 8f
                            )
                        )
                    )
                }
            } else {
                // 时间显示
                if (isCustomTimeCourse) {
                    Text(
                        text = customTimeString,
                        fontSize = s10,
                        color = textColor.copy(alpha = 0.8f),
                        fontWeight = FontWeight.SemiBold,
                        textAlign = textAlign,
                        style = TextStyle(lineHeight = 1.em)
                    )
                } else if (style.showStartTime && startTime != null) {
                    Text(
                        text = startTime,
                        fontSize = s10,
                        color = textColor.copy(alpha = 0.8f),
                        fontWeight = FontWeight.SemiBold,
                        textAlign = textAlign,
                        style = TextStyle(lineHeight = 1.em)
                    )
                }

                // 课程名称
                Text(
                    text = firstCourse?.course?.name ?: "",
                    fontSize = s13,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = textAlign,
                    modifier = Modifier.weight(1f, fill = false),
                    style = TextStyle(lineHeight = 1.2.em)
                )

                // 教师
                if (!style.hideTeacher) {
                    val teacher = firstCourse?.course?.teacher ?: ""
                    if (teacher.isNotBlank()) {
                        Text(
                            text = teacher,
                            fontSize = s10,
                            color = textColor,
                            textAlign = textAlign,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(lineHeight = 1.em)
                        )
                    }
                }

                // 地点
                if (!style.hideLocation) {
                    val position = firstCourse?.course?.position ?: ""
                    if (position.isNotBlank()) {
                        val prefix = if (style.removeLocationAt) "" else "@"
                        Text(
                            text = "$prefix$position",
                            fontSize = s10,
                            color = textColor,
                            textAlign = textAlign,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(lineHeight = 1.em)
                        )
                    }
                }

                // 开启样式切换后，在文字内容下方追加重叠提示
                if (mergedBlock.isConflict) {
                    Text(
                        text = stringResource(R.string.label_courses_overlap, mergedBlock.courses.size),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor.copy(alpha = 0.9f),
                        textAlign = textAlign,
                        style = TextStyle(
                            lineHeight = 1.em,
                            shadow = Shadow(
                                color = (if (isDarkTheme) Color.Black else Color.White).copy(alpha = 0.4f),
                                blurRadius = 4f
                            )
                        )
                    )
                }
            }
        }

        // 非本周课程标记
        if (mergedBlock.hasNonCurrentWeekCourses) {
            Icon(
                painter = painterResource(id = R.drawable.stacks_24px),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(16.dp),
                tint = textColor.copy(alpha = 0.6f)
            )
        }

        // 视觉降级蒙版层
        if (mergedBlock.isVisualDemoted) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = (if (isDarkTheme) Color.Black else Color.White).copy(alpha = 0.618f)
                    )
                    .drawBehind {
                        val stripeWidth = 5.dp.toPx()
                        val stripeColor = (if (isDarkTheme) Color.White else Color.Black).copy(alpha = 0.06f)
                        val brush = Brush.linearGradient(
                            0.0f to stripeColor,
                            0.45f to stripeColor,
                            0.55f to Color.Transparent,
                            1.0f to Color.Transparent,
                            start = Offset(0f, 0f),
                            end = Offset(stripeWidth, stripeWidth),
                            tileMode = TileMode.Repeated
                        )
                        drawRect(brush = brush)
                    }
            )
        }
    }
}