package com.xingheyuzhuan.shiguangschedule.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
import androidx.core.graphics.createBitmap

// 扩展函数，将 Dp 转换为像素 (px)
fun Dp.toPx(context: Context): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.value,
        context.resources.displayMetrics
    )
}


/**
 * 核心功能：动态绘制单行文本到 Bitmap，并在超出指定最大宽度时添加省略号。
 *
 * @param text 要绘制的文本内容。
 * @param fontSizeDp 期望的字体大小 (DP)。
 * @param colorProvider 文本颜色。
 * @param context 上下文。
 * @param maxWidthDp 文本内容允许的最大宽度 (DP)。 必须手动传入容器的预估宽度。
 * @param textAlign 文本在 Bitmap 内部的对齐方式 (Paint.Align)。
 * @return 包含绘制文本的 Bitmap。
 */
fun drawEllipsizedTextToBitmap(
    text: String,
    fontSizeDp: Dp,
    colorProvider: ColorProvider,
    context: Context,
    maxWidthDp: Dp,
    textAlign: Paint.Align = Paint.Align.LEFT,
): Bitmap {
    val textSizePx = fontSizeDp.toPx(context)
    val colorInt = colorProvider.getColor(context).toArgb()
    val maxWidthPx = maxWidthDp.toPx(context)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = textSizePx
        color = colorInt
        isSubpixelText = true
        this.textAlign = textAlign
    }

    // 1. 测量文本和度量
    val fontMetrics = paint.fontMetrics
    val ellipsis = "..."
    val ellipsisWidth = paint.measureText(ellipsis)
    val totalTextWidth = paint.measureText(text)

    var measuredText = text
    var finalDrawWidthPx = totalTextWidth

    // 2. 检查并截断文本，添加省略号
    if (totalTextWidth > maxWidthPx) {
        // 留出省略号和安全边距（4px 是为了防止 breakText 计算导致的边界裁剪）
        val availableWidth = maxWidthPx - ellipsisWidth - 4f

        val charCount = paint.breakText(text, true, availableWidth, null)

        measuredText = if (charCount > 0) {
            // 截断文本，去除末尾空格，然后添加省略号
            text.take(charCount).trimEnd() + ellipsis
        } else {
            // 最小化显示：如果连第一个字符都放不下，只显示省略号
            ellipsis
        }
        finalDrawWidthPx = maxWidthPx
    }

    // 3. 确定 Bitmap 尺寸 - 关键修正
    // 使用 finalDrawWidthPx 来确定 Bitmap 宽度，实现 wrapContent 或 maxWidthPx
    // 使用 coerceAtLeast(1) 确保宽度至少为 1
    val bitmapWidth = finalDrawWidthPx.toInt().coerceAtLeast(1)
    val bitmapHeight = (fontMetrics.bottom - fontMetrics.top).toInt()

    if (bitmapWidth <= 0 || bitmapHeight <= 0) {
        return createBitmap(1, 1)
    }

    // 4. 创建 Bitmap 和 Canvas
    val bitmap = createBitmap(bitmapWidth, bitmapHeight)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.TRANSPARENT)

    // 5. 绘制文本
    val baselineY = -fontMetrics.top

    val startX = when (textAlign) {
        Paint.Align.LEFT -> 0f
        Paint.Align.CENTER -> bitmapWidth / 2f
        Paint.Align.RIGHT -> bitmapWidth.toFloat()
    }

    canvas.drawText(measuredText, startX, baselineY, paint)

    return bitmap
}


/**
 * 将文本渲染为 Bitmap，支持大字体、宽度限制和省略号。
 *
 * @param text 要显示的文本内容。
 * @param fontSizeDp 期望的字体大小。
 * @param color 字体颜色。
 * @param maxWidthDp 文本允许的最大宽度。
 * @param textAlign 文本在 Bitmap 内部的对齐方式 (Paint.Align)。
 * @param modifier Composable 修饰符。
 */
@Composable
fun EllipsizedBitmapText(
    text: String,
    fontSizeDp: Dp,
    color: ColorProvider,
    maxWidthDp: Dp,
    textAlign: Paint.Align = Paint.Align.LEFT,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    // 传入对齐参数
    val bitmap = drawEllipsizedTextToBitmap(text, fontSizeDp, color, context, maxWidthDp, textAlign)

    Image(
        provider = ImageProvider(bitmap),
        contentDescription = null,
        modifier = modifier.wrapContentSize()
    )
}