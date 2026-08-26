package com.xingheyuzhuan.shiguangschedule

import androidx.compose.ui.window.ComposeUIViewController

private var isKoinInitialized = false

private fun ensureKoinInitialized() {
    if (!isKoinInitialized) {
        initKoin()
        isKoinInitialized = true
    }
}

fun MainViewController() = ComposeUIViewController {
    ensureKoinInitialized()
    App()
}
