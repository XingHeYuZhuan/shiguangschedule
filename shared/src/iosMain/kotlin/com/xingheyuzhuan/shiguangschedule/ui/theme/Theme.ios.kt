package com.xingheyuzhuan.shiguangschedule.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.UIStatusBarStyleLightContent
import platform.UIKit.UIWindow
import platform.UIKit.setStatusBarStyle

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
    SideEffect {
        val style = if (darkTheme) {
            UIStatusBarStyleLightContent
        } else {
            UIStatusBarStyleDarkContent
        }

        UIApplication.sharedApplication.setStatusBarStyle(style, animated = true)

        val uiColor = if (darkTheme) UIColor.blackColor else UIColor.whiteColor

        @Suppress("DEPRECATION")
        UIApplication.sharedApplication.windows.forEach { window ->
            (window as? UIWindow)?.backgroundColor = uiColor
        }
    }
}

actual val supportsDynamicColor: Boolean = false