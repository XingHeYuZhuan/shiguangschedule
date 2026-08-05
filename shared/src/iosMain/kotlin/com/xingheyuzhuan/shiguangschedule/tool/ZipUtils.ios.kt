// shared/src/iosMain/kotlin/com/xingheyuzhuan/shiguangschedule/tool/ZipUtils.ios.kt
package com.xingheyuzhuan.shiguangschedule.tool

/**
 * iOS 端的 Zip 工具类实现
 * 当前为占位符，尚未实现具体逻辑。
 * 如果在 iOS 运行此代码将会触发 NotImplementedError。
 */
actual object ZipUtils {
    actual fun createZip(entries: Map<String, ByteArray>): ByteArray {
        throw NotImplementedError("ZipUtils.createZip is not yet implemented for iOS platform.")
    }
}