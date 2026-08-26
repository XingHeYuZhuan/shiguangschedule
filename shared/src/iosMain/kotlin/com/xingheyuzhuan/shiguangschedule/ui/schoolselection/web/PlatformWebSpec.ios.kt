package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlin.native.Platform
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ObjCSignatureOverride
import platform.Foundation.HTTPMethod
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSLock
import platform.Foundation.NSLog
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSDictionary
import platform.Foundation.NSString
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURLResponse
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionDataTask
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.allHTTPHeaderFields
import platform.Foundation.allKeys
import platform.Foundation.base64EncodedStringWithOptions
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.objectForKeyedSubscript
import platform.Foundation.NSNull
import platform.Foundation.NSHTTPCookie
import platform.Foundation.setHTTPBody
import platform.Foundation.setValue
import platform.Foundation.valueForKey
import platform.Foundation.valueForHTTPHeaderField
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKURLSchemeHandlerProtocol
import platform.WebKit.WKURLSchemeTaskProtocol
import platform.WebKit.WKUserScript
import platform.WebKit.WKUserContentController
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUserScriptInjectionTime.WKUserScriptInjectionTimeAtDocumentStart
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

actual val isDesktopPlatform: Boolean = false

private const val ABOUT_BLANK = "about:blank"
private const val PROXY_SCHEME = "kmp-resource"
private const val DIAGNOSTIC_PREFIX = "SHIGUANG_DIAG|"
private const val FETCH_PREFIX = "SHIGUANG_FETCH|"
@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
private val isIosWebDiagnosticsLogEnabled = Platform.isDebugBinary

private object IosWebDiagnostics {
    private const val MAX_LOGS = 300
    private val lock = NSLock()
    private val entries = mutableListOf<String>()

    fun add(message: String) {
        lock.lock()
        try {
            val entry = "${entries.size + 1}. $message"
            entries += entry
            if (isIosWebDiagnosticsLogEnabled) {
                NSLog("ShiguangWebDiagnostics: ${entry.replace("\n", " ").replace("\r", " ")}")
            }
            if (entries.size > MAX_LOGS) entries.removeAt(0)
        } finally {
            lock.unlock()
        }
    }

    fun pageHtml(): String {
        val logs = snapshot().joinToString("\n") { "<pre>${it.escapeHtml()}</pre>" }
        return """
            <!doctype html>
            <html>
            <head>
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <style>
                body { margin: 0; font-family: -apple-system, BlinkMacSystemFont, sans-serif; background: #f7f7f8; color: #1f2328; }
                header { position: sticky; top: 0; padding: 14px 16px; background: #ffffff; border-bottom: 1px solid #d0d7de; }
                h1 { margin: 0 0 4px; font-size: 18px; }
                p { margin: 0; color: #57606a; font-size: 13px; }
                main { padding: 12px; }
                pre { margin: 0 0 8px; padding: 10px; white-space: pre-wrap; overflow-wrap: anywhere; background: #ffffff; border: 1px solid #d0d7de; border-radius: 8px; font-size: 12px; line-height: 1.45; }
                .empty { padding: 16px; background: #ffffff; border: 1px solid #d0d7de; border-radius: 8px; color: #57606a; }
              </style>
            </head>
            <body>
              <header>
                <h1>Web Request Diagnostics</h1>
                <p>Open this page after reproducing the failure in desktop mode. Use Back to return.</p>
              </header>
              <main>${if (logs.isBlank()) "<div class=\"empty\">No logs captured yet.</div>" else logs}</main>
            </body>
            </html>
        """.trimIndent()
    }

    private fun snapshot(): List<String> {
        lock.lock()
        return try {
            entries.toList()
        } finally {
            lock.unlock()
        }
    }
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS navigation is handled by the shared screen's toolbar and swipe navigation.
}

private class IosNavigationDelegate(
    private val onProgress: (Float) -> Unit,
    private val onTitle: (String) -> Unit,
    private val injectBridge: () -> Unit
) : NSObject(), WKNavigationDelegateProtocol {
    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didStartProvisionalNavigation: WKNavigation?) {
        IosWebDiagnostics.add("navigation start url=${webView.URL?.absoluteString ?: "pending"}")
        onProgress(0.1f)
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
        onProgress(1f)
        webView.title?.takeIf { it.isNotBlank() }?.let(onTitle)
        IosWebDiagnostics.add("navigation finish url=${webView.URL?.absoluteString ?: ""} title=${webView.title ?: ""}")
        injectBridge()
    }
}

private class IosScriptMessageHandler(
    private val bridgeHandler: WebBridgeHandler,
    private val onFetch: (NSDictionary) -> Unit
) : NSObject(), WKScriptMessageHandlerProtocol {
    override fun userContentController(userContentController: WKUserContentController, didReceiveScriptMessage: WKScriptMessage) {
        val body = didReceiveScriptMessage.body
        if (body is NSDictionary && body.objectForKey("type") == FETCH_PREFIX) {
            onFetch(body)
            return
        }

        val message = body.toString()
        when {
            message.startsWith(DIAGNOSTIC_PREFIX) -> IosWebDiagnostics.add("js ${message.removePrefix(DIAGNOSTIC_PREFIX)}")
            message.isNotBlank() -> bridgeHandler.onMessageReceived(message)
        }
    }
}

private class IosFetchProxy(
    private val webView: () -> WKWebView?
) {
    fun handle(message: NSDictionary) {
        val requestId = message.objectForKey("id")?.toString() ?: return
        val urlString = message.objectForKey("url")?.toString() ?: return
        IosWebDiagnostics.add("fetch bridge received id=$requestId url=$urlString")
        val url = NSURL.URLWithString(urlString) ?: run {
            finish(requestId, error = "Invalid URL")
            return
        }
        val method = message.objectForKey("method")?.toString() ?: "GET"
        val suppliedHeaders = message.objectForKey("headers") as? NSDictionary
        val body = message.objectForKey("body")
        val cookieStore = webView()?.configuration?.websiteDataStore?.httpCookieStore
        cookieStore?.getAllCookies { cookies ->
            val cookieHeader = cookies.orEmpty()
                .filterIsInstance<NSHTTPCookie>()
                .filter { cookie -> url.host?.lowercase()?.endsWith(cookie.domain.trimStart('.').lowercase()) == true }
                .joinToString("; ") { "${it.name}=${it.value}" }
            val cookieNames = cookies.orEmpty()
                .filterIsInstance<NSHTTPCookie>()
                .filter { cookie -> url.host?.lowercase()?.endsWith(cookie.domain.trimStart('.').lowercase()) == true }
                .joinToString(",") { it.name }
            val request = NSMutableURLRequest.requestWithURL(url).apply {
                HTTPMethod = method
                suppliedHeaders?.allKeys()?.forEach { rawKey ->
                    val key = rawKey?.toString() ?: return@forEach
                    val value = suppliedHeaders.objectForKeyedSubscript(rawKey)?.toString() ?: return@forEach
                    if (key.equals("Cookie", ignoreCase = true) && value.isBlank()) return@forEach
                    if (key.canForwardNativeFetchHeader()) setValue(value, forHTTPHeaderField = key)
                }
                if (cookieHeader.isNotBlank() && valueForHTTPHeaderField("Cookie") == null) {
                    setValue(cookieHeader, forHTTPHeaderField = "Cookie")
                }
                if (valueForHTTPHeaderField("User-Agent") == null) {
                    setValue(DESKTOP_USER_AGENT, forHTTPHeaderField = "User-Agent")
                }
                if (valueForHTTPHeaderField("Accept") == null) {
                    setValue("application/json, text/plain, */*", forHTTPHeaderField = "Accept")
                }
                if (valueForHTTPHeaderField("X-Requested-With") == null) {
                    setValue("XMLHttpRequest", forHTTPHeaderField = "X-Requested-With")
                }
                if (HTTPMethod.uppercase() !in setOf("GET", "HEAD") && body !is NSNull && body != null) {
                    NSString.create(body.toString()).dataUsingEncoding(NSUTF8StringEncoding)?.let(::setHTTPBody)
                }
            }

            IosWebDiagnostics.add("native fetch request id=$requestId method=${request.HTTPMethod} url=$urlString cookie=${if (cookieHeader.isBlank()) "none" else "present names=$cookieNames"} headers=${request.diagnosticHeaders()}")
            NSURLSession.sharedSession.dataTaskWithRequest(request as NSURLRequest) { data, response, error ->
            when {
                error != null -> {
                    IosWebDiagnostics.add("native fetch error id=$requestId url=$urlString error=${error.localizedDescription}")
                    finish(requestId, error = error.localizedDescription)
                }
                response is NSHTTPURLResponse -> {
                    val headers = response.allHeaderFields.entries.joinToString(",") { entry ->
                        "\"${entry.key.toString().jsonEscape()}\":\"${entry.value.toString().jsonEscape()}\""
                    }
                    val bodyBase64 = data?.base64EncodedStringWithOptions(0u) ?: ""
                    val bodyPreview = data?.let { NSString.create(it, NSUTF8StringEncoding)?.toString() }
                        ?.replace("\n", " ")
                        ?.replace("\r", " ")
                        ?.take(1200)
                        ?: "<binary or empty>"
                    val responseCookies = NSHTTPCookie.cookiesWithResponseHeaderFields(response.allHeaderFields, url)
                        .filterIsInstance<NSHTTPCookie>()
                    responseCookies.forEach { cookie -> cookieStore.setCookie(cookie, null) }
                    val setCookieNames = responseCookies.joinToString(",") { it.name }
                    IosWebDiagnostics.add("native fetch response id=$requestId status=${response.statusCode} bytes=${data?.length ?: 0} contentType=${response.allHeaderFields["Content-Type"] ?: ""} setCookie=$setCookieNames headers=${response.diagnosticHeaders()} body=$bodyPreview url=$urlString")
                    finish(requestId, response.statusCode.toInt(), "{$headers}", bodyBase64)
                }
                else -> finish(requestId, error = "Invalid response")
            }
            }.resume()
        } ?: run {
            finish(requestId, error = "WK cookie store unavailable")
        }
    }

    private fun finish(
        requestId: String,
        status: Int? = null,
        headersJson: String = "{}",
        bodyBase64: String = "",
        error: String? = null
    ) {
        val statusValue = status?.toString() ?: "null"
        val errorValue = error?.let { "'${it.jsEscape()}'" } ?: "null"
        val script = "window.__shiguangFetchDone('${requestId.jsEscape()}', $statusValue, $headersJson, '$bodyBase64', $errorValue);"
        dispatchToMain {
            webView()?.evaluateJavaScript(script, null)
        }
    }
}

private fun dispatchToMain(block: () -> Unit) {
    dispatch_async(dispatch_get_main_queue(), block)
}

private fun WKWebView.clearCookiesForUrl(url: String, onComplete: () -> Unit) {
    val targetHost = NSURL.URLWithString(url)?.host?.lowercase()
    if (targetHost == null) {
        onComplete()
        return
    }
    configuration.websiteDataStore.httpCookieStore.getAllCookies { cookies ->
        val matchingCookies = cookies.orEmpty()
            .filterIsInstance<NSHTTPCookie>()
            .filter { cookie -> targetHost.endsWith(cookie.domain.trimStart('.').lowercase()) }

        if (matchingCookies.isEmpty()) {
            IosWebDiagnostics.add("clear cookies url=$url names=none")
            dispatchToMain(onComplete)
            return@getAllCookies
        }

        IosWebDiagnostics.add("clear cookies url=$url names=${matchingCookies.joinToString(",") { it.name }}")
        var remaining = matchingCookies.size
        matchingCookies.forEach { cookie ->
            configuration.websiteDataStore.httpCookieStore.deleteCookie(cookie) {
                remaining -= 1
                if (remaining == 0) dispatchToMain(onComplete)
            }
        }
    }
}

private class IosUrlSchemeHandler : NSObject(), WKURLSchemeHandlerProtocol {
    private val tasks = mutableMapOf<Int, NSURLSessionDataTask>()

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, startURLSchemeTask: WKURLSchemeTaskProtocol) {
        val sourceRequest = startURLSchemeTask.request
        val targetUrl = sourceRequest.URL?.toHttpsUrl() ?: run {
            IosWebDiagnostics.add("scheme request invalid url=${sourceRequest.URL?.absoluteString ?: ""}")
            startURLSchemeTask.didFailWithError(NSError.errorWithDomain("ShiguangWebProxy", 1, null))
            return
        }
        val method = sourceRequest.HTTPMethod ?: "GET"
        IosWebDiagnostics.add("scheme request method=$method source=${sourceRequest.URL?.absoluteString ?: ""}\ntarget=${targetUrl.absoluteString}\nheaders=${sourceRequest.diagnosticHeaders()}")

        if (method.equals("OPTIONS", ignoreCase = true)) {
            startURLSchemeTask.didReceiveResponse(
                NSHTTPURLResponse(
                    uRL = targetUrl,
                    statusCode = 204,
                    HTTPVersion = "HTTP/1.1",
                    headerFields = proxyCorsHeaders(sourceRequest, null) as Map<Any?, *>
                )!!
            )
            startURLSchemeTask.didFinish()
            IosWebDiagnostics.add("scheme preflight response status=204 target=${targetUrl.absoluteString}")
            return
        }

        val nativeRequest = NSMutableURLRequest.requestWithURL(targetUrl).apply {
            HTTPMethod = method
            sourceRequest.allHTTPHeaderFields?.forEach { entry ->
                val header = entry.key as? String
                val headerValue = entry.value as? String
                if (header != null && headerValue != null && header.canForwardHeader()) {
                    setValue(headerValue, forHTTPHeaderField = header)
                }
            }
            (sourceRequest.valueForKey("HTTPBody") as? NSData)?.let { setHTTPBody(it) }
        }

        val taskKey = startURLSchemeTask.hash.toInt()
        val dataTask = NSURLSession.sharedSession.dataTaskWithRequest(
            nativeRequest as NSURLRequest,
            { data: NSData?, response: NSURLResponse?, error: NSError? ->
                tasks.remove(taskKey)
                when {
                    error != null -> {
                        IosWebDiagnostics.add("native request error target=${targetUrl.absoluteString} error=${error.localizedDescription}")
                        startURLSchemeTask.didFailWithError(error)
                    }
                    response is NSHTTPURLResponse -> {
                        IosWebDiagnostics.add("native response status=${response.statusCode} target=${targetUrl.absoluteString}\nheaders=${response.diagnosticHeaders()}")
                        startURLSchemeTask.didReceiveResponse(response.withProxyHeaders(sourceRequest))
                        data?.let(startURLSchemeTask::didReceiveData)
                        startURLSchemeTask.didFinish()
                    }
                    response != null -> {
                        IosWebDiagnostics.add("native response non-http target=${targetUrl.absoluteString}")
                        startURLSchemeTask.didReceiveResponse(response)
                        data?.let(startURLSchemeTask::didReceiveData)
                        startURLSchemeTask.didFinish()
                    }
                    else -> {
                        IosWebDiagnostics.add("native request empty response target=${targetUrl.absoluteString}")
                        startURLSchemeTask.didFailWithError(NSError.errorWithDomain("ShiguangWebProxy", 2, null))
                    }
                }
            }
        )
        tasks[taskKey] = dataTask
        dataTask.resume()
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, stopURLSchemeTask: WKURLSchemeTaskProtocol) {
        IosWebDiagnostics.add("scheme request cancelled url=${stopURLSchemeTask.request.URL?.absoluteString ?: ""}")
        tasks.remove(stopURLSchemeTask.hash.toInt())?.cancel()
    }
}

private class IosWebViewController : WebViewController {
    var webView: WKWebView? = null
    var scriptMessageHandler: IosScriptMessageHandler? = null
    var urlSchemeHandler: IosUrlSchemeHandler? = null
    override val currentUrl: String get() = webView?.URL?.absoluteString ?: ""
    override fun reload() { webView?.reload() }
    override fun goBack(): Boolean = webView?.goBack() != null
    override fun canGoBack(): Boolean = webView?.canGoBack == true
    override fun setDevToolsEnabled(enabled: Boolean) = Unit
    override fun executeScript(jsCode: String) { evaluateJavascript(jsCode) }
    override fun evaluateJavascript(script: String, callback: ((String?) -> Unit)?) {
        val finalScript = if (script.contains("_shiguangBridgeInjected")) {
            script
        } else {
            "$JS_BRIDGE_INIT\n$script"
        }
        webView?.evaluateJavaScript(finalScript) { result, _ -> callback?.invoke(result?.toString()) }
    }
}

@Composable
actual fun rememberWebViewController(): WebViewController = remember { IosWebViewController() }

@OptIn(BetaInteropApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)
@Composable
actual fun PlatformWebView(
    modifier: Modifier,
    url: String,
    isDesktopMode: Boolean,
    isDevToolsEnabled: Boolean,
    controller: WebViewController,
    bridgeHandler: WebBridgeHandler,
    onProgressChange: (Float) -> Unit,
    onTitleChange: (String) -> Unit,
    onNavigateToSchedule: () -> Unit
) {
    val iosController = controller as IosWebViewController
    key(isDesktopMode) {
        UIKitView(
            modifier = modifier,
            factory = {
                val configuration = WKWebViewConfiguration().apply {
                    if (isDesktopMode) {
                        IosWebDiagnostics.add("desktop mode proxy enabled url=$url")
                        val schemeHandler = IosUrlSchemeHandler()
                        iosController.urlSchemeHandler = schemeHandler
                        setURLSchemeHandler(schemeHandler, forURLScheme = PROXY_SCHEME)
                        userContentController.addUserScript(WKUserScript(proxyRewriteScript, WKUserScriptInjectionTimeAtDocumentStart, false))
                    }
                }

                WKWebView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), configuration = configuration).also { view ->
                    iosController.webView = view
                    applyUserAgent(view, isDesktopMode)
                    view.navigationDelegate = IosNavigationDelegate(
                        onProgress = onProgressChange,
                        onTitle = onTitleChange,
                        injectBridge = {
                            view.evaluateJavaScript(JS_BRIDGE_INIT, null)
                            if (isDesktopMode) {
                                injectDesktopViewportFix(view)
                            }
                        }
                    )
                    val fetchProxy = IosFetchProxy { iosController.webView }
                    val messageHandler = IosScriptMessageHandler(bridgeHandler, fetchProxy::handle)
                    iosController.scriptMessageHandler = messageHandler
                    view.configuration.userContentController.addScriptMessageHandler(messageHandler, "shiguangBridge")
                    view.configuration.userContentController.addScriptMessageHandler(messageHandler, "_shiguangNativeBridge")
                    if (url.shouldLoad()) {
                        IosWebDiagnostics.add("load url=$url desktopMode=$isDesktopMode")
                        if (isDesktopMode) {
                            view.clearCookiesForUrl(url) { view.loadUrl(url) }
                        } else {
                            view.loadUrl(url)
                        }
                    }
                }
            },
            update = { view ->
                iosController.webView = view
                if (url.shouldLoad() && view.URL?.absoluteString != url) {
                    IosWebDiagnostics.add("update load url=$url desktopMode=$isDesktopMode current=${view.URL?.absoluteString ?: ""}")
                    view.loadUrl(url)
                }
            }
        )
    }
}

private fun applyUserAgent(webView: WKWebView, isDesktopMode: Boolean) {
    webView.customUserAgent = if (isDesktopMode) DESKTOP_USER_AGENT else null
}

private fun String.shouldLoad(): Boolean = isNotBlank() && this != ABOUT_BLANK

private fun WKWebView.loadUrl(url: String) {
    loadRequest(NSURLRequest(NSURL.URLWithString(url)!!))
}

private fun NSURL.toHttpsUrl(): NSURL? {
    if (scheme != PROXY_SCHEME) return this
    val components = NSURLComponents.componentsWithURL(this, resolvingAgainstBaseURL = false) ?: return null
    components.scheme = "https"
    return components.URL
}

private fun String.canForwardHeader(): Boolean {
    val lower = lowercase()
    return lower != "host" &&
        lower != "connection" &&
        lower != "content-length" &&
        lower != "origin" &&
        lower != "referer"
}

private fun String.canForwardNativeFetchHeader(): Boolean {
    val lower = lowercase()
    return lower != "host" && lower != "connection" && lower != "content-length"
}

private fun NSHTTPURLResponse.withProxyHeaders(sourceRequest: NSURLRequest): NSURLResponse {
    val headers = mutableMapOf<String, String>()
    allHeaderFields.forEach { entry ->
        val header = entry.key as? String
        val headerValue = entry.value?.toString()
        if (header != null && headerValue != null) headers[header] = headerValue
    }
    headers.putAll(proxyCorsHeaders(sourceRequest, headers["Access-Control-Allow-Headers"]))
    return NSHTTPURLResponse(
        uRL = URL ?: return this,
        statusCode = statusCode.toLong(),
        HTTPVersion = "HTTP/1.1",
        headerFields = headers as Map<Any?, *>
    ) ?: this
}

private fun NSURLRequest.diagnosticHeaders(): String {
    val headers = allHTTPHeaderFields ?: return "{}"
    return headers.entries.joinToString(prefix = "{", postfix = "}") { entry ->
        val key = entry.key?.toString().orEmpty()
        val value = entry.value?.toString().orEmpty()
        "$key=${key.redactHeaderValue(value)}"
    }
}

private fun NSHTTPURLResponse.diagnosticHeaders(): String {
    return allHeaderFields.entries.joinToString(prefix = "{", postfix = "}") { entry ->
        val key = entry.key?.toString().orEmpty()
        val value = entry.value?.toString().orEmpty()
        "$key=${key.redactHeaderValue(value)}"
    }
}

private fun String.redactHeaderValue(value: String): String {
    val lower = lowercase()
    return when {
        lower == "cookie" || lower == "set-cookie" -> "[redacted length=${value.length}]"
        lower == "authorization" -> "[redacted length=${value.length}]"
        else -> value
    }
}

private fun String.escapeHtml(): String = replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")

private fun String.jsonEscape(): String = replace("\\", "\\\\")
    .replace("\"", "\\\"")
    .replace("\n", "\\n")
    .replace("\r", "\\r")

private fun String.jsEscape(): String = jsonEscape()
    .replace("'", "\\'")

private fun proxyCorsHeaders(sourceRequest: NSURLRequest, existingAllowedHeaders: String?): Map<String, String> {
    val origin = sourceRequest.allHTTPHeaderFields?.get("Origin") as? String
    val requestedHeaders = sourceRequest.allHTTPHeaderFields?.get("Access-Control-Request-Headers") as? String
    return buildMap {
        put("Vary", "Origin")
        put("Cross-Origin-Resource-Policy", "cross-origin")
        put("Access-Control-Allow-Origin", origin ?: "*")
        put("Access-Control-Allow-Credentials", "true")
        put("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS")
        put("Access-Control-Allow-Headers", requestedHeaders ?: existingAllowedHeaders ?: "*")
    }
}

private val proxyRewriteScript = """
    (function() {
        if (window._shiguangProxyRewriteInjected) return;
        window._shiguangProxyRewriteInjected = true;

        function rewriteUrl(input) {
            try {
                var url = new URL(input, window.location.href);
                if (shouldUseNativeProxy(url)) {
                    return 'kmp-resource://' + url.host + url.pathname + url.search + url.hash;
                }
            } catch (e) {}
            return input;
        }

        function shouldUseNativeProxy(url) {
            return (url.protocol === 'http:' || url.protocol === 'https:') && url.origin !== window.location.origin;
        }

        function diag(message) {
            try {
                var bridge = window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers._shiguangNativeBridge;
                if (bridge) bridge.postMessage('SHIGUANG_DIAG|' + message);
            } catch (e) {}
        }

        var loginRedirectStarted = false;

        function redirectToHomeLogin() {
            if (loginRedirectStarted) return;
            try {
                var contextPath = window.contextPath || (window.WIS_CONFIG && window.WIS_CONFIG.ROOT_PATH) || '';
                if (contextPath) {
                    loginRedirectStarted = true;
                    var loginUrl = contextPath + '/sys/homeapp/index.do?contextPath=' + contextPath;
                    diag('web fetch redirect login url=' + loginUrl);
                    window.location.replace(loginUrl);
                }
            } catch (e) {
                diag('web fetch redirect login error=' + e);
            }
        }

        diag('proxy rewrite script injected href=' + window.location.href);

        var nativeFetchWaiters = {};
        var nativeFetchSequence = 0;

        window.__shiguangFetchDone = function(id, status, headers, bodyBase64, error) {
            var waiter = nativeFetchWaiters[id];
            if (!waiter) return;
            delete nativeFetchWaiters[id];
            if (error) {
                diag('native fetch callback error id=' + id + ' error=' + error);
                waiter.reject(new TypeError(error));
                return;
            }
            diag('native fetch callback id=' + id + ' status=' + status + ' bytes=' + (bodyBase64 || '').length);
            var binary = atob(bodyBase64 || '');
            var bytes = new Uint8Array(binary.length);
            for (var i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
            try {
                waiter.resolve(new Response(bytes, { status: status, headers: headers || {} }));
            } catch (e) {
                diag('native fetch Response construction error id=' + id + ' error=' + e);
                waiter.reject(e);
            }
        };

        function nativeFetch(input, init) {
            var request = input && typeof input === 'object' && input.url ? input : null;
            var url = request ? request.url : input.toString();
            var options = init || {};
            var method = options.method || (request && request.method) || 'GET';
            var rawHeaders = options.headers || (request && request.headers) || {};
            var headers = {};
            if (window.Headers && rawHeaders instanceof Headers) {
                rawHeaders.forEach(function(value, key) { headers[key] = value; });
            } else if (Array.isArray(rawHeaders)) {
                rawHeaders.forEach(function(pair) { if (pair && pair.length > 1) headers[pair[0]] = pair[1]; });
            } else {
                headers = rawHeaders;
            }
            if (!headers.Cookie && document.cookie) headers.Cookie = document.cookie;
            if (!headers.Origin) headers.Origin = window.location.origin;
            if (!headers.Referer) headers.Referer = window.location.href;
            if (!headers['User-Agent']) headers['User-Agent'] = navigator.userAgent || '';
            var body = options.body;
            var id = 'fetch_' + (++nativeFetchSequence) + '_' + Date.now();
            diag('native fetch ' + method + ' ' + url);
            return new Promise(function(resolve, reject) {
                nativeFetchWaiters[id] = { resolve: resolve, reject: reject };
                try {
                    var bridge = window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers._shiguangNativeBridge;
                    bridge.postMessage({ type: 'SHIGUANG_FETCH|', id: id, url: url, method: method, headers: headers, body: body || null });
                } catch (e) {
                    delete nativeFetchWaiters[id];
                    reject(e);
                }
            });
        }

        if (window.fetch) {
            var oldFetch = window.fetch;
            window.fetch = function(input, init) {
                try {
                    var requestUrl = input && typeof input === 'object' && input.url ? input.url : input.toString();
                    var url = new URL(requestUrl, window.location.href);
                    var absoluteUrl = url.toString();
                    var options = init || (input && typeof input === 'object' ? input : null);
                    if (shouldUseNativeProxy(url)) {
                        return nativeFetch(absoluteUrl, options);
                    }
                    var method = (init && init.method) || (input && typeof input === 'object' && input.method) || 'GET';
                    return oldFetch.apply(this, arguments).then(function(response) {
                        diag('web fetch response method=' + method + ' status=' + response.status + ' url=' + absoluteUrl);
                        return response;
                    }, function(error) {
                        diag('web fetch error method=' + method + ' url=' + absoluteUrl + ' error=' + error);
                        redirectToHomeLogin();
                        throw new TypeError('Failed to fetch');
                    });
                } catch (e) { diag('native fetch setup error ' + e); }
                return oldFetch.apply(this, arguments);
            };
        }

        var oldOpen = XMLHttpRequest.prototype.open;
        XMLHttpRequest.prototype.open = function(method, url) {
            try {
                var original = url;
                arguments[1] = rewriteUrl(url);
                if (arguments[1] !== original) diag('xhr rewrite method=' + method + ' ' + original + ' -> ' + arguments[1]);
            } catch (e) {}
            return oldOpen.apply(this, arguments);
        };
    })();
""".trimIndent()

private fun injectDesktopViewportFix(webView: WKWebView) {
    val desktopWidth = 1280
    webView.evaluateJavaScript(
        """
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
                    window.dispatchEvent(new Event('resize'));
                } catch(e) {
                    console.error("injectDesktopViewportFix Error: ", e);
                }
            })();
        """.trimIndent(),
        null
    )
}