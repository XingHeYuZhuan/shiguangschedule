// DebugOkHttpExt.kt (位于 app/src/release/java/...)

package com.xingheyuzhuan.shiguangschedule.data

import okhttp3.OkHttpClient
/**
 * Release Source Set 的实现：空操作 (No-Op)。
 */
fun OkHttpClient.Builder.addDebugInterceptor(): OkHttpClient.Builder {
    return this // 直接返回构建器，什么都不做
}