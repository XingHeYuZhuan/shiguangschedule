// NotificationSettingsViewModel.kt

package com.xingheyuzhuan.shiguangschedule.ui.settings.notification

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.MyApplication
import com.xingheyuzhuan.shiguangschedule.data.ApiDateImporter
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.service.CourseAlarmReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 通知设置界面的UI状态数据类
 */
data class NotificationSettingsUiState(
    val reminderEnabled: Boolean = false,           // 是否启用提醒
    val remindBeforeMinutes: Int = 15,             // 提前提醒分钟数
    val skippedDates: Set<String> = emptySet(),    // 跳过的日期集合
    val isLoading: Boolean = false,                // 加载状态
    val exactAlarmStatus: Boolean = false,          // 精确闹钟权限状态
    val dndPermissionStatus: Boolean = false,       // 勿扰模式权限状态
    val autoModeEnabled: Boolean = false,          // 是否启用上课自动模式
    val autoControlMode: String = CourseAlarmReceiver.MODE_DND // 模式类型：DND 或 SILENT
)

/**
 * 通知设置ViewModel，管理通知相关的业务逻辑
 */
class NotificationSettingsViewModel(
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState

    init {
        // 初始化时监听设置变化并更新UI状态
        viewModelScope.launch {
            appSettingsRepository.getAppSettings().collect { settings ->
                _uiState.value = _uiState.value.copy(
                    reminderEnabled = settings.reminderEnabled,
                    remindBeforeMinutes = settings.remindBeforeMinutes,
                    skippedDates = settings.skippedDates ?: emptySet(),
                    autoModeEnabled = settings.autoModeEnabled,
                    autoControlMode = settings.autoControlMode
                )
            }
        }
    }

    // 更新精确闹钟权限状态
    fun updateExactAlarmStatus(status: Boolean) {
        _uiState.value = _uiState.value.copy(exactAlarmStatus = status)
    }

    fun updateDndPermissionStatus(status: Boolean) {
        _uiState.value = _uiState.value.copy(dndPermissionStatus = status)
    }

    // 处理提醒开关变化
    fun onReminderEnabledChange(isEnabled: Boolean, hasExactAlarmPermission: Boolean, triggerWorker: (Context) -> Unit, onNoPermission: () -> Unit, context: Context) {
        viewModelScope.launch {
            if (isEnabled && !hasExactAlarmPermission) {
                onNoPermission()
                return@launch
            }

            val currentSettings = appSettingsRepository.getAppSettings().first()
            val updatedSettings = currentSettings.copy(
                reminderEnabled = isEnabled,
                autoModeEnabled = if (!isEnabled) false else currentSettings.autoModeEnabled
            )
            appSettingsRepository.insertOrUpdateAppSettings(updatedSettings)
            triggerWorker(context)
        }
    }

    /**
     * 处理自动模式状态和模式类型的合并变化
     */
    fun onAutoModeStateChange(isEnabled: Boolean, newControlMode: String, triggerDndWorker: (Context) -> Unit, context: Context) {
        viewModelScope.launch {
            val currentSettings = appSettingsRepository.getAppSettings().first()

            // 只有当选择关闭模式时，才将 isEnabled 设为 false，否则根据 newControlMode 设置 isEnabled 和 autoControlMode
            val finalModeEnabled = isEnabled
            val finalControlMode = if (isEnabled) newControlMode else currentSettings.autoControlMode

            val updatedSettings = currentSettings.copy(
                autoModeEnabled = finalModeEnabled,
                autoControlMode = finalControlMode
            )
            appSettingsRepository.insertOrUpdateAppSettings(updatedSettings)

            // 无论开启、关闭或更改模式类型，都需要重新调度 Worker
            triggerDndWorker(context)
        }
    }
    // 保存提前提醒分钟数
    fun onSaveRemindBeforeMinutes(minutes: Int, triggerWorker: (Context) -> Unit, context: Context) {
        viewModelScope.launch {
            val currentSettings = appSettingsRepository.getAppSettings().first()
            val updatedSettings = currentSettings.copy(remindBeforeMinutes = minutes)
            appSettingsRepository.insertOrUpdateAppSettings(updatedSettings)
            triggerWorker(context)
        }
    }

    fun onUpdateHolidays(onSuccess: (Context) -> Unit, onFailure: (Context, String) -> Unit, context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                withContext(Dispatchers.IO) {
                    ApiDateImporter.importAndSaveSkippedDates(appSettingsRepository)
                }
                val newSkippedDates = appSettingsRepository.getAppSettings().first().skippedDates ?: emptySet()
                _uiState.value = _uiState.value.copy(skippedDates = newSkippedDates)
                onSuccess(context)
            } catch (e: Exception) {
                onFailure(context, e.message ?: "未知错误")
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    // 清除跳过的日期
    fun onClearSkippedDates(onSuccess: (Context) -> Unit, onFailure: (Context, String) -> Unit, context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                withContext(viewModelScope.coroutineContext) {
                    val currentSettings = appSettingsRepository.getAppSettings().first()
                    appSettingsRepository.insertOrUpdateAppSettings(currentSettings.copy(skippedDates = emptySet()))
                }
                _uiState.value = _uiState.value.copy(skippedDates = emptySet())
                onSuccess(context)
            } catch (e: Exception) {
                onFailure(context, e.message ?: "未知错误")
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    companion object {
        // 提供ViewModel工厂方法
        fun provideFactory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(NotificationSettingsViewModel::class.java)) {
                        return NotificationSettingsViewModel((application as MyApplication).appSettingsRepository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}