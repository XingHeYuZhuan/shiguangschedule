package com.xingheyuzhuan.shiguangschedule.ui.components

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

@Composable
fun BottomNavigationBar(navController: NavHostController, currentRoute: String?) {
    val navItems = listOf(
        "今日课表" to Screen.TodaySchedule.route,
        "课表" to Screen.CourseSchedule.route,
        "我的" to Screen.Settings.route
    )

    NavigationBar {
        navItems.forEach { (label, route) ->
            NavigationBarItem(
                selected = currentRoute == route,
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
                    Icon(icon, contentDescription = label)
                },
                label = { Text(label) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BottomNavigationBarPreview() {
    BottomNavigationBar(navController = rememberNavController(), currentRoute = "today")
}
