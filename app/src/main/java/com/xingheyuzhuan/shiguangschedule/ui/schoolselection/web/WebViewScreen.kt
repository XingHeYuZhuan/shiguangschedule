package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web

import android.annotation.SuppressLint
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
import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.xingheyuzhuan.shiguangschedule.BuildConfig
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.ui.components.CourseTablePickerDialog
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import org.koin.compose.viewmodel.koinViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit,
    initialUrl: String?,
    assetJsPath: String?,
    viewModel: WebViewModel = koinViewModel()
) {
    val courseConversionRepository = viewModel.courseConversionRepository

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    val startedEmpty: Boolean = remember { initialUrl.isNullOrBlank() || initialUrl == "about:blank" }

    // --- 预取字符串资源 ---
    val titleEnterUrl = stringResource(R.string.title_enter_url)
    val titleLoading = stringResource(R.string.title_loading)
    val toastImportFinished = stringResource(R.string.toast_import_script_finished)
    val toastSwitchedToDesktop = stringResource(R.string.toast_switched_to_desktop)
    val toastSwitchedToPhone = stringResource(R.string.toast_switched_to_phone)
    val toastUrlEmpty = stringResource(R.string.toast_url_empty_enter_first)
    val toastDevToolsEnabledFmt = stringResource(R.string.toast_devtools_enabled_format)
    val statusEnabled = stringResource(R.string.status_enabled)
    val statusDisabled = stringResource(R.string.status_disabled)
    val toastNoManualImport = stringResource(R.string.toast_no_script_manual_import)
    val toastExecutingImport = stringResource(R.string.toast_executing_import_script)
    val toastNoImportScript = stringResource(R.string.toast_no_import_script)
    val toastImportNotFoundFmt = stringResource(R.string.toast_import_script_not_found)
    val toastLoadImportFailedFmt = stringResource(R.string.toast_load_import_script_failed)

    var currentUrl by remember { mutableStateOf(initialUrl ?: "about:blank") }
    var inputUrl by remember { mutableStateOf(if (startedEmpty) "" else (initialUrl ?: "")) }
    var loadingProgress by remember { mutableFloatStateOf(0f) }
    var pageTitle by remember {
        mutableStateOf(
            if (startedEmpty) titleEnterUrl else titleLoading
        )
    }
    var expanded by remember { mutableStateOf(false) }
    var isDesktopMode by remember { mutableStateOf(false) }

    var isEditingUrl by remember {
        mutableStateOf(startedEmpty)
    }

    var isDevToolsEnabled by remember { mutableStateOf(false) }
    var showCourseTablePicker by remember { mutableStateOf(false) }

    var sslErrorHandleState by remember {
        mutableStateOf<Pair<SslErrorHandler, SslError>?>(null)
    }

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

            // 实例化 AndroidBridge
            androidBridge = AndroidBridge(
                context = context,
                coroutineScope = coroutineScope,
                webView = this,
                uiEventChannel = uiEventChannel,
                courseConversionRepository = courseConversionRepository,
                onTaskCompleted = {
                    Toast.makeText(context, toastImportFinished, Toast.LENGTH_LONG).show()
                    onNavigate(Destination.CourseSchedule)
                }
            )
            addJavascriptInterface(androidBridge!!, "AndroidBridge")
            addJavascriptInterface(WebPostBridge(), "WebPostService")
        }
    }

    // 处理系统返回键
    BackHandler(enabled = !isEditingUrl) {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            onBack()
        }
    }

    LaunchedEffect(isDesktopMode) {
        val compatDelegate = WebCompatDelegate(webView)
        compatDelegate.enhanceSettings(isDesktopMode)

        webView.settings.userAgentString = if (isDesktopMode) DESKTOP_USER_AGENT else defaultUserAgent

        val businessClient = object : WebViewClient() {
            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean = false

            @SuppressLint("WebViewClientOnReceivedSslError")
            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                sslErrorHandleState = Pair(handler, error)
            }

            override fun onReceivedError(view: WebView, request: android.webkit.WebResourceRequest, error: android.webkit.WebResourceError) {
                if (request.isForMainFrame) {
                    val description = error.description.toString()
                    view.post {
                        Toast.makeText(view.context, view.context.getString(R.string.toast_web_load_error_format, description), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        val businessChrome = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                loadingProgress = newProgress / 100f
            }
            override fun onReceivedTitle(view: WebView?, title: String?) {
                if (title != null) pageTitle = title
            }
        }
        webView.webViewClient = compatDelegate.wrapWebViewClient(businessClient, isDesktopMode)
        webView.webChromeClient = compatDelegate.wrapWebChromeClient(businessChrome) { /* 进度已在 businessChrome 处理 */ }
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
        pageTitle = titleLoading
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        if (isEditingUrl) {
                            isEditingUrl = false
                            val rawUrl = webView.url
                            inputUrl = if (rawUrl.isNullOrBlank() || rawUrl == "about:blank") "" else rawUrl
                            keyboardController?.hide()
                        } else {
                            onBack()
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
                                onGo = {
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
                        Text(
                            text = pageTitle,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
                                val rawUrl = webView.url
                                inputUrl = if (rawUrl.isNullOrBlank() || rawUrl == "about:blank") "" else rawUrl
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
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_refresh)) },
                                onClick = { webView.reload(); expanded = false },
                                leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.a11y_refresh)) }
                            )

                            val switchTextId = if (isDesktopMode) R.string.action_switch_to_phone_mode else R.string.action_switch_to_desktop_mode
                            val switchIcon = if (isDesktopMode) Icons.Filled.PhoneAndroid else Icons.Filled.DesktopWindows

                            DropdownMenuItem(
                                text = { Text(stringResource(switchTextId)) },
                                onClick = {
                                    isDesktopMode = !isDesktopMode

                                    val tText = if (isDesktopMode) toastSwitchedToDesktop else toastSwitchedToPhone
                                    Toast.makeText(context, tText, Toast.LENGTH_SHORT).show()

                                    val urlToReload = webView.url
                                    if (!urlToReload.isNullOrBlank() && urlToReload != "about:blank") {
                                        webView.loadUrl(urlToReload)
                                    } else if (inputUrl.isNotBlank()) {
                                        currentUrl = inputUrl
                                    } else {
                                        Toast.makeText(context, toastUrlEmpty, Toast.LENGTH_LONG).show()
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

                                        val statusText = if (isDevToolsEnabled) statusEnabled else statusDisabled
                                        Toast.makeText(
                                            context,
                                            toastDevToolsEnabledFmt.format(statusText),
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
                                    Toast.makeText(context, toastNoManualImport, Toast.LENGTH_LONG).show()
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
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { webView },
                update = {}
            )

            if (loadingProgress < 1.0f) {
                LinearProgressIndicator(
                    progress = { loadingProgress },
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }

            WebDialogHost(
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
                                    Toast.makeText(context, toastExecutingImport, Toast.LENGTH_SHORT).show()
                                } else {
                                    val msg = toastImportNotFoundFmt.format(jsFile.path)
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                val errMsg = toastLoadImportFailedFmt.format(e.localizedMessage ?: "")
                                Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                            }
                        } ?: run {
                            Toast.makeText(context, toastNoImportScript, Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }

            sslErrorHandleState?.let { (handler, _) ->
                AlertDialog(
                    onDismissRequest = {
                        handler.cancel()
                        sslErrorHandleState = null
                    },
                    title = { Text(stringResource(R.string.dialog_ssl_error_title)) },
                    text = { Text(stringResource(R.string.dialog_ssl_error_message)) },
                    confirmButton = {
                        Button(
                            onClick = {
                                handler.proceed()
                                sslErrorHandleState = null
                            }
                        ) {
                            Text(stringResource(R.string.action_continue_browsing))
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = {
                                handler.cancel()
                                sslErrorHandleState = null
                            }
                        ) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                )
            }
        }
    }
}