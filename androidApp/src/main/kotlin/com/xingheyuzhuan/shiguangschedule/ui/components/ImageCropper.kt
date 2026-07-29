package com.xingheyuzhuan.shiguangschedule.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.xingheyuzhuan.shiguangschedule.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 通用图片裁剪组件
 * 如果未来更换第三方库，仅需在此文件内替换实现逻辑
 */
@Composable
fun ImageCropper(
    uri: Uri,
    aspectRatio: Float, // 传入目标比例，例如屏幕比例
    onCropConfirmed: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var containerSize by remember { mutableStateOf(Size.Zero) }
    val density = LocalDensity.current

    // 加载图片
    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use {
                    bitmap = BitmapFactory.decodeStream(it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    BackHandler { onDismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .zIndex(10f),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { containerSize = it.size.toSize() }
            ) {
                val cw = constraints.maxWidth.toFloat()
                val ch = constraints.maxHeight.toFloat()

                // 裁剪区域计算
                val buttonAreaHeight = with(density) { 120.dp.toPx() }
                val topPadding = with(density) { 60.dp.toPx() }
                val availableHeight = ch - buttonAreaHeight - topPadding

                val cropWidthCandidate = cw * 0.75f
                val cropHeightCandidate = cropWidthCandidate / aspectRatio

                val (cropWidth, cropHeight) = if (cropHeightCandidate > availableHeight) {
                    val h = availableHeight * 0.85f
                    Pair(h * aspectRatio, h)
                } else {
                    Pair(cropWidthCandidate, cropHeightCandidate)
                }

                val cropRect = Rect(
                    left = (cw - cropWidth) / 2,
                    top = topPadding + (availableHeight - cropHeight) / 2,
                    right = (cw + cropWidth) / 2,
                    bottom = topPadding + (availableHeight + cropHeight) / 2
                )

                // 初始化缩放与位置
                LaunchedEffect(bitmap, containerSize) {
                    if (bitmap != null && containerSize != Size.Zero) {
                        val imgW = bitmap!!.width.toFloat()
                        val imgH = bitmap!!.height.toFloat()
                        scale = max(cropWidth / imgW, cropHeight / imgH)
                        val centerX = cropRect.left + cropWidth / 2
                        val centerY = cropRect.top + cropHeight / 2
                        offset = Offset(centerX - cw / 2, centerY - ch / 2)
                    }
                }

                // 边界约束函数
                fun constrainOffset(newOffset: Offset, newScale: Float): Offset {
                    val imgW = bitmap!!.width * newScale
                    val imgH = bitmap!!.height * newScale
                    val minX = cropRect.right - (cw + imgW) / 2
                    val maxX = cropRect.left - (cw - imgW) / 2
                    val minY = cropRect.bottom - (ch + imgH) / 2
                    val maxY = cropRect.top - (ch - imgH) / 2
                    return Offset(
                        newOffset.x.coerceIn(min(minX, maxX), max(minX, maxX)),
                        newOffset.y.coerceIn(min(minY, maxY), max(minY, maxY))
                    )
                }

                // 手势与渲染层
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val minScale = max(cropWidth / bitmap!!.width, cropHeight / bitmap!!.height)
                                val newScale = (scale * zoom).coerceAtLeast(minScale)
                                scale = newScale
                                offset = constrainOffset(offset + pan, newScale)
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val imgW = bitmap!!.width * scale
                        val imgH = bitmap!!.height * scale
                        val startX = (cw - imgW) / 2 + offset.x
                        val startY = (ch - imgH) / 2 + offset.y

                        drawImage(
                            image = bitmap!!.asImageBitmap(),
                            dstOffset = IntOffset(startX.roundToInt(), startY.roundToInt()),
                            dstSize = IntSize(imgW.roundToInt(), imgH.roundToInt())
                        )
                    }

                    // 遮罩层
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val path = androidx.compose.ui.graphics.Path().apply { addRect(cropRect) }
                        clipPath(path, clipOp = ClipOp.Difference) {
                            drawRect(Color.Black.copy(alpha = 0.75f))
                        }
                    }

                    // 裁剪框白边
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(cropRect.left.roundToInt(), cropRect.top.roundToInt()) }
                            .size(with(density) { cropWidth.toDp() }, with(density) { cropHeight.toDp() })
                            .border(1.5.dp, Color.White.copy(alpha = 0.8f))
                    )
                }

                // 确认按钮
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            val imgW = bitmap!!.width * scale
                            val imgH = bitmap!!.height * scale
                            val startX = (cw - imgW) / 2 + offset.x
                            val startY = (ch - imgH) / 2 + offset.y

                            val relativeX = (cropRect.left - startX) / scale
                            val relativeY = (cropRect.top - startY) / scale
                            val relativeW = cropWidth / scale
                            val relativeH = cropHeight / scale

                            val cropped = Bitmap.createBitmap(
                                bitmap!!,
                                relativeX.roundToInt().coerceIn(0, bitmap!!.width - 1),
                                relativeY.roundToInt().coerceIn(0, bitmap!!.height - 1),
                                relativeW.roundToInt().coerceAtMost(bitmap!!.width - relativeX.roundToInt()),
                                relativeH.roundToInt().coerceAtMost(bitmap!!.height - relativeY.roundToInt())
                            )
                            onCropConfirmed(cropped)
                        },
                        shape = RoundedCornerShape(24.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                    ) {
                        Text(stringResource(R.string.action_confirm_crop))
                    }
                }
            }
        } else {
            CircularProgressIndicator(color = Color.White)
        }
    }
}