package com.xingheyuzhuan.shiguangschedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import com.xingheyuzhuan.shiguangschedule.ui.settings.update.UpdateRepoScreen
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
        // 这些顶级页面的转场是瞬间完成的
        composable(
            Screen.CourseSchedule.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            WeeklyScheduleScreen(navController = navController)
        }
        composable(
            Screen.Settings.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            SettingsScreen(navController = navController)
        }
        composable(
            Screen.TodaySchedule.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            TodayScheduleScreen(navController = navController)
        }

        // 所有子页面的转场也都是瞬间完成的
        composable(
            Screen.TimeSlotSettings.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            TimeSlotManagementScreen(onBackClick = { navController.popBackStack() })
        }
        composable(
            Screen.ManageCourseTables.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            ManageCourseTablesScreen(navController = navController)
        }
        composable(
            Screen.SchoolSelection.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            SchoolSelectionScreen(navController = navController)
        }
        composable(
            route = Screen.WebView.route,
            arguments = listOf(navArgument("schoolId") { type = NavType.StringType }),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) { backStackEntry ->
            val schoolId = backStackEntry.arguments?.getString("schoolId")
            if (schoolId != null) {
                WebViewScreen(navController = navController, schoolId = schoolId)
            } else {
                Text("School ID 参数缺失或无效")
                navController.popBackStack()
            }
        }
        composable(
            Screen.NotificationSettings.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            NotificationSettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.AddEditCourse.route,
            arguments = listOf(
                navArgument("courseId") {
                    type = NavType.StringType
                    nullable = true
                }
            ),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId")
            AddEditCourseScreen(
                courseId = courseId,
                onNavigateBack = { navController.popBackStack() },
                initialDay = -1,
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
            ),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
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
        composable(
            Screen.CourseTableConversion.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            CourseTableConversionScreen(navController = navController)
        }
        composable(
            Screen.MoreOptions.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            MoreOptionsScreen(navController = navController)
        }
        composable(
            Screen.OpenSourceLicenses.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            OpenSourceLicensesScreen (navController = navController)
        }
        composable(
            Screen.UpdateRepo.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            UpdateRepoScreen(navController = navController)
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