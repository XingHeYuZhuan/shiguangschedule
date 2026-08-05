package com.xingheyuzhuan.shiguangschedule.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.xingheyuzhuan.shiguangschedule.data.model.AppSettingsModel
import com.xingheyuzhuan.shiguangschedule.data.model.AppThemeMode

/**
 * 定义一个用于全局同步深色模式状态的 Local 变量
 */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

/**
 * 外部调用的快捷主题函数
 * 自动根据 AppSettingsModel 处理所有主题逻辑
 */
@Composable
fun ShiguangScheduleTheme(
    settings: AppSettingsModel,
    content: @Composable () -> Unit
) {
    val darkTheme = when (settings.themeMode) {
        AppThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        ShiguangScheduleTheme(
            darkTheme = darkTheme,
            dynamicColor = settings.useDynamicColor,
            customLightPrimary = Color(settings.customLightPrimary),
            customDarkPrimary = Color(settings.customDarkPrimary),
            content = content
        )
    }
}

/**
 * 核心主题实现函数（跨平台通用）
 */
@Composable
fun ShiguangScheduleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    customLightPrimary: Color = Purple40,
    customDarkPrimary: Color = Purple80,
    content: @Composable () -> Unit
) {
    val colorScheme = rememberColorScheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        customLightPrimary = customLightPrimary,
        customDarkPrimary = customDarkPrimary
    )

    // 应用平台特定的窗口与系统栏外观控制
    SetupPlatformThemeEffects(colorScheme = colorScheme, darkTheme = darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * 平台特定的配色生成声明
 */
@Composable
expect fun rememberColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    customLightPrimary: Color,
    customDarkPrimary: Color
): ColorScheme

/**
 * 平台特定的窗口与系统栏外观控制声明
 */
@Composable
expect fun SetupPlatformThemeEffects(
    colorScheme: ColorScheme,
    darkTheme: Boolean
)

/**
 * 平台特定的能力：当前系统/平台是否支持 Dynamic Color (动态取色)
 */
expect val supportsDynamicColor: Boolean