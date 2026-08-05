package com.xingheyuzhuan.shiguangschedule.ui.components

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object GlobalToastState {
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun emit(message: String) {
        _messages.tryEmit(message)
    }
}

actual fun showPlatformToast(message: String) {
    GlobalToastState.emit(message)
}