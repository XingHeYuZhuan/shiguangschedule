package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.xingheyuzhuan.shiguangschedule.Screen
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun BottomNavigationBar(navController: NavHostController, currentRoute: String?) {
    val navItems = listOf(
        "今日课表" to Screen.TodaySchedule.route,
        "课表" to Screen.CourseSchedule.route,
        "我的" to Screen.Settings.route
    )

    NavigationBar {
        navItems.forEach { (label, route) ->
            val isSelected = currentRoute == route
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (currentRoute != route) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    val icon = when (route) {
                        Screen.TodaySchedule.route -> Icons.Filled.Today
                        Screen.CourseSchedule.route -> Icons.Filled.Schedule
                        Screen.Settings.route -> Icons.Filled.Person
                        else -> Icons.Filled.Schedule
                    }
                    Icon(icon, contentDescription = label,modifier = if (isSelected) Modifier.size(28.8.dp) else Modifier.size(24.dp))
                },
                label = {
                    Text(
                        label,
                        fontSize = if (isSelected) 14.4.sp else 12.sp
                    )
                },
                // 在这里设置颜色来移除背景指示器
                colors = NavigationBarItemDefaults.colors(
                    // 隐藏胶囊形指示器的颜色
                    indicatorColor = Color.Transparent,
                    // 设置未选中状态的颜色
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    // 设置选中状态的颜色
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BottomNavigationBarPreview() {
    BottomNavigationBar(navController = rememberNavController(), currentRoute = "today")
}
