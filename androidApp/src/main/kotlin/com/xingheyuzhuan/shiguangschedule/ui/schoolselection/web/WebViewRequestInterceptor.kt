package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web

import android.util.Log
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class WebViewRequestInterceptor {
    companion object {
        private val clientWithRedirects = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(30, TimeUnit.SECONDS) // 增加超时时间以提高稳定性
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        private val clientNoRedirects = clientWithRedirects.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()

        // Registry for POST bodies sent from JS
        private val postBodyRegistry = java.util.Collections.synchronizedMap(mutableMapOf<String, RegisteredPostData>())

        fun registerPostData(id: String, body: String, contentType: String) {
            postBodyRegistry[id] = RegisteredPostData(body, contentType)
        }

        private data class RegisteredPostData(val body: String, val contentType: String)
    }

    private val cookieManager = CookieManager.getInstance()

    /**
     * @param isDesktopMode 仅在电脑模式开启时执行拦截
     */
    fun intercept(request: WebResourceRequest, isDesktopMode: Boolean): WebResourceResponse? {
        val rawUrl = request.url.toString()
        
        // 跳过非 http(s) 链接
        if (!rawUrl.startsWith("http")) return null
        
        // 关键改进：仅在电脑模式下处理，手机模式直接返回 null 让 WebView 原生处理
        if (!isDesktopMode) return null

        // 1. 检查注册的 POST Body ID
        val requestIdHeader = request.requestHeaders["X-WebView-Post-Id"]
        val requestIdParam = request.url.getQueryParameter("_webview_post_id")
        val requestId = requestIdHeader ?: requestIdParam
        
        // --- 改进：URL 去污染逻辑 ---
        // 在转发前从 URL 中剔除我们的内部参数，确保服务器收到纯净的地址
        val url = if (requestIdParam != null) {
            val uriBuilder = request.url.buildUpon().clearQuery()
            request.url.queryParameterNames.forEach { name ->
                if (name != "_webview_post_id") {
                    request.url.getQueryParameters(name).forEach { value ->
                        uriBuilder.appendQueryParameter(name, value)
                    }
                }
            }
            uriBuilder.build().toString()
        } else {
            rawUrl
        }

        val registeredData = requestId?.let { postBodyRegistry.remove(it) }

        // 如果是 POST 但没有注册 Body，我们无法处理
        if (request.method.uppercase() != "GET" && registeredData == null) {
            return null
        }

        // 主框架使用 clientNoRedirects，子资源使用 clientWithRedirects
        val client = if (request.isForMainFrame) clientNoRedirects else clientWithRedirects

        try {
            val builder = Request.Builder().url(url)
            
            // 2. 设置请求方法和 Body
            if (registeredData != null) {
                val mediaType = registeredData.contentType.ifBlank { "application/x-www-form-urlencoded" }.toMediaTypeOrNull()
                val body = registeredData.body.toRequestBody(mediaType)
                builder.method(request.method, body)
            } else {
                builder.method(request.method, null)
            }

            // 3. 复制头部，剔除 X-Requested-With 和内部 ID 头部
            request.requestHeaders.forEach { (key, value) ->
                if (!key.equals("X-Requested-With", ignoreCase = true) && 
                    !key.equals("X-WebView-Post-Id", ignoreCase = true)) {
                    builder.addHeader(key, value)
                }
            }

            // 4. 同步 WebView 的 Cookie 到 OkHttp
            val cookies = cookieManager.getCookie(url)
            if (!cookies.isNullOrEmpty()) {
                builder.addHeader("Cookie", cookies)
            }

            val response = client.newCall(builder.build()).execute()

            // 5. 同步 OkHttp 的 Cookie 回 WebView
            val responseHeaders = response.headers
            responseHeaders.values("Set-Cookie").forEach {
                cookieManager.setCookie(url, it)
            }
            cookieManager.flush()

            // 6. 处理重定向 (WebResourceResponse 不支持 3xx)
            if (response.code in 300..399) {
                if (request.isForMainFrame) {
                    val location = response.header("Location")
                    if (location != null) {
                        val absoluteLocation = response.request.url.resolve(location)?.toString() ?: location
                        val html = "<html><script>window.location.replace('$absoluteLocation');</script></html>"
                        response.close()
                        return WebResourceResponse(
                            "text/html",
                            "UTF-8",
                            200,
                            "OK",
                            mapOf("Cache-Control" to "no-cache"),
                            html.byteInputStream()
                        )
                    }
                }
                response.body.close()
                response.close()
                return null
            }

            // 7. 构造 WebResourceResponse
            val contentType = response.header("Content-Type")
            val mimeType = contentType?.substringBefore(";") ?: "text/html"
            val encoding = contentType?.substringAfter("charset=", "UTF-8") ?: "UTF-8"

            val responseHeadersMap = mutableMapOf<String, String>()
            for (i in 0 until responseHeaders.size) {
                val name = responseHeaders.name(i)
                if (!name.equals("Content-Encoding", ignoreCase = true)) {
                    responseHeadersMap[name] = responseHeaders.value(i)
                }
            }

            // 重要：不要在这里立即调用 response.close()，让 WebView 消耗完数据流
            return WebResourceResponse(
                mimeType,
                encoding,
                response.code,
                response.message.ifBlank { "OK" },
                responseHeadersMap,
                response.body.byteStream()
            )
        } catch (e: Exception) {
            Log.e("WebViewInterceptor", "Error intercepting request: $url", e)
            return null
        }
    }
}

/**
 * 专门用于网络拦截的内部桥接，不属于业务 API。
 */
class WebPostBridge {
    @JavascriptInterface
    fun register(id: String, body: String, type: String) {
        WebViewRequestInterceptor.registerPostData(id, body, type)
    }
}