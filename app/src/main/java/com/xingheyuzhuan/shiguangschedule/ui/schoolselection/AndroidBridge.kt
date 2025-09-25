package com.xingheyuzhuan.shiguangschedule.ui.schoolselection

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseConversionRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseImportExport
import com.xingheyuzhuan.shiguangschedule.data.repository.TimeSlotRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val TAG = "AndroidBridge"

// 定义用于 Compose 弹窗的数据类 (保持不变)
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

// 用于解析 JS 端传来的时间段 JSON
@Serializable
data class TimeSlotJsonModel(
    val number: Int,
    val startTime: String,
    val endTime: String
)

/**
 * AndroidBridge 类用于提供 JavaScript 与 Android 原生功能交互的桥接。
 * 注入到 WebView 后，JavaScript 可以通过全局的 'AndroidBridge' 对象调用这些方法。
 *
 * @param context 应用程序上下文。
 * @param coroutineScope 用于启动协程来执行挂起函数（如保存数据）。
 * @param webView 传入的 WebView 实例，用于执行 JS 回调。
 * @param onShowComposeAlertDialog 通知 Compose 显示带确认按钮的弹窗。
 * @param onShowComposePromptDialog 通知 Compose 显示带输入框的弹窗。
 * @param onShowComposeSingleSelectionDialog 通知 Compose 显示单选列表弹窗。
 * @param courseConversionRepository 转换数据仓库的新依赖。
 * @param timeSlotRepository 时间段数据仓库的新依赖。
 * @param onTaskCompleted 任务完成后通知 Compose 端执行收尾操作
 */
class AndroidBridge(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val webView: WebView,
    private val onShowComposeAlertDialog: (AlertDialogData, (Boolean) -> Unit) -> Unit,
    private val onShowComposePromptDialog: (PromptDialogData, (String?) -> Unit) -> Unit,
    private val onShowComposeSingleSelectionDialog: (SingleSelectionDialogData, (Int?) -> Unit) -> Unit,
    private val courseConversionRepository: CourseConversionRepository,
    private val timeSlotRepository: TimeSlotRepository,
    private val onTaskCompleted: () -> Unit
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val handler = Handler(Looper.getMainLooper())

    // 用于保存用户选择的导入课表 ID，当用户取消选择时，该值为 null
    private var importTableId: String? = null

    // 用于存储当前 Toast 实例
    private var currentToast: Toast? = null

    // 提供一个公共方法供外部设置 ID
    fun setImportTableId(tableId: String) {
        this.importTableId = tableId
    }

    /**
     * JS 调用：显示一个短暂的 Toast 消息。
     */
    @JavascriptInterface
    fun showToast(message: String) {
        handler.post {
            currentToast?.cancel()
            val newToast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
            newToast.show()
            currentToast = newToast
        }
    }

    /**
     * JS 调用：显示一个带确认按钮的公告弹窗。
     *
     * @param titleText 弹窗标题。
     * @param contentText 弹窗内容。
     * @param confirmText 确认按钮文本。
     * @param promiseId 唯一 ID，用于 JS 侧 resolve 或 reject Promise。
     */
    @JavascriptInterface
    fun showAlert(titleText: String, contentText: String, confirmText: String, promiseId: String) {
        handler.post {
            val data = AlertDialogData(titleText, contentText, confirmText)
            onShowComposeAlertDialog(data) { confirmed ->
                if (confirmed) {
                    resolveJsPromise(promiseId, "true")
                } else {
                    resolveJsPromise(promiseId, "false")
                }
            }
        }
    }

    /**
     * JS 调用：显示一个带有输入框的弹窗（对应 AISchedulePrompt）。
     *
     * @param titleText 弹窗标题。
     * @param tipText 输入框提示文本。
     * @param defaultText 输入框默认文本。
     * @param validatorJsFunction JS 验证函数名称，JS 侧提供一个函数来验证用户输入。
     * @param promiseId 唯一 ID，用于 JS 侧 resolve或 reject Promise。
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
            onShowComposePromptDialog(data) { input ->
                if (input != null) {
                    val escapedInput = input.replace("'", "\\'")
                    resolveJsPromise(promiseId, "'$escapedInput'")
                } else {
                    resolveJsPromise(promiseId, "null")
                }
            }
        }
    }

    /**
     * JS 调用：显示一个单选列表弹窗，让用户从列表中选择一项。
     *
     * @param titleText 弹窗标题。
     * @param itemsJsonString 选项列表的 JSON 字符串，例如 `["选项1", "选项2", "选项3"]`。
     * @param defaultSelectedIndex 默认选中项的索引，如果为 -1 则不选中。
     * @param promiseId 唯一 ID，用于 JS 侧 resolve 或 reject Promise。
     */
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
                onShowComposeSingleSelectionDialog(data) { selectedIndex ->
                    if (selectedIndex != null) {
                        resolveJsPromise(promiseId, selectedIndex.toString())
                    } else {
                        resolveJsPromise(promiseId, "null")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "解析单选列表 itemsJsonString 失败: ${e.message}", e)
                Toast.makeText(context, "单选列表数据错误，无法显示。", Toast.LENGTH_LONG).show()
                rejectJsPromise(promiseId, "Invalid items JSON: ${e.message}")
            }
        }
    }

    /**
     * JS 调用：将解析后的课程数据传回 Android 端进行保存。
     *
     * @param coursesJsonString 课程数据的 JSON 字符串。
     * @param promiseId 唯一 ID，用于 JS 侧 resolve 或 reject Promise。
     */
    @JavascriptInterface
    fun saveImportedCourses(coursesJsonString: String, promiseId: String) {
        Log.d(TAG, "从 JS 接收到课程数据，大小: ${coursesJsonString.length / 1024} KB")
        coroutineScope.launch(Dispatchers.Main) {
            try {
                val tableId = importTableId

                if (tableId == null) {
                    Toast.makeText(context, "导入失败：未选择课表。", Toast.LENGTH_LONG).show()
                    rejectJsPromise(promiseId, "Course table selection cancelled.")
                    Log.e(TAG, "导入失败：用户取消了课表选择。")
                    return@launch
                }

                val importedCoursesList = json.decodeFromString<List<CourseImportExport.ImportCourseJsonModel>>(coursesJsonString)

                courseConversionRepository.importCoursesFromList(tableId, importedCoursesList)

                Toast.makeText(context, "课程导入成功！课表已更新。", Toast.LENGTH_LONG).show()
                resolveJsPromise(promiseId, "true")
                Log.d(TAG, "课程数据成功覆盖到课表: $tableId，共 ${importedCoursesList.size} 门课程。")
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "课程导入失败: ${e.message}", Toast.LENGTH_LONG).show()
                rejectJsPromise(promiseId, "Course import failed: ${e.message}")
                Log.e(TAG, "课程导入失败: ${e.message}", e)
            }
        }
    }
    /**
     * JS 调用：将预设时间段数据传回 Android 端进行保存。
     *
     * @param timeSlotsJsonString 时间段数据的 JSON 字符串。
     * @param promiseId 唯一 ID，用于 JS 侧 resolve 或 reject Promise。
     */
    @JavascriptInterface
    fun savePresetTimeSlots(timeSlotsJsonString: String, promiseId: String) {
        Log.d(TAG, "从 JS 接收到预设时间段数据，大小: ${timeSlotsJsonString.length / 1024} KB")
        coroutineScope.launch(Dispatchers.Main) {
            try {
                val tableId = importTableId

                importTableId = null

                if (tableId == null) {
                    Toast.makeText(context, "导入失败：未选择课表。", Toast.LENGTH_LONG).show()
                    rejectJsPromise(promiseId, "Course table selection cancelled.")
                    Log.e(TAG, "导入失败：用户取消了课表选择。")
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
                Log.d(TAG, "预设时间段成功保存，共 ${timeSlotEntities.size} 个。")
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "预设时间段导入失败: ${e.message}", Toast.LENGTH_LONG).show()
                rejectJsPromise(promiseId, "Preset time slots import failed: ${e.message}")
                Log.e(TAG, "预设时间段导入失败: ${e.message}", e)
            }
        }
    }

    /**
     * JS 调用：通知 Native 端整个 JS 任务已逻辑完成，是生命周期结束的信号。
     */
    @JavascriptInterface
    fun notifyTaskCompletion() {
        // 触发回调
        handler.post {
            onTaskCompleted()
        }
    }

    /**
     * 通过 evaluateJavascript 在 JS 环境中解决 Promise。
     * @param promiseId 对应 JS Promise 的 ID。
     * @param result 要传递给 Promise resolve 的结果字符串。
     */
    private fun resolveJsPromise(promiseId: String, result: String) {
        handler.post {
            webView.evaluateJavascript("window._resolveAndroidPromise('$promiseId', $result);", null)
        }
    }

    /**
     * 通过 evaluateJavascript 在 JS 环境中拒绝 Promise。
     * @param promiseId 对应 JS Promise 的 ID。
     * @param error 要传递给 Promise reject 的错误信息字符串。
     */
    private fun rejectJsPromise(promiseId: String, error: String) {
        handler.post {
            val escapedError = error.replace("'", "\\'")
            webView.evaluateJavascript("window._rejectAndroidPromise('$promiseId', '$escapedError');", null)
        }
    }
}