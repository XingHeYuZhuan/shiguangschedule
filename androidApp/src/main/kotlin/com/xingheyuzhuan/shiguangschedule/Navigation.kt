package com.xingheyuzhuan.shiguangschedule

import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.NavMetadataKey
import kotlinx.serialization.Serializable

/**
 * 导航元数据 Key 定义
 */
object ShiguangNavMetadata {
    /** 作用：标记是否为一级主界面，用于控制切换动画（主界面间无过渡） */
    object IsMainScreenKey : NavMetadataKey<Boolean>
}

/**
 * 应用所有目的地（页面）的定义
 * 采用 Kotlin Serialization 实现类型安全的参数传递
 */
@Serializable
sealed interface Destination : NavKey {

    // --- 一级导航页面（底栏对应页面，通常无滑动动画） ---

    @Serializable data object CourseSchedule : Destination
    @Serializable data object Settings : Destination
    @Serializable data object TodaySchedule : Destination

    // --- 普通功能页面（二级页面，通常使用标准滑动动画） ---

    @Serializable data object TimeSlotSettings : Destination
    @Serializable data object ManageCourseTables : Destination
    @Serializable data object SchoolSelectionListScreen : Destination
    @Serializable data object CourseTableConversion : Destination
    @Serializable data object NotificationSettings : Destination
    @Serializable data object MoreOptions : Destination
    @Serializable data object OpenSourceLicenses : Destination
    @Serializable data object UpdateRepo : Destination
    @Serializable data object QuickActions : Destination
    @Serializable data object TweakSchedule : Destination
    @Serializable data object QuickDelete : Destination
    @Serializable data object ContributionList : Destination
    @Serializable data object CourseManagementList : Destination
    @Serializable data object StyleSettings : Destination
    @Serializable data object ThemeSettings : Destination
    @Serializable data object BackupAndRestore : Destination
    // --- 动态传参页面 ---

    @Serializable
    data class AdapterSelection(
        val schoolId: String,
        val schoolName: String,
        val categoryNumber: Int,
        val resourceFolder: String
    ) : Destination

    @Serializable
    data class WebView(
        val initialUrl: String? = "about:blank",
        val assetJsPath: String? = null
    ) : Destination

    @Serializable
    data class AddEditCourse(
        val courseId: String? = null
    ) : Destination

    @Serializable
    data class CourseManagementDetail(
        val courseName: String
    ) : Destination
}

/**
 * 作用：快速判断目的地是否属于“一级导航”，供 NavEntry 注入元数据
 */
val Destination.isMainScreen: Boolean
    get() = this is Destination.CourseSchedule ||
            this is Destination.Settings ||
            this is Destination.TodaySchedule