package com.xingheyuzhuan.shiguangschedule.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.xingheyuzhuan.shiguangschedule.data.model.AppThemeMode
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.UIStatusBarStyleLightContent
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
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
    return rememberMaterialKolorScheme(
        darkTheme = darkTheme,
        seedColor = if (darkTheme) customDarkPrimary else customLightPrimary
    )
}

@Composable
actual fun SetupPlatformThemeEffects(
    colorScheme: ColorScheme,
    darkTheme: Boolean,
    themeMode: AppThemeMode
) {
    SideEffect {
        dispatch_async(dispatch_get_main_queue()) {
            val style = if (darkTheme) {
                UIStatusBarStyleLightContent
            } else {
                UIStatusBarStyleDarkContent
            }
            UIApplication.sharedApplication.setStatusBarStyle(style, animated = true)

            val interfaceStyle: UIUserInterfaceStyle = when (themeMode) {
                AppThemeMode.FOLLOW_SYSTEM -> UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified
                AppThemeMode.LIGHT -> UIUserInterfaceStyle.UIUserInterfaceStyleLight
                AppThemeMode.DARK -> UIUserInterfaceStyle.UIUserInterfaceStyleDark
            }

            keyWindow()?.let { window ->
                window.overrideUserInterfaceStyle = interfaceStyle
                window.backgroundColor = colorScheme.background.toUIColor()
            }
        }
    }
}

private fun keyWindow(): UIWindow? {
    val windowScene = UIApplication.sharedApplication.connectedScenes
        .firstOrNull { it is UIWindowScene } as? UIWindowScene

    return windowScene?.windows
        .orEmpty()
        .mapNotNull { it as? UIWindow }
        .firstOrNull { it.isKeyWindow() }
}

private fun Color.toUIColor(): UIColor {
    val argb = toArgb()
    return UIColor.colorWithRed(
        red = ((argb shr 16) and 0xFF) / 255.0,
        green = ((argb shr 8) and 0xFF) / 255.0,
        blue = (argb and 0xFF) / 255.0,
        alpha = ((argb ushr 24) and 0xFF) / 255.0
    )
}

actual val supportsDynamicColor: Boolean = false
