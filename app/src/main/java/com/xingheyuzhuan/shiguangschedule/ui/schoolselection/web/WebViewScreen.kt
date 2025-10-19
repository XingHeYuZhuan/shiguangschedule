package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web

import android.annotation.SuppressLint
import android.app.Activity
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xingheyuzhuan.shiguangschedule.BuildConfig
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseConversionRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.TimeSlotRepository
import com.xingheyuzhuan.shiguangschedule.ui.components.CourseTablePickerDialog
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    navController: NavController,
    initialUrl: String?,
    assetJsPath: String?,
    courseConversionRepository: CourseConversionRepository,
    timeSlotRepository: TimeSlotRepository,
    courseScheduleRoute: String = "CourseSchedule"
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // --- 状态管理 ---
    var currentUrl by remember { mutableStateOf(initialUrl ?: "about:blank") }
    var inputUrl by remember { mutableStateOf(initialUrl ?: "https://") }
    var loadingProgress by remember { mutableFloatStateOf(0f) }
    var pageTitle by remember { mutableStateOf(if (currentUrl.isBlank() || currentUrl == "about:blank") "输入网址" else "加载中...") }
    var expanded by remember { mutableStateOf(false) }
    var isDesktopMode by remember { mutableStateOf(false) }
    var isEditingUrl by remember { mutableStateOf(false) }
    var isDevToolsEnabled by remember { mutableStateOf(false) }
    var showCourseTablePicker by remember { mutableStateOf(false) }

    // --- DevTools 逻辑 ---
    val enableDevToolsOptionInUi = BuildConfig.ENABLE_DEV_TOOLS_OPTION_IN_UI
    val enableAddressBarToggleButton = BuildConfig.ENABLE_ADDRESS_BAR_TOGGLE_BUTTON

    // --- 浏览器配置和 Agent ---
    val defaultUserAgent = remember { WebSettings.getDefaultUserAgent(context) }
    val desktopUserAgent = DESKTOP_USER_AGENT

    // --- Channel 和 Bridge 实例化 ---
    val uiEventChannel = remember { Channel<WebUiEvent>(Channel.BUFFERED) }
    var androidBridge: AndroidBridge? by remember { mutableStateOf(null) }


    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // WebView 配置
            @SuppressLint("SetJavaScriptEnabled")
            settings.javaScriptEnabled = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            settings.useWideViewPort = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.textZoom = 100
            settings.userAgentString = defaultUserAgent
            CookieManager.getInstance().setAcceptCookie(true)


            // 实例化 AndroidBridge
            androidBridge = AndroidBridge(
                context = context,
                coroutineScope = coroutineScope,
                webView = this,
                uiEventChannel = uiEventChannel,
                courseConversionRepository = courseConversionRepository,
                timeSlotRepository = timeSlotRepository,
                onTaskCompleted = {
                    Toast.makeText(context, "导入脚本执行完毕，返回课表页面。", Toast.LENGTH_LONG).show()
                    navController.popBackStack(
                        route = courseScheduleRoute,
                        inclusive = false
                    )
                }
            )
            addJavascriptInterface(androidBridge!!, "AndroidBridge")

            // WebViewClient: 页面导航和加载事件
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    return false
                }

                @SuppressLint("WebViewClientOnReceivedSslError")
                override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                    (context as? Activity)?.let { activity ->
                        MaterialAlertDialogBuilder(activity)
                            .setMessage("SSL证书验证失败，这可能意味着连接不安全。是否继续浏览？")
                            .setPositiveButton("继续浏览") { _, _ ->
                                handler.proceed()
                            }
                            .setNegativeButton("取消") { _, _ ->
                                handler.cancel()
                            }
                            .setCancelable(false)
                            .show()
                    } ?: run {
                        Toast.makeText(context, "SSL证书验证失败，已取消连接。", Toast.LENGTH_LONG).show()
                        handler.cancel()
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    view?.injectAllJavaScript(isDesktopMode)
                }

                override fun onReceivedError(view: WebView, request: android.webkit.WebResourceRequest, error: android.webkit.WebResourceError) {
                    if (request.isForMainFrame) {
                        val description = error.description.toString()
                        val context = view.context
                        view.post {
                            Toast.makeText(context, "网页加载错误: $description", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }

            // WebChromeClient: 进度条和标题
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    loadingProgress = newProgress / 100f
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    if (title != null) {
                        pageTitle = title
                    }
                }
            }

            loadUrl(initialUrl ?: "about:blank")
        }
    }

    // 状态改变时加载 URL
    LaunchedEffect(currentUrl) {
        if (currentUrl.isNotBlank() && currentUrl != "about:blank") {
            val urlToLoad = if (currentUrl.startsWith("http://") || currentUrl.startsWith("https://")) {
                currentUrl
            } else {
                "https://$currentUrl"
            }
            webView.loadUrl(urlToLoad)
        } else if (currentUrl == "about:blank") {
            webView.loadUrl("about:blank")
        }
    }

    // 资源清理
    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.clearCache(true)
            webView.clearFormData()
            webView.clearHistory()
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookies(null)
            cookieManager.flush()
            WebStorage.getInstance().deleteAllData()
            webView.removeAllViews()
            webView.destroy()
        }
    }

    val onSearch: (String) -> Unit = { query ->
        keyboardController?.hide()
        currentUrl = query
        isEditingUrl = false
        pageTitle = "加载中..."
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        if (isEditingUrl) {
                            isEditingUrl = false
                            inputUrl = webView.url ?: currentUrl
                            keyboardController?.hide()
                        } else {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isEditingUrl) "取消编辑" else "返回"
                        )
                    }
                },

                title = {
                    if (isEditingUrl) {
                        OutlinedTextField(
                            value = inputUrl,
                            onValueChange = { newQuery: String -> inputUrl = newQuery },
                            placeholder = { Text("输入网址 (https://...)") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(
                                onGo = { ->
                                    onSearch(inputUrl)
                                    isEditingUrl = false
                                }
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                errorBorderColor = Color.Transparent,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ),
                            textStyle = MaterialTheme.typography.bodyLarge,
                        )
                    } else {
                        Text(pageTitle, style = MaterialTheme.typography.titleLarge)
                    }
                },

                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isEditingUrl) {
                            IconButton(
                                onClick = {
                                    onSearch(inputUrl)
                                    isEditingUrl = false
                                },
                                enabled = inputUrl.isNotBlank() && inputUrl != "https://"
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "加载")
                            }
                        } else if (enableAddressBarToggleButton) {
                            IconButton(onClick = {
                                isEditingUrl = true
                                inputUrl = webView.url?.takeIf { it.isNotBlank() && it != "about:blank" } ?: "https://"
                                keyboardController?.show()
                            }) {
                                Icon(Icons.Default.Link, contentDescription = "输入网址")
                            }
                        }

                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "更多选项")
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            // 刷新
                            DropdownMenuItem(
                                text = { Text("刷新") },
                                onClick = { webView.reload(); expanded = false },
                                leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = "刷新") }
                            )

                            // 电脑/手机模式切换
                            DropdownMenuItem(
                                text = { Text(if (isDesktopMode) "切换到手机模式" else "切换到电脑模式") },
                                onClick = {
                                    isDesktopMode = !isDesktopMode
                                    webView.settings.userAgentString = if (isDesktopMode) DESKTOP_USER_AGENT else defaultUserAgent
                                    webView.settings.loadWithOverviewMode = !isDesktopMode
                                    Toast.makeText(context, if (isDesktopMode) "已切换到电脑模式" else "已切换到手机模式", Toast.LENGTH_SHORT).show()
                                    if (currentUrl.isNotBlank() && currentUrl != "about:blank") {
                                        webView.loadUrl(currentUrl)
                                    } else {
                                        Toast.makeText(context, "URL为空，请先输入网址。", Toast.LENGTH_LONG).show()
                                    }
                                    expanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        if (isDesktopMode) Icons.Filled.PhoneAndroid else Icons.Filled.DesktopWindows,
                                        contentDescription = if (isDesktopMode) "切换到手机模式" else "切换到电脑模式"
                                    )
                                }
                            )

                            if (enableDevToolsOptionInUi) {
                                DropdownMenuItem(
                                    onClick = {
                                        isDevToolsEnabled = !isDevToolsEnabled
                                        WebView.setWebContentsDebuggingEnabled(isDevToolsEnabled)
                                        Toast.makeText(context, "DevTools 网页调试已 ${if (isDevToolsEnabled) "启用" else "关闭"}。", Toast.LENGTH_SHORT).show()
                                    },
                                    leadingIcon = { Icon(Icons.Filled.Build, contentDescription = "DevTools") },
                                    text = { Text("DevTools 网页调试") },
                                    trailingIcon = { Switch(checked = isDevToolsEnabled, onCheckedChange = null) }
                                )
                            }
                        }
                    }
                },
            )
        },
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                content = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "请登录教务系统后切换到有课表显示的页面再点击导入课程，点击右上角的更多查看其他选项",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.width(12.dp))

                        Button(
                            onClick = {
                                assetJsPath?.let {
                                    showCourseTablePicker = true
                                } ?: run {
                                    Toast.makeText(context, "该适配器没有脚本，请手动导入。", Toast.LENGTH_LONG).show()
                                }
                            },
                            enabled = assetJsPath != null
                        ) {
                            Text("执行导入")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            // 渲染 WebView
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { webView },
                update = {}
            )

            // 加载进度条
            if (loadingProgress < 1.0f) {
                LinearProgressIndicator(
                    progress = { loadingProgress },
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }

            WebDialogHost(
                webView = webView,
                uiEvents = uiEventChannel.receiveAsFlow()
            )

            if (showCourseTablePicker) {
                CourseTablePickerDialog(
                    title = "选择一个课表以导入课程",
                    onDismissRequest = { showCourseTablePicker = false },
                    onTableSelected = { selectedTable ->
                        showCourseTablePicker = false
                        assetJsPath?.let { assetPath ->
                            try {
                                androidBridge?.setImportTableId(selectedTable.id)

                                val jsFile = File(context.filesDir, "repo/schools/resources/$assetPath")

                                if (jsFile.exists()) {
                                    val jsCode = jsFile.readText()
                                    webView.evaluateJavascript(jsCode, null)
                                    Toast.makeText(context, "正在执行导入脚本...", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "导入脚本文件不存在: ${jsFile.path}", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "加载导入脚本失败: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        } ?: run {
                            Toast.makeText(context, "该适配器没有导入脚本", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
        }
    }
}