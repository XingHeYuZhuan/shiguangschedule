package com.xingheyuzhuan.shiguangschedule.ui.settings.conversion

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.tool.shareFile
import java.io.File
import java.io.FileOutputStream

@Composable
fun ShareFileDialog(
    shareData: Triple<Uri, String, String>,
    context: Context,
    onDismiss: () -> Unit,
    onCopyFailed: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title_file_saved)) },
        text = { Text(stringResource(R.string.dialog_text_file_saved_share_prompt)) },
        confirmButton = {
            TextButton(onClick = {
                val (publicUri, mimeType, defaultFilename) = shareData

                val userDefinedFilename = context.contentResolver.query(
                    publicUri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) cursor.getString(nameIndex) else null
                    } else null
                } ?: defaultFilename

                val shareTempDir = File(context.cacheDir, "share_temp").apply {
                    if (!exists()) mkdirs()
                }
                val tempFile = File(shareTempDir, userDefinedFilename)

                try {
                    context.contentResolver.openInputStream(publicUri)?.use { input ->
                        FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    onCopyFailed()
                    onDismiss()
                    return@TextButton
                }

                val shareUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFile
                )
                shareFile(context, shareUri, mimeType)
                onDismiss()
            }) {
                Text(stringResource(R.string.action_share))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}