// com/xingheyuzhuan/shiguangschedule/ui/schoolselection/web/AndroidBridge.kt
package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebView
import android.widget.Toast
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseConversionRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseImportExport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.xingheyuzhuan.shiguangschedule.data.repository.TimeSlotRepository

private const val TAG = "AndroidBridge"


// JS 端传来的时间段 JSON 模型
@Serializable
data class TimeSlotJsonModel(
    val number: Int,
    val startTime: String,
    val endTime: String
)

/**
 * AndroidBridge：处理 WebView 与 Native 代码的通信。
 * 通过 Channel 发送 WebUiEvent，实现与 Compose UI 的解耦。
 */
class AndroidBridge(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val webView: WebView,
    private val uiEventChannel: Channel<WebUiEvent>, // UI 事件通道
    private val courseConversionRepository: CourseConversionRepository,
    private val timeSlotRepository: TimeSlotRepository,
    private val onTaskCompleted: () -> Unit
) {
    // ... (其他私有成员保持不变)
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val handler = Handler(Looper.getMainLooper())
    private var importTableId: String? = null
    private var currentToast: Toast? = null

    // 外部设置导入课表 ID
    fun setImportTableId(tableId: String) {
        this.importTableId = tableId
    }

    /** JS 调用：显示短暂的 Toast 消息。 */
    @JavascriptInterface
    fun showToast(message: String) {
        handler.post {
            currentToast?.cancel()
            val newToast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
            newToast.show()
            currentToast = newToast
        }
    }

    /** JS 调用：显示 Alert 弹窗。通过 Channel 发送事件。 */
    @JavascriptInterface
    fun showAlert(titleText: String, contentText: String, confirmText: String, promiseId: String) {
        handler.post {
            val data = AlertDialogData(titleText, contentText, confirmText)

            val promiseCallback: (Boolean) -> Unit = { confirmed ->
                if (confirmed) {
                    resolveJsPromise(promiseId, "true")
                } else {
                    resolveJsPromise(promiseId, "false")
                }
            }

            val success = uiEventChannel.trySend(
                WebUiEvent.ShowAlert(data, promiseCallback)
            ).isSuccess

            if (!success) {
                rejectJsPromise(promiseId, "原生 UI 事件队列已满。")
            }
        }
    }

    /**
     * JS 调用：显示 Prompt 弹窗，并支持异步 JS 验证。
     * - Compose 发送输入给 onRequestValidation。
     * - Android 执行 JS 验证，结果通过 errorFeedbackFlow 反馈。
     */
    @JavascriptInterface
    fun showPrompt(
        titleText: String,
        tipText: String,
        defaultText: String,
        validatorJsFunction: String,
        promiseId: String
    ) {
        handler.post {
            val data = PromptDialogData(titleText, tipText, defaultText, validatorJsFunction)

            // 错误反馈通道
            val errorFlow = MutableSharedFlow<String?>(extraBufferCapacity = 1)

            // 取消回调：解决 Promise 为 null
            val onCancel: () -> Unit = {
                resolveJsPromise(promiseId, "null")
            }

            // 验证请求回调 (Compose 触发)
            val onRequestValidation: (String) -> Unit = { input ->
                handler.post {
                    // 无验证函数时直接成功
                    if (data.validatorJsFunction.isNullOrEmpty()) {
                        val escapedInput = input.replace("'", "\\'")
                        resolveJsPromise(promiseId, "'$escapedInput'")
                        return@post
                    }

                    // 构造 JS 验证代码
                    val jsScript = "javascript: ${data.validatorJsFunction}('${input.replace("'", "\\'")}')"

                    // 执行 JS 验证
                    webView.evaluateJavascript(jsScript, ValueCallback { result ->
                        val validationResult = result?.trim('\"')

                        if (validationResult.isNullOrEmpty() || validationResult.equals("false", ignoreCase = true)) {
                            // 验证成功：解决 Promise
                            val escapedInput = input.replace("'", "\\'")
                            resolveJsPromise(promiseId, "'$escapedInput'")
                        } else {
                            // 验证失败：发送错误给 Compose UI
                            coroutineScope.launch {
                                errorFlow.emit(validationResult)
                            }
                        }
                    })
                }
            }

            // 发送事件
            val success = uiEventChannel.trySend(
                WebUiEvent.ShowPrompt(data, onRequestValidation, errorFlow.asSharedFlow(), onCancel)
            ).isSuccess

            if (!success) {
                rejectJsPromise(promiseId, "原生 UI 事件队列已满。")
            }
        }
    }

    /** JS 调用：显示单选列表弹窗。通过 Channel 发送事件。 */
    @JavascriptInterface
    fun showSingleSelection(
        titleText: String,
        itemsJsonString: String,
        defaultSelectedIndex: Int,
        promiseId: String
    ) {
        handler.post {
            try {
                val items = json.decodeFromString<List<String>>(itemsJsonString)
                val data = SingleSelectionDialogData(titleText, items, defaultSelectedIndex)

                val promiseCallback: (Int?) -> Unit = { selectedIndex ->
                    if (selectedIndex != null) {
                        resolveJsPromise(promiseId, selectedIndex.toString())
                    } else {
                        resolveJsPromise(promiseId, "null")
                    }
                }

                val success = uiEventChannel.trySend(
                    WebUiEvent.ShowSingleSelection(data, promiseCallback)
                ).isSuccess

                if (!success) {
                    rejectJsPromise(promiseId, "原生 UI 事件队列已满。")
                }

            } catch (e: Exception) {
                Log.e(TAG, "解析单选列表 itemsJsonString 失败: ${e.message}", e)
                Toast.makeText(context, "单选列表数据错误，无法显示。", Toast.LENGTH_LONG).show()
                rejectJsPromise(promiseId, "选项列表 JSON 无效: ${e.message}")
            }
        }
    }

    /** JS 调用：将课程数据传回 Android 端进行保存。 */
    @JavascriptInterface
    fun saveImportedCourses(coursesJsonString: String, promiseId: String) {
        Log.d(TAG, "接收到课程数据，大小: ${coursesJsonString.length / 1024} KB")
        coroutineScope.launch(Dispatchers.Main) {
            try {
                val tableId = importTableId

                if (tableId == null) {
                    Toast.makeText(context, "导入失败：未选择课表。", Toast.LENGTH_LONG).show()
                    rejectJsPromise(promiseId, "课表选择已取消。")
                    return@launch
                }

                val importedCoursesList = json.decodeFromString<List<CourseImportExport.ImportCourseJsonModel>>(coursesJsonString)
                courseConversionRepository.importCoursesFromList(tableId, importedCoursesList)

                Toast.makeText(context, "课程导入成功！课表已更新。", Toast.LENGTH_LONG).show()
                resolveJsPromise(promiseId, "true")
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "课程导入失败: ${e.message}", Toast.LENGTH_LONG).show()
                rejectJsPromise(promiseId, "课程导入失败: ${e.message}")
            }
        }
    }

    /** JS 调用：将预设时间段数据传回 Android 端进行保存。 */
    @JavascriptInterface
    fun savePresetTimeSlots(timeSlotsJsonString: String, promiseId: String) {
        Log.d(TAG, "接收到预设时间段数据，大小: ${timeSlotsJsonString.length / 1024} KB")
        coroutineScope.launch(Dispatchers.Main) {
            try {
                val tableId = importTableId

                importTableId = null

                if (tableId == null) {
                    Toast.makeText(context, "导入失败：未选择课表。", Toast.LENGTH_LONG).show()
                    rejectJsPromise(promiseId, "课表选择已取消。")
                    return@launch
                }

                val importedTimeSlotsJson = json.decodeFromString<List<TimeSlotJsonModel>>(timeSlotsJsonString)

                val timeSlotEntities = importedTimeSlotsJson.map { jsonModel ->
                    TimeSlot(
                        number = jsonModel.number,
                        startTime = jsonModel.startTime,
                        endTime = jsonModel.endTime,
                        courseTableId = tableId
                    )
                }

                timeSlotRepository.replaceAllForCourseTable(tableId, timeSlotEntities)

                Toast.makeText(context, "预设时间段导入成功！", Toast.LENGTH_LONG).show()
                resolveJsPromise(promiseId, "true")
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "预设时间段导入失败: ${e.message}", Toast.LENGTH_LONG).show()
                rejectJsPromise(promiseId, "预设时间段导入失败: ${e.message}")
            }
        }
    }

    /** JS 调用：通知 Native 端 JS 任务已逻辑完成。 */
    @JavascriptInterface
    fun notifyTaskCompletion() {
        handler.post {
            onTaskCompleted()
        }
    }

    /** 在 JS 环境中解决 Promise。 */
    private fun resolveJsPromise(promiseId: String, result: String) {
        handler.post {
            webView.evaluateJavascript("window._resolveAndroidPromise('$promiseId', $result);", null)
        }
    }

    /** 在 JS 环境中拒绝 Promise。 */
    private fun rejectJsPromise(promiseId: String, error: String) {
        handler.post {
            val escapedError = error.replace("'", "\\'")
            webView.evaluateJavascript("window._rejectAndroidPromise('$promiseId', '$escapedError');", null)
        }
    }
}