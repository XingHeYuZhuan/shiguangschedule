// com/xingheyuzhuan/shiguangschedule/ui.schoolselection.web/WebDialogHost.kt
package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web

import android.webkit.WebView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.Flow

/**
 * 宿主：监听 AndroidBridge 事件，负责显示 JS 触发的 Compose 弹窗。
 */
@Composable
fun WebDialogHost(
    webView: WebView, // 仍需传入 WebView，但对话框逻辑已解耦。
    uiEvents: Flow<WebUiEvent> // 从 AndroidBridge 接收的 UI 事件流
) {
    var currentEvent by remember { mutableStateOf<WebUiEvent?>(null) }

    // 监听事件流
    LaunchedEffect(uiEvents) {
        uiEvents.collect { event ->
            currentEvent = event
        }
    }

    // 根据当前事件类型显示弹窗
    when (val event = currentEvent) {
        is WebUiEvent.ShowAlert -> {
            AlertHost(event.data, onConfirm = {
                event.callback(true)
                currentEvent = null
            }, onDismiss = {
                event.callback(false)
                currentEvent = null
            })
        }
        is WebUiEvent.ShowPrompt -> {
            PromptHost(
                event.data,
                onRequestValidation = { input ->
                    event.onRequestValidation(input)
                },
                errorFlow = event.errorFeedbackFlow, // 错误反馈流
                onCancel = {
                    event.onCancel()
                    currentEvent = null
                }
            )
        }
        is WebUiEvent.ShowSingleSelection -> {
            SingleSelectionHost(event.data, onResult = { index ->
                event.callback(index)
                currentEvent = null
            })
        }
        null -> Unit
    }
}

// ===============================================
// 具体的弹窗 Composable
// ===============================================

/** 显示 Alert/Confirm 弹窗。 */
@Composable
private fun AlertHost(data: AlertDialogData, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(data.title) },
        text = { Text(data.content) },
        confirmButton = {
            Button(onClick = onConfirm) { Text(data.confirmText) }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("取消") }
        }
    )
}

/**
 * 显示 Prompt 弹窗，通过 Flow 接收 JS 验证的错误反馈。
 * 核心逻辑：Compose 仅负责 UI 状态和输入发送，验证由 Bridge/JS 处理。
 * @param onRequestValidation 确定按钮点击时，发送输入给 Bridge 进行验证。
 * @param errorFlow 监听来自 Bridge/JS 的错误反馈。
 * @param onCancel 取消时调用。
 */
@Composable
private fun PromptHost(
    data: PromptDialogData,
    onRequestValidation: (String) -> Unit,
    errorFlow: Flow<String?>,
    onCancel: () -> Unit
) {
    var inputText by rememberSaveable { mutableStateOf(data.defaultText) }
    var errorText by rememberSaveable { mutableStateOf<String?>(null) } // JS 验证返回的错误信息

    // 监听错误反馈流
    LaunchedEffect(errorFlow) {
        errorFlow.collect { message ->
            // 收到错误时更新 UI 状态
            errorText = message
        }
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(data.title) },
        text = {
            Column {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = {
                        inputText = it
                        errorText = null // 用户修改输入时清空错误
                    },
                    label = { Text(data.tip) },
                    isError = errorText != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                errorText?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // 发送输入给 Bridge，请求验证
                    onRequestValidation(inputText)
                }
            ) { Text("确定") }
        },
        dismissButton = {
            Button(onClick = onCancel) { Text("取消") }
        }
    )
}

/** 显示单选列表弹窗。 */
@Composable
private fun SingleSelectionHost(data: SingleSelectionDialogData, onResult: (Int?) -> Unit) {
    var selectedIndex by rememberSaveable { mutableStateOf(data.defaultSelectedIndex) }

    AlertDialog(
        onDismissRequest = { onResult(null) },
        title = { Text(data.title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                data.items.forEachIndexed { index, item ->
                    ListItem(
                        headlineContent = { Text(item) },
                        leadingContent = {
                            RadioButton(
                                selected = (index == selectedIndex),
                                onClick = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedIndex = index }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onResult(selectedIndex) },
                enabled = selectedIndex != -1
            ) { Text("确定") }
        },
        dismissButton = {
            Button(onClick = { onResult(null) }) { Text("取消") }
        }
    )
}