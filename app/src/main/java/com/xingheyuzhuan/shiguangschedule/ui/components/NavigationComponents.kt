package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewWeek
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
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.ViewWeek


@Composable
fun BottomNavigationBar(navController: NavHostController, currentRoute: String?) {

    val navItems = listOf(
        "今日课表" to Screen.TodaySchedule.route,
        "课表" to Screen.CourseSchedule.route,
        "我的" to Screen.Settings.route
    )

    val iconSize = 24.dp
    val textSize = 12.sp


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
                    val (selectedIcon, unselectedIcon) = when (route) {
                        Screen.TodaySchedule.route -> Icons.Filled.ViewAgenda to Icons.Outlined.ViewAgenda
                        Screen.CourseSchedule.route -> Icons.Filled.ViewWeek to Icons.Outlined.ViewWeek
                        Screen.Settings.route -> Icons.Filled.AccountCircle to Icons.Outlined.AccountCircle
                        else -> Icons.Filled.AccountCircle to Icons.Outlined.AccountCircle
                    }

                    val icon = if (isSelected) selectedIcon else unselectedIcon

                    Icon(
                        icon,
                        contentDescription = label,
                        modifier = Modifier.size(iconSize)
                    )
                },
                label = {
                    Text(
                        label,
                        fontSize = textSize
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent,

                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,

                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,

                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BottomNavigationBarPreview() {
    BottomNavigationBar(navController = rememberNavController(), currentRoute = Screen.Settings.route)
}