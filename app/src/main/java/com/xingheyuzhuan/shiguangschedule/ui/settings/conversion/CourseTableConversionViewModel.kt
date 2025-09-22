package com.xingheyuzhuan.shiguangschedule.ui.settings.conversion

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.xingheyuzhuan.shiguangschedule.MyApplication
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseConversionRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseImportExport
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseTableRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset

/**
 * 课表导入/导出界面的 ViewModel。
 * 处理所有业务逻辑和状态，并通过事件通道与 UI 沟通。
 */
class CourseTableConversionViewModel(
    private val courseConversionRepository: CourseConversionRepository,
    private val courseTableRepository: CourseTableRepository,
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    // UI 状态流，仅包含 UI 显示相关的状态（如对话框可见性、加载状态）
    private val _uiState = MutableStateFlow(ConversionUiState())
    val uiState = _uiState.asStateFlow()

    // UI 事件通道，用于发送一次性副作用（如启动文件选择器，显示 Snackbar）
    private val _events = Channel<ConversionEvent>()
    val events = _events.receiveAsFlow()

    fun onImportClick() {
        _uiState.value = _uiState.value.copy(showImportTableDialog = true)
    }

    fun onExportClick() {
        _uiState.value = _uiState.value.copy(
            showExportTableDialog = true,
            exportType = ExportType.JSON
        )
    }

    fun onExportIcsClick() {
        _uiState.value = _uiState.value.copy(
            showExportTableDialog = true,
            exportType = ExportType.ICS
        )
    }

    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(
            showImportTableDialog = false,
            showExportTableDialog = false
        )
    }

    fun onImportTableSelected(tableId: String) {
        viewModelScope.launch {
            _events.send(ConversionEvent.LaunchImportFilePicker(tableId))
            dismissDialog()
        }
    }

    fun onExportTableSelected(tableId: String, alarmMinutes: Int?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                if (_uiState.value.exportType == ExportType.JSON) {
                    val jsonModel = courseConversionRepository.exportCourseTableToJson(tableId)
                    if (jsonModel != null) {
                        val jsonString = Json.encodeToString(jsonModel)
                        _events.send(ConversionEvent.LaunchExportFileCreator(jsonString))
                    } else {
                        _events.send(ConversionEvent.ShowMessage("导出失败：找不到课表"))
                    }
                } else if (_uiState.value.exportType == ExportType.ICS) {
                    _events.send(ConversionEvent.LaunchExportIcsFileCreator(tableId, alarmMinutes))
                }
            } catch (e: Exception) {
                Log.e("CourseTableConversionViewModel", "导出失败：${e.message}", e)
                _events.send(ConversionEvent.ShowMessage("导出失败：${e.message}"))
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
                dismissDialog()
            }
        }
    }

    fun handleFileImport(tableId: String, inputStream: InputStream) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val jsonString = inputStream.bufferedReader(Charset.forName("UTF-8")).use { it.readText() }
                val importModel = Json.decodeFromString<CourseImportExport.CourseTableImportModel>(jsonString)
                courseConversionRepository.importCourseTableFromJson(tableId, importModel)

                _events.send(ConversionEvent.ShowMessage("课表导入成功！"))
            } catch (e: Exception) {
                Log.e("CourseTableConversionViewModel", "导入失败：${e.message}", e)
                _events.send(ConversionEvent.ShowMessage("导入失败：${e.message}"))
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun handleIcsExport(tableId: String, outputStream: OutputStream, alarmMinutes: Int?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val icsContent = courseConversionRepository.exportToIcsString(tableId, alarmMinutes)
                if (icsContent != null) {
                    outputStream.bufferedWriter(Charset.forName("UTF-8")).use { writer ->
                        writer.write(icsContent)
                    }
                    _events.send(ConversionEvent.ShowMessage("日历文件导出成功！"))
                } else {
                    _events.send(ConversionEvent.ShowMessage("日历文件导出失败：获取数据失败"))
                }
            } catch (e: Exception) {
                Log.e("CourseTableConversionViewModel", "日历文件导出失败：${e.message}", e)
                _events.send(ConversionEvent.ShowMessage("日历文件导出失败：${e.message}"))
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}

data class ConversionUiState(
    val isLoading: Boolean = false,
    val showImportTableDialog: Boolean = false,
    val showExportTableDialog: Boolean = false,
    val exportType: ExportType = ExportType.NONE
)

enum class ExportType {
    NONE,
    JSON,
    ICS
}

sealed class ConversionEvent {
    data class LaunchImportFilePicker(val tableId: String) : ConversionEvent()
    data class LaunchExportFileCreator(val jsonContent: String) : ConversionEvent()
    data class LaunchExportIcsFileCreator(val tableId: String, val alarmMinutes: Int?) : ConversionEvent()
    data class ShowMessage(val message: String) : ConversionEvent()
}

object CourseTableConversionViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
        if (modelClass.isAssignableFrom(CourseTableConversionViewModel::class.java)) {
            val app = application as MyApplication
            @Suppress("UNCHECKED_CAST")
            return CourseTableConversionViewModel(
                app.courseConversionRepository,
                app.courseTableRepository,
                app.appSettingsRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}