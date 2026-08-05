package com.xingheyuzhuan.shiguangschedule.ui.components

import android.content.Context
import android.widget.Toast
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private object AndroidToastHelper : KoinComponent {
    val context: Context by inject()
}

actual fun showPlatformToast(message: String) {
    Toast.makeText(
        AndroidToastHelper.context,
        message,
        Toast.LENGTH_SHORT
    ).show()
}