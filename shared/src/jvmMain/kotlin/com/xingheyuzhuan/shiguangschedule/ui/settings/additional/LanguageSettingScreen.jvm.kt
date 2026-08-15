package com.xingheyuzhuan.shiguangschedule.ui.settings.additional

import com.xingheyuzhuan.shiguangschedule.data.model.JvmBasePreferences
import java.util.Locale

/**
 * JVM / Desktop 平台的语言配置管理器 actual 实现
 * 继承 [JvmBasePreferences]，数据统一落盘保存在 AppData/files/locale_settings.properties
 */
actual object PlatformLocaleManager : JvmBasePreferences("locale_settings.properties") {

    private const val PREF_KEY_LANGUAGE = "app_language_tag"

    actual fun setLanguageTag(tag: String) {
        if (tag.isEmpty()) {
            putString(PREF_KEY_LANGUAGE, null)
            Locale.setDefault(Locale.getDefault())
        } else {
            putString(PREF_KEY_LANGUAGE, tag)
            Locale.setDefault(Locale.forLanguageTag(tag))
        }
    }

    actual fun getCurrentLanguageTag(): String {
        return getString(PREF_KEY_LANGUAGE, "")
    }
}