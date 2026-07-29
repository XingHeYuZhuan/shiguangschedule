package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.R

/**
 * 导航 3 版本的底部导航栏
 * @param currentDestination 当前所在的 Destination 对象
 * @param onTabSelected 当用户点击 Tab 时的回调
 * @param isTransparent 是否开启透明模式（用于课表背景图展示）
 * @param contentColor 自定义内容颜色（通常来自课表样式的文字颜色）
 * @param modifier 外部传入的修饰符，用于支持折叠动画同步
 */
@Composable
fun BottomNavigationBar(
    currentDestination: Destination,
    onTabSelected: (Destination) -> Unit,
    modifier: Modifier = Modifier,
    isTransparent: Boolean = false,
    contentColor: Color? = null
) {
    // 定义底部三个主入口及其对应的文本和图标
    val navItems = listOf(
        Triple(stringResource(R.string.nav_today_schedule), Destination.TodaySchedule, Icons.Filled.ViewAgenda to Icons.Outlined.ViewAgenda),
        Triple(stringResource(R.string.nav_course_schedule), Destination.CourseSchedule, Icons.Filled.ViewWeek to Icons.Outlined.ViewWeek),
        Triple(stringResource(R.string.nav_settings), Destination.Settings, Icons.Filled.AccountCircle to Icons.Outlined.AccountCircle)
    )

    val iconSize = 24.dp
    val textSize = 12.sp

    val finalContentColor = contentColor ?: MaterialTheme.colorScheme.onSurface
    val finalSubTextColor = finalContentColor.copy(alpha = 0.7f)

    NavigationBar(
        containerColor = if (isTransparent) Color.Transparent else MaterialTheme.colorScheme.surface,
        tonalElevation = if (isTransparent) 0.dp else 3.dp,
        modifier = modifier
    ) {
        navItems.forEach { (label, destination, icons) ->
            // 检查当前目的地类型是否匹配
            val isSelected = currentDestination::class == destination::class

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        onTabSelected(destination)
                    }
                },
                icon = {
                    val (selectedIcon, unselectedIcon) = icons
                    val icon = if (isSelected) selectedIcon else unselectedIcon
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(iconSize)
                    )
                },
                label = { Text(label, fontSize = textSize) },
                colors = NavigationBarItemDefaults.colors(
                    // 透明模式下隐藏指示器背景（那个椭圆），纯色模式下保留（方便识别）
                    indicatorColor = if (isTransparent) Color.Transparent else MaterialTheme.colorScheme.secondaryContainer,

                    // 只要自定义了颜色 (contentColor != null)，就应用 finalContentColor
                    selectedIconColor = if (contentColor != null) finalContentColor else MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = if (contentColor != null) finalContentColor else MaterialTheme.colorScheme.onSurface,
                    unselectedIconColor = if (contentColor != null) finalSubTextColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = if (contentColor != null) finalSubTextColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BottomNavigationBarPreview() {
    MaterialTheme {
        BottomNavigationBar(
            currentDestination = Destination.Settings,
            onTabSelected = {}
        )
    }
}