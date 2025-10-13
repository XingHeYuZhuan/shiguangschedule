package com.xingheyuzhuan.shiguangschedule.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider

/**
 * 小组件固定颜色配置 - 基于网页样式设计
 * 移除Material You动态取色，使用固定配色方案
 */
object WidgetColors {
    // 背景色
    val background = ColorProvider(Color(0xFFFFFFFF)) // 白色背景
    val cardBackground = ColorProvider(Color(0xFFFFFFFF)) // 卡片背景
    
    // 主色调 - 顶部日历栏
    val primary = ColorProvider(Color(0xFF3498DB)) // 蓝色
    val onPrimary = ColorProvider(Color(0xFFFFFFFF)) // 白色文字
    
    // 文字颜色
    val textPrimary = ColorProvider(Color(0xFF2C3E50)) // 深色主文字
    val textSecondary = ColorProvider(Color(0xFF7F8C8D)) // 灰色次要文字
    val textTertiary = ColorProvider(Color(0xFF95A5A6)) // 浅灰色辅助文字
    val textHint = ColorProvider(Color(0xFFBDC3C7)) // 提示文字
    
    // 边框和分割线
    val border = ColorProvider(Color(0xFFE0E0E0)) // 边框
    val divider = ColorProvider(Color(0xFFECF0F1)) // 分割线
    
    // 左侧课程标识色 - 多种颜色供选择
    val courseIndicator1 = ColorProvider(Color(0xFF3498DB)) // 蓝色
    private val courseIndicator2 = ColorProvider(Color(0xFFE74C3C)) // 红色
    private val courseIndicator3 = ColorProvider(Color(0xFF9B59B6)) // 紫色
    private val courseIndicator4 = ColorProvider(Color(0xFF2ECC71)) // 绿色
    private val courseIndicator5 = ColorProvider(Color(0xFFF39C12)) // 橙色
    
    // 卡片次要容器色（用于课程卡片背景）
    val secondaryContainer = ColorProvider(Color(0xFFF8F9FA)) // 浅灰色背景
    val onSecondaryContainer = ColorProvider(Color(0xFF2C3E50)) // 深色文字
    
    // 获取课程指示器颜色（根据索引循环）
    fun getCourseIndicatorColor(index: Int): ColorProvider {
        return when (index % 5) {
            0 -> courseIndicator1
            1 -> courseIndicator2
            2 -> courseIndicator3
            3 -> courseIndicator4
            else -> courseIndicator5
        }
    }
}

