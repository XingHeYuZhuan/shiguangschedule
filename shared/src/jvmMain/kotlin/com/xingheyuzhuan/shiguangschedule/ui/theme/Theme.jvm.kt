package com.xingheyuzhuan.shiguangschedule.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
actual fun rememberColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    customLightPrimary: Color,
    customDarkPrimary: Color
): ColorScheme {
    return if (darkTheme) {
        darkColorScheme(primary = customDarkPrimary)
    } else {
        lightColorScheme(primary = customLightPrimary)
    }
}

@Composable
actual fun SetupPlatformThemeEffects(
    colorScheme: ColorScheme,
    darkTheme: Boolean
) {
    // Desktop 端窗口外观交由 Compose Desktop 的 Window 属性控制，此处无需额外处理
    // 如要实现自绘边框需要补充实现
}


actual val supportsDynamicColor: Boolean = false