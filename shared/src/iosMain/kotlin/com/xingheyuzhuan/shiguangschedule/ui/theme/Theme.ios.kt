package com.xingheyuzhuan.shiguangschedule.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import com.xingheyuzhuan.shiguangschedule.data.model.AppThemeMode
import platform.UIKit.UIApplication
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.UIStatusBarStyleLightContent
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
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
    darkTheme: Boolean,
    themeMode: AppThemeMode
) {
    SideEffect {
        val uiStyle = when (themeMode) {
            AppThemeMode.FOLLOW_SYSTEM -> UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified
            AppThemeMode.LIGHT -> UIUserInterfaceStyle.UIUserInterfaceStyleLight
            AppThemeMode.DARK -> UIUserInterfaceStyle.UIUserInterfaceStyleDark
        }

        val style = if (darkTheme) {
            UIStatusBarStyleLightContent
        } else {
            UIStatusBarStyleDarkContent
        }

        UIApplication.sharedApplication.connectedScenes.forEach { scene ->
            (scene as? UIWindowScene)?.windows?.forEach { window ->
                (window as? UIWindow)?.let { win ->
                    win.overrideUserInterfaceStyle = uiStyle
                    win.rootViewController?.setNeedsStatusBarAppearanceUpdate()
                }
            }
        }

        @Suppress("DEPRECATION")
        UIApplication.sharedApplication.setStatusBarStyle(style, animated = true)
    }
}

actual val supportsDynamicColor: Boolean = false