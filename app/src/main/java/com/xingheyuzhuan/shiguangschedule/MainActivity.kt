package com.xingheyuzhuan.shiguangschedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xingheyuzhuan.shiguangschedule.ui.schedule.WeeklyScheduleScreen
import com.xingheyuzhuan.shiguangschedule.ui.schoolselection.SchoolSelectionScreen
import com.xingheyuzhuan.shiguangschedule.ui.schoolselection.WebViewScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.coursetables.ManageCourseTablesScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.notification.NotificationSettingsScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.SettingsScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.additional.MoreOptionsScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.additional.OpenSourceLicensesScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.conversion.CourseTableConversionScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.course.AddEditCourseScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.time.TimeSlotManagementScreen
import com.xingheyuzhuan.shiguangschedule.ui.theme.shiguangscheduleTheme
import com.xingheyuzhuan.shiguangschedule.ui.today.TodayScheduleScreen


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            shiguangscheduleTheme {
                AppNavigation()
            }
        }
    }
}


@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.CourseSchedule.route,
        modifier = Modifier.fillMaxSize()
    ){
        composable(Screen.CourseSchedule.route) {
            WeeklyScheduleScreen(navController = navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
        composable(Screen.TodaySchedule.route) {
            TodayScheduleScreen(navController = navController)
        }
        composable(Screen.TimeSlotSettings.route) {
            TimeSlotManagementScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Screen.ManageCourseTables.route) {
            ManageCourseTablesScreen(navController = navController)
        }
        composable(Screen.SchoolSelection.route) {
            SchoolSelectionScreen(navController = navController)
        }
        composable(
            route = Screen.WebView.route,
            arguments = listOf(navArgument("schoolId") { type = NavType.StringType })
        ) { backStackEntry ->
            val schoolId = backStackEntry.arguments?.getString("schoolId")
            if (schoolId != null) {
                WebViewScreen(navController = navController, schoolId = schoolId)
            } else {
                Text("School ID 参数缺失或无效")
                navController.popBackStack()
            }
        }
        composable(Screen.NotificationSettings.route) {
            NotificationSettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.AddEditCourse.route,
            arguments = listOf(
                navArgument("courseId") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId")
            AddEditCourseScreen(
                courseId = courseId,
                onNavigateBack = { navController.popBackStack() },
                initialDay = -1, // 默认值
                initialSection = -1
            )
        }
        composable(
            route = "add_edit_course_route?day={day}&section={section}",
            arguments = listOf(
                navArgument("day") {
                    type = NavType.IntType
                    defaultValue = -1
                },
                navArgument("section") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val initialDay = backStackEntry.arguments?.getInt("day")
            val initialSection = backStackEntry.arguments?.getInt("section")
            AddEditCourseScreen(
                courseId = null,
                initialDay = initialDay,
                initialSection = initialSection,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.CourseTableConversion.route) {
            CourseTableConversionScreen(navController = navController)
        }
        composable(Screen.MoreOptions.route) {
            MoreOptionsScreen(navController = navController)
        }
        composable(Screen.OpenSourceLicenses.route) {
            OpenSourceLicensesScreen (navController = navController)
        }
    }
}

@Preview(showBackground = true, name = "App Navigation Light")
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "App Navigation Dark")
@Composable
fun PreviewAppNavigation() {
    shiguangscheduleTheme {
        AppNavigation()
    }
}