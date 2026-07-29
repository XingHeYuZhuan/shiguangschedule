package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web

import android.graphics.Bitmap
import android.webkit.*

class WebCompatDelegate(private val webView: WebView) {

    /**
     * 增强 WebView 基础配置
     */
    fun enhanceSettings(isDesktopMode: Boolean): WebCompatDelegate {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowUniversalAccessFromFileURLs = true
            allowFileAccessFromFileURLs = true
            allowFileAccess = true
            allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

            if (isDesktopMode) {
                useWideViewPort = true
                loadWithOverviewMode = true // 设为 true 以确保初始加载时能自动缩放预览网页全貌
                layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
            } else {
                useWideViewPort = false
                loadWithOverviewMode = true
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

    /**
     * 包装 WebViewClient，统一控制 JS 注入流程
     */
    fun wrapWebViewClient(original: WebViewClient, isDesktopMode: Boolean): WebViewClient {
        val interceptor = WebViewRequestInterceptor()
        return object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                if (request != null) {
                    val interceptedResponse = interceptor.intercept(request, isDesktopMode)
                    if (interceptedResponse != null) {
                        return interceptedResponse
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                original.onPageStarted(view, url, favicon)
                // Also inject onPageStarted to catch early requests
                view?.evaluateJavascript(JS_INTERCEPT_POST, null)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                original.onPageFinished(view, url)

                view?.let { wv ->
                    // Inject POST interception JS as early as possible on every page
                    wv.evaluateJavascript(JS_INTERCEPT_POST, null)

                    if (isDesktopMode) {
                        injectDesktopCompatLayer(wv)
                    }
                    wv.injectAllJavaScript()
                }
            }

            override fun onReceivedSslError(v: WebView, h: SslErrorHandler, e: android.net.http.SslError) =
                original.onReceivedSslError(v, h, e)

            override fun onReceivedError(v: WebView, q: WebResourceRequest, e: WebResourceError) =
                original.onReceivedError(v, q, e)
        }
    }

    /**
     * 注入桌面模式特定的 CSS 和 Viewport 修复
     */
    private fun injectDesktopCompatLayer(view: WebView) {
        val desktopWidth = 1920
        view.evaluateJavascript("""
            (function() {
                var metas = document.getElementsByTagName('meta');
                for (var i = metas.length - 1; i >= 0; i--) {
                    if (metas[i].getAttribute('name') === 'viewport') metas[i].parentNode.removeChild(metas[i]);
                }
                var meta = document.createElement('meta');
                meta.name = "viewport";
                meta.content = "width=$desktopWidth, initial-scale=1.0, minimum-scale=0.1, maximum-scale=5.0, user-scalable=yes";
                document.head.appendChild(meta);
                var css = 'html, body { ' +
                          'width: ${desktopWidth}px !important; ' + 
                          'min-width: ${desktopWidth}px !important; ' + 
                          'overflow-x: auto !important; ' + 
                          'position: relative !important; ' + 
                          'display: block !important; ' +
                          '-webkit-overflow-scrolling: touch !important;' +
                          '}';
                var style = document.createElement('style');
                style.type = 'text/css';
                style.appendChild(document.createTextNode(css));
                document.head.appendChild(style);
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