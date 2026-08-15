package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xingheyuzhuan.shiguangschedule.Destination
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.account_circle_24px
import shiguangschedule.shared.generated.resources.account_circle_filled_24px
import shiguangschedule.shared.generated.resources.nav_course_schedule
import shiguangschedule.shared.generated.resources.nav_settings
import shiguangschedule.shared.generated.resources.nav_today_schedule
import shiguangschedule.shared.generated.resources.view_agenda_24px
import shiguangschedule.shared.generated.resources.view_agenda_filled_24px
import shiguangschedule.shared.generated.resources.view_week_24px
import shiguangschedule.shared.generated.resources.view_week_filled_24px

/**
 * 底部导航栏组件
 *
 * @param currentDestination 当前路由 Destination 对象
 * @param onTabSelected 点击 Tab 时的回调
 * @param isTransparent 是否开启透明模式
 * @param contentColor 自定义文本与图标颜色
 * @param modifier 布局修饰符
 */
@Composable
fun BottomNavigationBar(
    currentDestination: Destination,
    onTabSelected: (Destination) -> Unit,
    modifier: Modifier = Modifier,
    isTransparent: Boolean = false,
    contentColor: Color? = null
) {
    val navItems = listOf(
        Triple(
            stringResource(Res.string.nav_today_schedule),
            Destination.TodaySchedule,
            vectorResource(Res.drawable.view_agenda_filled_24px) to vectorResource(Res.drawable.view_agenda_24px)
        ),
        Triple(
            stringResource(Res.string.nav_course_schedule),
            Destination.CourseSchedule,
            vectorResource(Res.drawable.view_week_filled_24px) to vectorResource(Res.drawable.view_week_24px)
        ),
        Triple(
            stringResource(Res.string.nav_settings),
            Destination.Settings,
            vectorResource(Res.drawable.account_circle_filled_24px) to vectorResource(Res.drawable.account_circle_24px)
        )
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
            currentDestination = Destination.CourseSchedule,
            onTabSelected = {}
        )
    }
}