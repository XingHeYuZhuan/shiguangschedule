package com.xingheyuzhuan.shiguangschedule.ui.settings.additional

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

actual object PlatformLocaleManager {

    actual fun setLanguageTag(tag: String) {
        val locales = if (tag.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    actual fun getCurrentLanguageTag(): String {
        return AppCompatDelegate.getApplicationLocales().toLanguageTags()
    }
}