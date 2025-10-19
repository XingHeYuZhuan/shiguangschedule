package com.xingheyuzhuan.shiguangschedule

import android.net.Uri // 必须导入 Uri 来进行 URL 编码

sealed class Screen(val route: String) {
    object CourseSchedule : Screen("course_schedule")

    object Settings : Screen("settings")

    object TodaySchedule : Screen("today_schedule")

    object TimeSlotSettings : Screen("time_slot_settings")

    object ManageCourseTables : Screen("manage_course_tables")

    object SchoolSelectionListScreen : Screen("school_selection")

    object CourseTableConversion : Screen("course_table_conversion")

    object AdapterSelection : Screen("adapterSelection/{schoolId}/{schoolName}/{categoryNumber}/{resourceFolder}") {

        fun createRoute(schoolId: String, schoolName: String, categoryNumber: Int, resourceFolder: String): String {
            val encodedSchoolName = Uri.encode(schoolName)
            val encodedResourceFolder = Uri.encode(resourceFolder)
            return "adapterSelection/$schoolId/$encodedSchoolName/$categoryNumber/$encodedResourceFolder"
        }
    }
    object WebView : Screen("web_view/{initialUrl}/{assetJsPath}") {
        fun createRoute(initialUrl: String?, assetJsPath: String?): String {
            val urlParam = Uri.encode(initialUrl ?: "about:blank")
            val pathParam = Uri.encode(assetJsPath ?: "")
            return "web_view/$urlParam/$pathParam"
        }
    }

    object NotificationSettings : Screen("notification_settings")

    object AddEditCourse : Screen("add_edit_course_route/{courseId}") {
        // 用于编辑现有课程，将 courseId 传递给路由
        fun createRouteWithCourseId(courseId: String): String {
            return "add_edit_course_route/$courseId"
        }
        fun createRouteForNewCourse(day: Int, section: Int): String {
            return "add_edit_course_route?day=$day&section=$section"
        }
    }

    object MoreOptions : Screen("more_options")

    object OpenSourceLicenses : Screen("open_source_licenses")

    object UpdateRepo : Screen("update_repo")

    object TweakSchedule : Screen("tweak_schedule")

    object ContributionList : Screen("contribution_list")
}