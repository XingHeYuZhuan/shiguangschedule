package com.xingheyuzhuan.shiguangschedule

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xingheyuzhuan.shiguangschedule.ui.components.BottomNavigationBar
import com.xingheyuzhuan.shiguangschedule.ui.schedule.WeeklyScheduleScreen
import com.xingheyuzhuan.shiguangschedule.ui.schoolselection.list.AdapterSelectionScreen
import com.xingheyuzhuan.shiguangschedule.ui.schoolselection.list.SchoolSelectionListScreen
import com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web.WebViewScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.SettingsScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.additional.MoreOptionsScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.additional.OpenSourceLicensesScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.contribution.ContributionScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.conversion.CourseTableConversionScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.course.AddEditCourseScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.coursemanagement.COURSE_NAME_ARG
import com.xingheyuzhuan.shiguangschedule.ui.settings.coursemanagement.CourseInstanceListScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.coursemanagement.CourseNameListScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.coursetables.ManageCourseTablesScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.notification.NotificationSettingsScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.quickactions.QuickActionsScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.quickactions.delete.QuickDeleteScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.style.StyleSettingsScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.time.TimeSlotManagementScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.quickactions.tweaks.TweakScheduleScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.update.UpdateRepoScreen
import com.xingheyuzhuan.shiguangschedule.ui.theme.ShiguangScheduleTheme
import com.xingheyuzhuan.shiguangschedule.ui.today.TodayScheduleScreen

private const val CONTENT_TRANSITION_DURATION_MS = 280

private enum class TransitionDirection {
    Forward,
    Backward
}

private fun mainRouteIndex(route: String?): Int = when (route) {
    Screen.TodaySchedule.route -> 0
    Screen.CourseSchedule.route -> 1
    Screen.Settings.route -> 2
    else -> -1
}

private fun resolveMainDirection(fromRoute: String?, toRoute: String?): TransitionDirection? {
    val fromIndex = mainRouteIndex(fromRoute)
    val toIndex = mainRouteIndex(toRoute)
    if (fromIndex == -1 || toIndex == -1) return null
    return if (toIndex >= fromIndex) TransitionDirection.Forward else TransitionDirection.Backward
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.resolveDirection(
    defaultDirection: TransitionDirection
): TransitionDirection {
    val fromRoute = initialState.destination.route
    val toRoute = targetState.destination.route
    return resolveMainDirection(fromRoute, toRoute) ?: defaultDirection
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.contentEnterTransition(): EnterTransition {
    val direction = resolveDirection(defaultDirection = TransitionDirection.Forward)
    return slideInHorizontally(
        animationSpec = tween(CONTENT_TRANSITION_DURATION_MS),
        initialOffsetX = { fullWidth ->
            if (direction == TransitionDirection.Forward) fullWidth else -fullWidth
        }
    )
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.contentExitTransition(): ExitTransition {
    val direction = resolveDirection(defaultDirection = TransitionDirection.Forward)
    return slideOutHorizontally(
        animationSpec = tween(CONTENT_TRANSITION_DURATION_MS),
        targetOffsetX = { fullWidth ->
            if (direction == TransitionDirection.Forward) -fullWidth else fullWidth
        }
    )
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.contentPopEnterTransition(): EnterTransition {
    val direction = resolveDirection(defaultDirection = TransitionDirection.Backward)
    return slideInHorizontally(
        animationSpec = tween(CONTENT_TRANSITION_DURATION_MS),
        initialOffsetX = { fullWidth ->
            if (direction == TransitionDirection.Forward) fullWidth else -fullWidth
        }
    )
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.contentPopExitTransition(): ExitTransition {
    val direction = resolveDirection(defaultDirection = TransitionDirection.Backward)
    return slideOutHorizontally(
        animationSpec = tween(CONTENT_TRANSITION_DURATION_MS),
        targetOffsetX = { fullWidth ->
            if (direction == TransitionDirection.Forward) -fullWidth else fullWidth
        }
    )
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ShiguangScheduleTheme {
                AppNavigation()
            }
        }
    }
}


@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val mainRoutes = rememberMainRoutes()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentRoute in mainRoutes) {
                BottomNavigationBar(
                    navController = navController,
                    currentRoute = currentRoute
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.CourseSchedule.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ){
            // 顶级页面也使用左右滑动转场，底部导航固定在外层不参与动画
            composable(
                Screen.CourseSchedule.route,
                enterTransition = { contentEnterTransition() },
                exitTransition = { contentExitTransition() },
                popEnterTransition = { contentPopEnterTransition() },
                popExitTransition = { contentPopExitTransition() }
            ) {
                WeeklyScheduleScreen(navController = navController)
            }
            composable(
                Screen.Settings.route,
                enterTransition = { contentEnterTransition() },
                exitTransition = { contentExitTransition() },
                popEnterTransition = { contentPopEnterTransition() },
                popExitTransition = { contentPopExitTransition() }
            ) {
                SettingsScreen(navController = navController)
            }
            composable(
                Screen.TodaySchedule.route,
                enterTransition = { contentEnterTransition() },
                exitTransition = { contentExitTransition() },
                popEnterTransition = { contentPopEnterTransition() },
                popExitTransition = { contentPopExitTransition() }
            ) {
                TodayScheduleScreen(navController = navController)
            }

            // 子页面使用左右滑动转场
            composable(
                Screen.TimeSlotSettings.route,
                enterTransition = { contentEnterTransition() },
                exitTransition = { contentExitTransition() },
                popEnterTransition = { contentPopEnterTransition() },
                popExitTransition = { contentPopExitTransition() }
            ) {
                TimeSlotManagementScreen(onBackClick = { navController.popBackStack() })
            }
            composable(
                Screen.ManageCourseTables.route,
                enterTransition = { contentEnterTransition() },
                exitTransition = { contentExitTransition() },
                popEnterTransition = { contentPopEnterTransition() },
                popExitTransition = { contentPopExitTransition() }
            ) {
                ManageCourseTablesScreen(navController = navController)
            }
            // 学校选择
            composable(
                Screen.SchoolSelectionListScreen.route,
                enterTransition = { contentEnterTransition() },
                exitTransition = { contentExitTransition() },
                popEnterTransition = { contentPopEnterTransition() },
                popExitTransition = { contentPopExitTransition() }
            ) {
                SchoolSelectionListScreen(navController = navController)
            }

            composable(
                route = "adapterSelection/{schoolId}/{schoolName}/{categoryNumber}/{resourceFolder}",
                arguments = listOf(
                    navArgument("schoolId") { type = NavType.StringType },
                    navArgument("schoolName") { type = NavType.StringType },
                    navArgument("categoryNumber") { type = NavType.IntType },
                    navArgument("resourceFolder") { type = NavType.StringType }
                ),
                enterTransition = { contentEnterTransition() },
                exitTransition = { contentExitTransition() },
                popEnterTransition = { contentPopEnterTransition() },
                popExitTransition = { contentPopExitTransition() }
            ) { backStackEntry ->
            val schoolId = backStackEntry.arguments?.getString("schoolId") ?: ""
            val schoolName = backStackEntry.arguments?.getString("schoolName") ?: "未知学校"
            val categoryNumber = backStackEntry.arguments?.getInt("categoryNumber") ?: 0
            val resourceFolder = backStackEntry.arguments?.getString("resourceFolder") ?: ""

            AdapterSelectionScreen(
                navController = navController,
                schoolId = schoolId,
                schoolName = schoolName,
                categoryNumber = categoryNumber,
                resourceFolder = resourceFolder
            )
            }
            composable(
                route = Screen.WebView.route,
                arguments = listOf(
                    navArgument("initialUrl") { type = NavType.StringType },
                    navArgument("assetJsPath") { type = NavType.StringType }
                ),
                enterTransition = { contentEnterTransition() },
                exitTransition = { contentExitTransition() },
                popEnterTransition = { contentPopEnterTransition() },
                popExitTransition = { contentPopExitTransition() }
            ) { backStackEntry ->
            val initialUrl = backStackEntry.arguments?.getString("initialUrl")
            val assetJsPath = backStackEntry.arguments?.getString("assetJsPath")

            val context = LocalContext.current
            val app = context.applicationContext as MyApplication

            val courseConversionRepository = app.courseConversionRepository
            val timeSlotRepository = app.timeSlotRepository

            WebViewScreen(
                navController = navController,
                initialUrl = initialUrl,
                assetJsPath = assetJsPath,
                courseConversionRepository = courseConversionRepository,
                timeSlotRepository = timeSlotRepository,
                courseScheduleRoute = Screen.CourseSchedule.route,
            )
            }
            composable(
                Screen.NotificationSettings.route,
                enterTransition = { contentEnterTransition() },
                exitTransition = { contentExitTransition() },
                popEnterTransition = { contentPopEnterTransition() },
                popExitTransition = { contentPopExitTransition() }
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
                enterTransition = { contentEnterTransition() },
                exitTransition = { contentExitTransition() },
                popEnterTransition = { contentPopEnterTransition() },
                popExitTransition = { contentPopExitTransition() }
            ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId")

            // 路由参数只处理 courseId (Add/Edit 模式)。
            AddEditCourseScreen(
                courseId = courseId,
                onNavigateBack = { navController.popBackStack() },
            )
            }
            composable(
                Screen.CourseTableConversion.route,
                enterTransition = { contentEnterTransition() },
                exitTransition = { contentExitTransition() },
                popEnterTransition = { contentPopEnterTransition() },
                popExitTransition = { contentPopExitTransition() }
            ) {
                CourseTableConversionScreen(navController = navController)
            }
            composable(
                Screen.MoreOptions.route,
                enterTransition = { contentEnterTransition() },
                exitTransition = { contentExitTransition() },
                popEnterTransition = { contentPopEnterTransition() },
                popExitTransition = { contentPopExitTransition() }
            ) {
                MoreOptionsScreen(navController = navController)
            }
            composable(
                Screen.OpenSourceLicenses.route,
                enterTransition = { contentEnterTransition() },
                exitTransition = { contentExitTransition() },
                popEnterTransition = { contentPopEnterTransition() },
                popExitTransition = { contentPopExitTransition() }
            ) {
                OpenSourceLicensesScreen (navController = navController)
            }
            composable(
                Screen.UpdateRepo.route,
                enterTransition = { contentEnterTransition() },
                exitTransition = { contentExitTransition() },
                popEnterTransition = { contentPopEnterTransition() },
                popExitTransition = { contentPopExitTransition() }
            ) {
                UpdateRepoScreen(navController = navController)
            }
            composable(
                Screen.QuickActions.route,
                enterTransition = { contentEnterTransition() },
                exitTransition = { contentExitTransition() },
                popEnterTransition = { contentPopEnterTransition() },
                popExitTransition = { contentPopExitTransition() }
            ) {
                QuickActionsScreen(navController = navController)
            }
            composable(
                Screen.TweakSchedule.route,
                enterTransition = { contentEnterTransition() },
                exitTransition = { contentExitTransition() },
                popEnterTransition = { contentPopEnterTransition() },
                popExitTransition = { contentPopExitTransition() }
            ) {
                TweakScheduleScreen(navController = navController)
            }
            composable(
                Screen.ContributionList.route,
                enterTransition = { contentEnterTransition() },
                exitTransition = { contentExitTransition() },
                popEnterTransition = { contentPopEnterTransition() },
                popExitTransition = { contentPopExitTransition() }
            ) {
                ContributionScreen(navController = navController)
            }
            // 课程管理 - 一级页面：课程名称列表
            composable(
                Screen.CourseManagementList.route,
                enterTransition = { contentEnterTransition() },
                exitTransition = { contentExitTransition() },
                popEnterTransition = { contentPopEnterTransition() },
                popExitTransition = { contentPopExitTransition() }
            ) {
                // CourseNameListScreen 负责显示不重复的课程名称
                CourseNameListScreen(navController = navController)
            }

            // 课程管理 - 二级页面：课程实例网格
            composable(
                route = Screen.CourseManagementDetail.route,
                arguments = listOf(
                    navArgument(COURSE_NAME_ARG) { type = NavType.StringType }
                ),
                enterTransition = { contentEnterTransition() },
                exitTransition = { contentExitTransition() },
                popEnterTransition = { contentPopEnterTransition() },
                popExitTransition = { contentPopExitTransition() }
            ) { backStackEntry ->
            val courseName = Uri.decode(backStackEntry.arguments?.getString(COURSE_NAME_ARG) ?: "")
            CourseInstanceListScreen(
                courseName = courseName,
                onNavigateBack = { navController.popBackStack() },
                navController = navController
            )
            }
            // 外观定制页面
            composable(
                Screen.StyleSettings.route,
                enterTransition = { contentEnterTransition() },
                exitTransition = { contentExitTransition() },
                popEnterTransition = { contentPopEnterTransition() },
                popExitTransition = { contentPopExitTransition() }
            ) {
                StyleSettingsScreen(navController = navController)
            }
            // 快速删除课程页面
            composable(
                Screen.QuickDelete.route,
                enterTransition = { contentEnterTransition() },
                exitTransition = { contentExitTransition() },
                popEnterTransition = { contentPopEnterTransition() },
                popExitTransition = { contentPopExitTransition() }
            ) {
                QuickDeleteScreen(navController = navController)
            }
        }
    }
}

@Composable
private fun rememberMainRoutes(): Set<String> = setOf(
    Screen.TodaySchedule.route,
    Screen.CourseSchedule.route,
    Screen.Settings.route
)