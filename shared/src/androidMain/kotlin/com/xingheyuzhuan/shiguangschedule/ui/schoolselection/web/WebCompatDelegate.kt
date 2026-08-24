package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * WebView 代理配置与客户端包装类
 */
class WebCompatDelegate(private val webView: WebView) {

    private val defaultUserAgent: String = webView.settings.userAgentString
    private val requestInterceptor = WebViewRequestInterceptor()

    @SuppressLint("SetJavaScriptEnabled")
    fun enhanceSettings(isDesktopMode: Boolean): WebCompatDelegate {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = true
            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs = true
            allowFileAccess = true
            allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            useWideViewPort = true
            loadWithOverviewMode = true

            if (isDesktopMode) {
                userAgentString = DESKTOP_USER_AGENT
                layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
            } else {
                userAgentString = defaultUserAgent
                layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
            }

            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
        }

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)
        return this
    }

    fun wrapWebViewClient(original: WebViewClient, isDesktopModeProvider: () -> Boolean): WebViewClient {
        return object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return original.shouldOverrideUrlLoading(view, request)
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                if (request != null) {
                    val currentDesktopMode = isDesktopModeProvider()
                    val interceptedResponse = requestInterceptor.intercept(request, currentDesktopMode)
                    if (interceptedResponse != null) {
                        return interceptedResponse
                    }
                }
                return original.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                original.onPageStarted(view, url, favicon)
                view?.let { wv ->
                    // POST 接管只服务于桌面模式。移动模式下原生 WebView 网络栈不会
                    // 消费内部 X-WebView-Post-Id，请求携带它反而会触发跨源预检。
                    if (isDesktopModeProvider()) {
                        wv.evaluateJavascript(JS_INTERCEPT_POST, null)
                    }
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                original.onPageFinished(view, url)

                view?.let { wv ->
                    if (isDesktopModeProvider()) {
                        injectDesktopViewportFix(wv)
                    }
                    wv.injectAllJavaScript(isDesktopModeProvider())
                }
            }

            override fun onReceivedSslError(v: WebView?, h: SslErrorHandler?, e: SslError?) {
                original.onReceivedSslError(v, h, e)
            }

            override fun onReceivedError(v: WebView, q: WebResourceRequest, e: WebResourceError) =
                original.onReceivedError(v, q, e)
        }
    }

    /**
     * 仅在桌面模式下补全 Viewport Meta 标签与触发 resize，避免 PC 网页排版挤压
     */
    private fun injectDesktopViewportFix(view: WebView) {
        val desktopWidth = 1280
        view.evaluateJavascript("""
            (function() {
                try {
                    var metas = document.getElementsByTagName('meta');
                    for (var i = metas.length - 1; i >= 0; i--) {
                        if (metas[i].getAttribute('name') === 'viewport') {
                            metas[i].parentNode.removeChild(metas[i]);
                        }
                    }
                    var meta = document.createElement('meta');
                    meta.name = "viewport";
                    meta.content = "width=$desktopWidth, initial-scale=1.0, minimum-scale=0.1, maximum-scale=5.0, user-scalable=yes";
                    document.head.appendChild(meta);

                    // 触发 resize 事件促使根据 window 宽高度重绘的 JS 组件重新计算高度
                    window.dispatchEvent(new Event('resize'));
                } catch(e) {
                    console.error("injectDesktopViewportFix Error: ", e);
                }
            })();
        """.trimIndent(), null)
    }

    fun wrapWebChromeClient(original: WebChromeClient, onProgress: (Int) -> Unit): WebChromeClient {
        return object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                onProgress(newProgress)
                original.onProgressChanged(view, newProgress)
            }
            override fun onReceivedTitle(v: WebView?, t: String?) = original.onReceivedTitle(v, t)
        }
    }
}

/** 注入 Bridge；仅在桌面模式下启用由原生层配套处理的 POST 拦截。 */
internal fun WebView.injectAllJavaScript(enablePostInterception: Boolean) {
    evaluateJavascript(JS_BRIDGE_INIT, null)
    if (enablePostInterception) {
        evaluateJavascript(JS_INTERCEPT_POST, null)
    }
}
