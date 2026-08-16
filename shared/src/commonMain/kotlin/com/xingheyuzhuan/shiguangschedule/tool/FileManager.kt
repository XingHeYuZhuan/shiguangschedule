package com.xingheyuzhuan.shiguangschedule.tool

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * 全局统一文件与平台资源管理器接口
 * 负责解耦底层 OS 文件系统，提供简洁的原生风格 API
 */
interface FileManager {
    /**
     * 唤起系统图片选择器
     */
    fun pickImage()

    /**
     * 唤起系统文件导入器（如导入课程表 .json / .ics 备份）
     * @param allowedExtensions 允许的文件后缀名（如 "json", "ics"）
     */
    fun importFile(allowedExtensions: List<String> = emptyList())

    /**
     * 唤起系统文件导出/保存器（如导出备份文件）
     * @param defaultFileName 默认文件名（如 "schedule_backup.json"）
     * @param bytes 要保存的文件二进制数据
     */
    fun exportFile(defaultFileName: String, bytes: ByteArray)
}

/**
 * 平台文件/资源回调监听器
 */
data class FileManagerCallbacks(
    val onImagePicked: ((ImageBitmap?) -> Unit)? = null,
    val onFileImported: ((bytes: ByteArray?, fileName: String?) -> Unit)? = null,
    val onFileExported: ((success: Boolean) -> Unit)? = null
)

/**
 * 在 Composable 中remember 并获取全局 FileManager 实例
 * 业务页面只需调用此函数，传入需要的回调即可
 */
@Composable
expect fun rememberFileManager(callbacks: FileManagerCallbacks): FileManager