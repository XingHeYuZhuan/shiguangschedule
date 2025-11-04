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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xingheyuzhuan.shiguangschedule.BuildConfig
import com.xingheyuzhuan.shiguangschedule.R
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
    courseScheduleRoute: String,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    val startedEmpty: Boolean = remember { initialUrl.isNullOrBlank() || initialUrl == "about:blank" }

    var currentUrl by remember { mutableStateOf(initialUrl ?: "about:blank") }
    var inputUrl by remember { mutableStateOf(initialUrl ?: "https://") }
    var loadingProgress by remember { mutableFloatStateOf(0f) }
    var pageTitle by remember {
        mutableStateOf(
            if (currentUrl.isBlank() || currentUrl == "about:blank")
                context.getString(R.string.title_enter_url)
            else
                context.getString(R.string.title_loading)
        )
    }
    var expanded by remember { mutableStateOf(false) }
    var isDesktopMode by remember { mutableStateOf(false) }

    var isEditingUrl by remember {
        mutableStateOf(startedEmpty)
    }

    var isDevToolsEnabled by remember { mutableStateOf(false) }
    var showCourseTablePicker by remember { mutableStateOf(false) }

    // --- DevTools 逻辑 ---
    val enableDevToolsOptionInUi = BuildConfig.ENABLE_DEV_TOOLS_OPTION_IN_UI
    val enableAddressBarToggleButton = BuildConfig.ENABLE_ADDRESS_BAR_TOGGLE_BUTTON

    // --- 浏览器配置和 Agent ---
    val defaultUserAgent = remember { WebSettings.getDefaultUserAgent(context) }

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
                    Toast.makeText(context, context.getString(R.string.toast_import_script_finished), Toast.LENGTH_LONG).show()
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
                            .setMessage(context.getString(R.string.dialog_ssl_error_message))
                            .setPositiveButton(context.getString(R.string.action_continue_browsing)) { _, _ ->
                                handler.proceed()
                            }
                            .setNegativeButton(context.getString(R.string.action_cancel)) { _, _ ->
                                handler.cancel()
                            }
                            .setCancelable(false)
                            .show()
                    } ?: run {
                        Toast.makeText(context, context.getString(R.string.toast_ssl_error_cancelled), Toast.LENGTH_LONG).show()
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
                            Toast.makeText(context, context.getString(R.string.toast_web_load_error_format, description), Toast.LENGTH_LONG).show()
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
        pageTitle = context.getString(R.string.title_loading)
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
                            contentDescription = stringResource(if (isEditingUrl) R.string.a11y_cancel_editing else R.string.a11y_back)
                        )
                    }
                },

                title = {
                    if (isEditingUrl) {
                        OutlinedTextField(
                            value = inputUrl,
                            onValueChange = { newQuery: String -> inputUrl = newQuery },
                            placeholder = { Text(stringResource(R.string.placeholder_enter_url_full)) },
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
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.a11y_load))
                            }
                        } else if (enableAddressBarToggleButton || startedEmpty) {
                            IconButton(onClick = {
                                isEditingUrl = true
                                inputUrl = webView.url?.takeIf { it.isNotBlank() && it != "about:blank" } ?: "https://"
                                keyboardController?.show()
                            }) {
                                Icon(Icons.Default.Link, contentDescription = stringResource(R.string.a11y_enter_url))
                            }
                        }

                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.a11y_more_options))
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            // 刷新
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_refresh)) },
                                onClick = { webView.reload(); expanded = false },
                                leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.a11y_refresh)) }
                            )

                            // 电脑/手机模式切换
                            val switchTextId = if (isDesktopMode) R.string.action_switch_to_phone_mode else R.string.action_switch_to_desktop_mode
                            val switchIcon = if (isDesktopMode) Icons.Filled.PhoneAndroid else Icons.Filled.DesktopWindows

                            DropdownMenuItem(
                                text = { Text(stringResource(switchTextId)) },
                                onClick = {
                                    isDesktopMode = !isDesktopMode
                                    webView.settings.userAgentString = if (isDesktopMode) DESKTOP_USER_AGENT else defaultUserAgent
                                    webView.settings.loadWithOverviewMode = !isDesktopMode

                                    val toastText = context.getString(if (isDesktopMode) R.string.toast_switched_to_desktop else R.string.toast_switched_to_phone)
                                    Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()

                                    if (currentUrl.isNotBlank() && currentUrl != "about:blank") {
                                        webView.loadUrl(currentUrl)
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.toast_url_empty_enter_first), Toast.LENGTH_LONG).show()
                                    }
                                    expanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        switchIcon,
                                        contentDescription = stringResource(switchTextId)
                                    )
                                }
                            )

                            if (enableDevToolsOptionInUi) {
                                DropdownMenuItem(
                                    onClick = {
                                        isDevToolsEnabled = !isDevToolsEnabled
                                        WebView.setWebContentsDebuggingEnabled(isDevToolsEnabled)

                                        val statusTextId = if (isDevToolsEnabled) R.string.status_enabled else R.string.status_disabled
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.toast_devtools_enabled_format, context.getString(statusTextId)),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    leadingIcon = { Icon(Icons.Filled.Build, contentDescription = stringResource(R.string.a11y_devtools)) },
                                    text = { Text(stringResource(R.string.item_devtools_debug)) },
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
                            text = stringResource(R.string.text_import_guide),
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
                                    Toast.makeText(context, context.getString(R.string.toast_no_script_manual_import), Toast.LENGTH_LONG).show()
                                }
                            },
                            enabled = assetJsPath != null
                        ) {
                            Text(stringResource(R.string.action_execute_import))
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
                    title = stringResource(R.string.dialog_title_select_table_for_import),
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
                                    Toast.makeText(context, context.getString(R.string.toast_executing_import_script), Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, context.getString(R.string.toast_import_script_not_found, jsFile.path), Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, context.getString(R.string.toast_load_import_script_failed, e.localizedMessage), Toast.LENGTH_LONG).show()
                            }
                        } ?: run {
                            Toast.makeText(context, context.getString(R.string.toast_no_import_script), Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
        }
    }
}