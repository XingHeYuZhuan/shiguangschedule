package com.xingheyuzhuan.shiguangschedule.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.TypedValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.layout.wrapContentSize
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.toArgb

// 扩展函数，将 Dp 转换为像素 (px)
fun Dp.toPx(context: Context): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.value,
        context.resources.displayMetrics
    )
}

/**
 * 动态绘制单行文本到 Bitmap。
 * 这是绕过 TextView 字体大小限制的核心逻辑。
 * * @param text 要绘制的文本内容。
 * @param fontSizeDp 期望的字体大小 (DP)。
 * @param colorProvider 文本颜色。
 * @param context 上下文。
 * @return 包含绘制文本的 Bitmap。
 */
fun drawTextToBitmap(
    text: String,
    fontSizeDp: Dp,
    colorProvider: ColorProvider,
    context: Context,
): Bitmap {
    val textSizePx = fontSizeDp.toPx(context)
    val colorInt = colorProvider.getColor(context).toArgb()

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = textSizePx
        color = colorInt
        // 确保文本不会有额外的上下间距，这与 TextView 的 includeFontPadding=false 对应
        isSubpixelText = true
    }

    // 1. 测量文本边界和字体度量
    val textBounds = Rect()
    paint.getTextBounds(text, 0, text.length, textBounds)
    val fontMetrics = paint.fontMetrics

    // 2. 确定 Bitmap 尺寸
    val padding = 2 // 少量额外 padding 确保不裁剪
    val bitmapWidth = textBounds.width() + padding
    // 高度：使用 ascent 到 descent 的距离 (确保完整包含所有字母部分)
    val bitmapHeight = (fontMetrics.bottom - fontMetrics.top).toInt()

    if (bitmapWidth <= 0 || bitmapHeight <= 0) {
        // 返回一个最小的透明 Bitmap，防止崩溃
        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }

    // 3. 创建 Bitmap 和 Canvas
    val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.TRANSPARENT) // 设置透明背景

    // 4. 绘制文本
    // 计算 y 坐标：将文本顶部 (top) 放在 canvas.top (0)
    // fontMetrics.top 是负值，因此 -fontMetrics.top 得到的是 baslineY
    // 这样可以确保文本的最高点与 Bitmap 的顶部对齐
    val baselineY = -fontMetrics.top
    canvas.drawText(text, (padding / 2).toFloat(), baselineY, paint)

    return bitmap
}

/**
 * [ScaledBitmapText] - 用于显示大尺寸文本的 Composable。
 * 它将文本渲染为 Bitmap，然后通过 Glance Image 组件显示，以绕过字体大小上限。
 *
 * @param text 要显示的文本内容。
 * @param fontSizeDp 期望的字体大小。
 * @param color 字体颜色。
 * @param modifier Composable 修饰符。
 */
@Composable
fun ScaledBitmapText(
    text: String,
    fontSizeDp: Dp,
    color: ColorProvider,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    // 在 Composable 中调用 Bitmap 绘制函数
    val bitmap = drawTextToBitmap(text, fontSizeDp, color, context)

    Image(
        provider = ImageProvider(bitmap),
        contentDescription = null,
        // 使用 wrapContentSize 让 Image 严格包裹 Bitmap 尺寸，防止挤压或拉伸
        modifier = modifier.wrapContentSize()
    )
}