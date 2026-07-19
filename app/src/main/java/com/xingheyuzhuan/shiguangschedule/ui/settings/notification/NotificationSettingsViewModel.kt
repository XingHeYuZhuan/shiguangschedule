package com.xingheyuzhuan.shiguangschedule.ui.settings.notification

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.data.api.date.ApiDateImporter
import com.xingheyuzhuan.shiguangschedule.data.model.AutoControlMode
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

/**
 * 定义弹窗类型枚举/密封类
 */
sealed class NotificationDialogType {
    object None : NotificationDialogType()
    object EditRemindMinutes : NotificationDialogType()
    object AutoModeSelection : NotificationDialogType()
    object ExactAlarmPermission : NotificationDialogType()
    object DndPermission : NotificationDialogType()
    object ClearConfirmation : NotificationDialogType()
    object ViewSkippedDates : NotificationDialogType()
}

data class NotificationSettingsUiState(
    val reminderEnabled: Boolean = false,
    val remindBeforeMinutes: Int = 15,
    val skippedDates: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val exactAlarmStatus: Boolean = false,
    val dndPermissionStatus: Boolean = false,
    val autoModeEnabled: Boolean = false,
    val autoControlMode: AutoControlMode = AutoControlMode.DND,
    val activeDialog: NotificationDialogType = NotificationDialogType.None, // 当前激活的弹窗
    val compatWearableSync: Boolean = false
)

@KoinViewModel
class NotificationSettingsViewModel(
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            appSettingsRepository.getAppSettings().collect { settings ->
                _uiState.value = _uiState.value.copy(
                    reminderEnabled = settings.reminderEnabled,
                    remindBeforeMinutes = settings.remindBeforeMinutes,
                    skippedDates = settings.skippedDates,
                    autoModeEnabled = settings.autoModeEnabled,
                    autoControlMode = settings.autoControlMode,
                    compatWearableSync = settings.compatWearableSync
                )
            }
        }
    }

    // --- 弹窗管理 ---
    fun showDialog(type: NotificationDialogType) {
        _uiState.value = _uiState.value.copy(activeDialog = type)
    }

    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(activeDialog = NotificationDialogType.None)
    }

    // --- 状态更新 ---
    fun updateExactAlarmStatus(hasPermission: Boolean) {
        _uiState.value = _uiState.value.copy(exactAlarmStatus = hasPermission)
    }

    fun updateDndPermissionStatus(hasPermission: Boolean) {
        _uiState.value = _uiState.value.copy(dndPermissionStatus = hasPermission)
    }

    // --- 业务逻辑 ---

    // 提醒开关切换逻辑
    fun onReminderEnabledChange(
        isEnabled: Boolean,
        triggerNotificationWorker: (Context) -> Unit,
        context: Context
    ) {
        viewModelScope.launch {
            // 如果开启提醒但没有精确闹钟权限，则拦截并弹窗
            if (isEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !_uiState.value.exactAlarmStatus) {
                showDialog(NotificationDialogType.ExactAlarmPermission)
                val currentSettings = appSettingsRepository.getAppSettings().first()
                _uiState.value = _uiState.value.copy(reminderEnabled = currentSettings.reminderEnabled)
                return@launch
            }

            val currentSettings = appSettingsRepository.getAppSettings().first()
            appSettingsRepository.insertOrUpdateAppSettings(currentSettings.copy(reminderEnabled = isEnabled))

            triggerNotificationWorker(context)
        }
    }

    /**
     * 兼容穿戴设备同步通知开关切换逻辑
     */
    fun onCompatWearableSyncChange(
        isEnabled: Boolean,
        triggerNotificationWorker: (Context) -> Unit,
        context: Context
    ) {
        viewModelScope.launch {
            val currentSettings = appSettingsRepository.getAppSettings().first()
            appSettingsRepository.insertOrUpdateAppSettings(
                currentSettings.copy(compatWearableSync = isEnabled)
            )
            triggerNotificationWorker(context)
        }
    }

    // 保存提醒时间
    fun onSaveRemindBeforeMinutes(
        minutes: Int,
        triggerNotificationWorker: (Context) -> Unit,
        context: Context
    ) {
        viewModelScope.launch {
            val currentSettings = appSettingsRepository.getAppSettings().first()
            appSettingsRepository.insertOrUpdateAppSettings(currentSettings.copy(remindBeforeMinutes = minutes))
            triggerNotificationWorker(context)
            dismissDialog()
        }
    }

    // 自动模式（勿扰/静音）状态改变
    fun onAutoModeStateChange(
        isEnabled: Boolean,
        newControlMode: AutoControlMode,
        triggerDndWorker: (Context) -> Unit,
        context: Context
    ) {
        viewModelScope.launch {
            if (isEnabled && !_uiState.value.dndPermissionStatus) {
                showDialog(NotificationDialogType.DndPermission)
                return@launch
            }

            val currentSettings = appSettingsRepository.getAppSettings().first()
            appSettingsRepository.insertOrUpdateAppSettings(
                currentSettings.copy(
                    autoModeEnabled = isEnabled,
                    autoControlMode = newControlMode
                )
            )
            triggerDndWorker(context)
            dismissDialog()
        }
    }

    // 更新节假日信息
    fun onUpdateHolidays(
        onSuccess: (Context) -> Unit,
        onFailure: (Context, String) -> Unit,
        context: Context
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                withContext(Dispatchers.IO) {
                    ApiDateImporter.importAndSaveSkippedDates(appSettingsRepository)
                }
                onSuccess(context)
            } catch (e: Exception) {
                onFailure(context, e.message ?: "未知错误")
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    // 清除跳过的日期
    fun onClearSkippedDates(
        onSuccess: (Context) -> Unit,
        onFailure: (Context, String) -> Unit,
        context: Context
    ) {
        viewModelScope.launch {
            try {
                val currentSettings = appSettingsRepository.getAppSettings().first()
                appSettingsRepository.insertOrUpdateAppSettings(currentSettings.copy(skippedDates = emptySet()))
                onSuccess(context)
                dismissDialog()
            } catch (e: Exception) {
                onFailure(context, e.message ?: "未知错误")
            }
        }
    }
}