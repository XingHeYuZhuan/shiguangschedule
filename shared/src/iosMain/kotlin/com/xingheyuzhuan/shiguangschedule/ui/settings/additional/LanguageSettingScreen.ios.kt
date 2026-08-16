package com.xingheyuzhuan.shiguangschedule.ui.settings.additional

import platform.Foundation.NSUserDefaults

actual object PlatformLocaleManager {

    private const val APPLE_LANGUAGES_KEY = "AppleLanguages"

    actual fun setLanguageTag(tag: String) {
        if (tag.isEmpty()) {
            NSUserDefaults.standardUserDefaults.removeObjectForKey(APPLE_LANGUAGES_KEY)
        } else {
            NSUserDefaults.standardUserDefaults.setObject(
                value = listOf(tag),
                forKey = APPLE_LANGUAGES_KEY
            )
        }
        NSUserDefaults.standardUserDefaults.synchronize()
    }

    actual fun getCurrentLanguageTag(): String {
        val languages = NSUserDefaults.standardUserDefaults.arrayForKey(APPLE_LANGUAGES_KEY)
        return (languages?.firstOrNull() as? String) ?: ""
    }
}