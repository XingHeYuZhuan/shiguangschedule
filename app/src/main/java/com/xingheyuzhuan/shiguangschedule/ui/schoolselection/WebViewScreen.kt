package com.xingheyuzhuan.shiguangschedule.ui.schoolselection

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xingheyuzhuan.shiguangschedule.MyApplication
import com.xingheyuzhuan.shiguangschedule.Screen
import com.xingheyuzhuan.shiguangschedule.data.SchoolRepository
import com.xingheyuzhuan.shiguangschedule.ui.components.CourseTablePickerDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    navController: NavController,
    schoolId: String,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val app = context.applicationContext as MyApplication
    val courseConversionRepository = app.courseConversionRepository
    val timeSlotRepository = app.timeSlotRepository

    var loadingProgress by remember { mutableFloatStateOf(0f) }
    var pageTitle by remember { mutableStateOf("加载中...") }
    var expanded by remember { mutableStateOf(false) }
    var isDesktopMode by remember { mutableStateOf(false) }

    val defaultUserAgent = remember { WebSettings.getDefaultUserAgent(context) }
    val desktopUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    var showComposeAlertDialogData by remember { mutableStateOf<AlertDialogData?>(null) }
    var alertDialogConfirmCallback by remember { mutableStateOf<(Boolean) -> Unit>({}) }

    var showComposePromptDialogData by remember { mutableStateOf<PromptDialogData?>(null) }
    var promptDialogInputText by remember { mutableStateOf("") }
    var promptDialogErrorText by remember { mutableStateOf<String?>(null) }
    var promptDialogCallback by remember { mutableStateOf<(String?) -> Unit>({}) }

    var showComposeSingleSelectionDialogData by remember { mutableStateOf<SingleSelectionDialogData?>(null) }
    var singleSelectionSelectedIndex by remember { mutableIntStateOf(-1) }
    var singleSelectionCallback by remember { mutableStateOf<(Int?) -> Unit>({}) }

    var showCourseTablePicker by remember { mutableStateOf(false) }

    var androidBridge: AndroidBridge? by remember { mutableStateOf(null) }

    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            settings.javaScriptEnabled = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.textZoom = 100
            settings.userAgentString = defaultUserAgent

            androidBridge = AndroidBridge(
                context = context,
                coroutineScope = coroutineScope,
                onTaskCompleted = {
                    // 这是在 JS 脚本调用 AndroidBridge.notifyTaskCompletion() 后执行的 Native 逻辑

                    // 1. 实现您提出的交互性 Toast/弹窗 (Toast是最简单且有效的)
                    Toast.makeText(context, "导入脚本执行完毕，返回课表页面。", Toast.LENGTH_LONG).show()

                    // 2. 执行导航逻辑
                    navController.popBackStack(
                        route = Screen.CourseSchedule.route,
                        inclusive = false
                    )
                },
                webView = this,
                onShowComposeAlertDialog = { data, callback ->
                    showComposeAlertDialogData = data
                    alertDialogConfirmCallback = callback
                },
                onShowComposePromptDialog = { data, callback ->
                    showComposePromptDialogData = data
                    promptDialogInputText = data.defaultText
                    promptDialogErrorText = null
                    promptDialogCallback = callback
                },
                onShowComposeSingleSelectionDialog = { data, callback ->
                    showComposeSingleSelectionDialogData = data
                    singleSelectionSelectedIndex = data.defaultSelectedIndex
                    singleSelectionCallback = callback
                },
                courseConversionRepository = courseConversionRepository,
                timeSlotRepository = timeSlotRepository
            )
            addJavascriptInterface(androidBridge!!, "AndroidBridge")

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
                    pageTitle = view?.title ?: "未知页面"
                    view?.evaluateJavascript("""
                        window._androidPromiseResolvers = {};
                        window._androidPromiseRejectors = {};

                        window._resolveAndroidPromise = function(promiseId, result) {
                            if (window._androidPromiseResolvers[promiseId]) {
                                window._androidPromiseResolvers[promiseId](result);
                                delete window._androidPromiseResolvers[promiseId];
                                delete window._androidPromiseRejectors[promiseId];
                            }
                        };

                        window._rejectAndroidPromise = function(promiseId, error) {
                            if (window._androidPromiseRejectors[promiseId]) {
                                window._androidPromiseRejectors[promiseId](new Error(error));
                                delete window._androidPromiseResolvers[promiseId];
                                delete window._androidPromiseRejectors[promiseId];
                            }
                        };

                        window.AndroidBridgePromise = {
                            showAlert: function(title, content, confirmText) {
                                return new Promise((resolve, reject) => {
                                    const promiseId = 'alert_' + Date.now() + Math.random().toString(36).substring(2);
                                    window._androidPromiseResolvers[promiseId] = resolve;
                                    window._androidPromiseRejectors[promiseId] = reject;
                                    AndroidBridge.showAlert(title, content, confirmText, promiseId);
                                });
                            },
                            showPrompt: function(title, tip, defaultText, validatorJsFunction) {
                                return new Promise((resolve, reject) => {
                                    const promiseId = 'prompt_' + Date.now() + Math.random().toString(36).substring(2);
                                    window._androidPromiseResolvers[promiseId] = resolve;
                                    window._androidPromiseRejectors[promiseId] = reject;
                                    AndroidBridge.showPrompt(title, tip, defaultText, validatorJsFunction, promiseId);
                                });
                            },
                            showSingleSelection: function(title, itemsJsonString, defaultSelectedIndex) {
                                return new Promise((resolve, reject) => {
                                    const promiseId = 'singleSelect_' + Date.now() + Math.random().toString(36).substring(2);
                                    window._androidPromiseResolvers[promiseId] = resolve;
                                    window._androidPromiseRejectors[promiseId] = reject;
                                    AndroidBridge.showSingleSelection(title, itemsJsonString, defaultSelectedIndex, promiseId);
                                });
                            },
                            saveImportedCourses: function(coursesJsonString) {
                                return new Promise((resolve, reject) => {
                                    const promiseId = 'saveCourses_' + Date.now() + Math.random().toString(36).substring(2);
                                    window._androidPromiseResolvers[promiseId] = resolve;
                                    window._androidPromiseRejectors[promiseId] = reject;
                                    AndroidBridge.saveImportedCourses(coursesJsonString, promiseId);
                                });
                            },
                            savePresetTimeSlots: function(timeSlotsJsonString) {
                                return new Promise((resolve, reject) => {
                                    const promiseId = 'saveTimeSlots_' + Date.now() + Math.random().toString(36).substring(2);
                                    window._androidPromiseResolvers[promiseId] = resolve;
                                    window._androidPromiseRejectors[promiseId] = reject;
                                    AndroidBridge.savePresetTimeSlots(timeSlotsJsonString, promiseId);
                                });
                            }
                        };
                    """, null)
                }

                override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    Toast.makeText(context, "网页加载错误: $description", Toast.LENGTH_LONG).show()
                }
            }

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
        }
    }

    var schoolImportUrl by remember { mutableStateOf<String?>(null) }
    var schoolAssetJsPath by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(schoolId) {
        val school = withContext(Dispatchers.IO) {
            SchoolRepository.getSchoolById(context, schoolId)
        }
        school?.let {
            schoolImportUrl = it.importUrl
            schoolAssetJsPath = it.assetJsPath
            webView.loadUrl(it.importUrl)
            pageTitle = it.name
        } ?: run {
            Toast.makeText(context, "未找到学校信息，无法加载网页。", Toast.LENGTH_LONG).show()
            navController.popBackStack()
        }
    }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pageTitle) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "更多选项")
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("刷新") },
                            onClick = {
                                webView.reload()
                                expanded = false
                            },
                            leadingIcon = {
                                Icon(Icons.Filled.Refresh, contentDescription = "刷新")
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(if (isDesktopMode) "切换到手机模式" else "切换到电脑模式")
                            },
                            onClick = {
                                isDesktopMode = !isDesktopMode
                                if (isDesktopMode) {
                                    webView.settings.userAgentString = desktopUserAgent
                                    Toast.makeText(context, "已切换到电脑模式", Toast.LENGTH_SHORT).show()
                                } else {
                                    webView.settings.userAgentString = defaultUserAgent
                                    Toast.makeText(context, "已切换到手机模式", Toast.LENGTH_SHORT).show()
                                }
                                schoolImportUrl?.let { url ->
                                    webView.loadUrl(url)
                                } ?: run {
                                    Toast.makeText(context, "无法加载网页，URL为空。", Toast.LENGTH_LONG).show()
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
                        DropdownMenuItem(
                            text = { Text("导入课程") },
                            onClick = {
                                expanded = false
                                schoolAssetJsPath?.let { assetPath ->
                                    showCourseTablePicker = true
                                } ?: run {
                                    Toast.makeText(context, "该学校没有适配代码，请手动导入。", Toast.LENGTH_LONG).show()
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Filled.Download, contentDescription = "导入课程")
                            }
                        )
                    }
                }
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    webView
                },
                update = {}
            )

            if (loadingProgress < 1.0f) {
                LinearProgressIndicator(
                    progress = { loadingProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }

            showComposeAlertDialogData?.let { data ->
                AlertDialog(
                    onDismissRequest = {
                        showComposeAlertDialogData = null
                        alertDialogConfirmCallback(false)
                    },
                    title = { Text(data.title) },
                    text = { Text(data.content) },
                    confirmButton = {
                        Button(onClick = {
                            showComposeAlertDialogData = null
                            alertDialogConfirmCallback(true)
                        }) {
                            Text(data.confirmText)
                        }
                    },
                    dismissButton = {
                        Button(onClick = {
                            showComposeAlertDialogData = null
                            alertDialogConfirmCallback(false)
                        }) {
                            Text("取消")
                        }
                    }
                )
            }

            showComposePromptDialogData?.let { data ->
                AlertDialog(
                    onDismissRequest = {
                        showComposePromptDialogData = null
                        promptDialogCallback(null)
                    },
                    title = { Text(data.title) },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = promptDialogInputText,
                                onValueChange = {
                                    promptDialogInputText = it
                                    promptDialogErrorText = null
                                },
                                label = { Text(data.tip) },
                                isError = promptDialogErrorText != null,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            promptDialogErrorText?.let {
                                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            data.validatorJsFunction?.let { validator ->
                                webView.evaluateJavascript("$validator('${promptDialogInputText.replace("'", "\\'")}')") { validationResult ->
                                    if (validationResult?.trim('"') == "false") {
                                        showComposePromptDialogData = null
                                        promptDialogCallback(promptDialogInputText)
                                    } else {
                                        promptDialogErrorText = validationResult?.trim('"')
                                    }
                                }
                            } ?: run {
                                showComposePromptDialogData = null
                                promptDialogCallback(promptDialogInputText)
                            }
                        }) {
                            Text("确定")
                        }
                    },
                    dismissButton = {
                        Button(onClick = {
                            showComposePromptDialogData = null
                            promptDialogCallback(null)
                        }) {
                            Text("取消")
                        }
                    }
                )
            }

            showComposeSingleSelectionDialogData?.let { data ->
                AlertDialog(
                    onDismissRequest = {
                        showComposeSingleSelectionDialogData = null
                        singleSelectionCallback(null)
                    },
                    title = { Text(data.title) },
                    text = {
                        Column {
                            data.items.forEachIndexed { index, item ->
                                ListItem(
                                    headlineContent = { Text(item) },
                                    leadingContent = {
                                        RadioButton(
                                            selected = (index == singleSelectionSelectedIndex),
                                            onClick = null
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            singleSelectionSelectedIndex = index
                                        }
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            showComposeSingleSelectionDialogData = null
                            singleSelectionCallback(if (singleSelectionSelectedIndex != -1) singleSelectionSelectedIndex else null)
                        }) {
                            Text("确定")
                        }
                    },
                    dismissButton = {
                        Button(onClick = {
                            showComposeSingleSelectionDialogData = null
                            singleSelectionCallback(null)
                        }) {
                            Text("取消")
                        }
                    }
                )
            }
        }
    }
    if (showCourseTablePicker) {
        CourseTablePickerDialog(
            title = "选择一个课表以导入课程",
            onDismissRequest = { showCourseTablePicker = false },
            onTableSelected = { selectedTable ->
                showCourseTablePicker = false
                schoolAssetJsPath?.let { assetPath ->
                    try {
                        androidBridge?.setImportTableId(selectedTable.id)
                        val jsFile = File(context.filesDir, "repo/$assetPath")
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
                    Toast.makeText(context, "该学校没有导入脚本", Toast.LENGTH_LONG).show()
                }
            }
        )
    }
}