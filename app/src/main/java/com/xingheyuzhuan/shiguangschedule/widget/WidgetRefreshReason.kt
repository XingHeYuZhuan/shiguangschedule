package com.xingheyuzhuan.shiguangschedule.widget

/**
 * 桌面小组件刷新原因。
 *
 * v1.3.0 将所有主动刷新统一标记原因，便于诊断“设置已变但小组件未变”的问题。
 */
enum class WidgetRefreshReason(val displayName: String) {
    SYSTEM_UPDATE("系统请求刷新"),
    PERIODIC("周期兜底刷新"),
    FULL_DATA_SYNC("完整数据同步"),
    COURSE_DATA_CHANGED("课程数据变化"),
    COURSE_TABLE_SWITCHED("当前课表切换"),
    COURSE_TABLE_CONFIG_CHANGED("课表配置变化"),
    STYLE_CHANGED("样式配置变化"),
    WIDGET_STYLE_PREVIEW("桌面小组件样式预览"),
    WIDGET_STYLE_CHANGED("桌面小组件样式保存"),
    CALENDAR_ALARM("课程提醒触发"),
    MANUAL("用户手动刷新"),
    UNKNOWN("未知原因");

    companion object {
        fun fromName(name: String?): WidgetRefreshReason = entries.firstOrNull { it.name == name } ?: UNKNOWN
    }
}
