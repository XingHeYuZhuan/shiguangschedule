// com/xingheyuzhuan/shiguangschedule/ui/schoolselection/web/WebUiEvent.kt
package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web

import kotlinx.coroutines.flow.Flow

// ===============================================
// 1. JS 交互所需的数据结构 (从旧 AndroidBridge.kt 移动过来)
// ===============================================

data class AlertDialogData(
    val title: String,
    val content: String,
    val confirmText: String
)

data class PromptDialogData(
    val title: String,
    val tip: String,
    val defaultText: String,
    val validatorJsFunction: String?
)

data class SingleSelectionDialogData(
    val title: String,
    val items: List<String>,
    val defaultSelectedIndex: Int = -1
)

// ===============================================
// 2. 统一的 Web UI 交互事件契约
// ===============================================

/**
 * 表示所有由 JavaScript 触发、需要 Compose UI 响应的交互事件。
 */
sealed interface WebUiEvent {

    /** 对应 AndroidBridge.showAlert */
    data class ShowAlert(
        val data: AlertDialogData,
        val callback: (confirmed: Boolean) -> Unit
    ) : WebUiEvent

    /** 对应 AndroidBridge.showPrompt */
    data class ShowPrompt(
        val data: PromptDialogData,
        val onRequestValidation: (String) -> Unit,
        val errorFeedbackFlow: Flow<String?>,
        val onCancel: () -> Unit
    ) : WebUiEvent

    /** 对应 AndroidBridge.showSingleSelection */
    data class ShowSingleSelection(
        val data: SingleSelectionDialogData,
        val callback: (selectedIndex: Int?) -> Unit
    ) : WebUiEvent
}