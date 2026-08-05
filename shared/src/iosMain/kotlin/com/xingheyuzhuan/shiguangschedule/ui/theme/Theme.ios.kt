package com.xingheyuzhuan.shiguangschedule.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import platform.UIKit.UIApplication
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.UIStatusBarStyleLightContent
import platform.UIKit.UIWindow
import platform.UIKit.setStatusBarStyle
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

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
        dispatch_async(dispatch_get_main_queue()) {
            val style = if (darkTheme) {
                UIStatusBarStyleLightContent
            } else {
                UIStatusBarStyleDarkContent
            }
            UIApplication.sharedApplication.setStatusBarStyle(style, animated = true)

            val uiColor = if (darkTheme) {
                platform.UIKit.UIColor.blackColor
            } else {
                platform.UIKit.UIColor.whiteColor
            }

            UIApplication.sharedApplication.windows.forEach { window ->
                (window as? UIWindow)?.backgroundColor = uiColor
            }
        }
    }
}

actual val supportsDynamicColor: Boolean = false