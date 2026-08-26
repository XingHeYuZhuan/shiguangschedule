package com.xingheyuzhuan.shiguangschedule.tool

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual object PlatformUpdateStrategy {
    actual val isUpdateSupported: Boolean = true

    actual fun parseTargetUrl(response: ApiReleaseResponse): String? =
        response.assets.firstOrNull { asset ->
            val name = asset.name.lowercase()
            name.endsWith(".ipa") || name.endsWith(".zip")
        }?.downloadUrl

    actual fun openUrl(url: String) {
        NSURL.URLWithString(url)?.let { UIApplication.sharedApplication.openURL(it) }
    }
}
