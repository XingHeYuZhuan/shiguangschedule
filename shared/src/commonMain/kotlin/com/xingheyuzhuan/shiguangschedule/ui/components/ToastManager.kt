package com.xingheyuzhuan.shiguangschedule.ui.components

expect fun showPlatformToast(message: String)

/**
 * 全局 Toast 管理器
 */
object ToastManager {
    fun show(message: String) {
        if (message.isNotBlank()) {
            showPlatformToast(message)
        }
    }
}