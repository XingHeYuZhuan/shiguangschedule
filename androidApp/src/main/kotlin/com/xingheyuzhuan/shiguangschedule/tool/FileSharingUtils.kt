package com.xingheyuzhuan.shiguangschedule.tool

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK

/**
 * 封装了调用系统文件分享页面的逻辑。
 *
 * @param context 当前上下文。
 * @param uri 要分享的文件的 Uri。这个 Uri 必须是一个 content:// Uri，通常由 FileProvider 生成。
 * @param mimeType 文件的 MIME 类型，例如 "application/json" 或 "text/calendar"。
 */
fun shareFile(context: Context, uri: Uri, mimeType: String) {
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, uri)
        type = mimeType
        addFlags(FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(FLAG_ACTIVITY_NEW_TASK)
    }

    // 检查是否有应用可以处理这个 Intent
    if (shareIntent.resolveActivity(context.packageManager) != null) {
        val chooser = Intent.createChooser(shareIntent, "分享文件")
        context.startActivity(chooser)
    } else {
        // 没有应用可以处理
    }
}