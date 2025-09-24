// com/xingheyuzhuan/shiguangschedule/Navigation.kt
package com.xingheyuzhuan.shiguangschedule

sealed class Screen(val route: String) {
    object CourseSchedule : Screen("course_schedule")

    object Settings : Screen("settings")

    object TodaySchedule : Screen("today_schedule")

    object TimeSlotSettings : Screen("time_slot_settings")

    object ManageCourseTables : Screen("manage_course_tables")

    object SchoolSelection : Screen("school_selection")

    object CourseTableConversion : Screen("course_table_conversion")

    // 修改：WebView 页面，使用路径参数传递 schoolId
    object WebView : Screen("web_view/{schoolId}") { // 路由现在是路径参数
        fun createRoute(schoolId: String) = "web_view/$schoolId" // 修改创建路由的方式
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
}