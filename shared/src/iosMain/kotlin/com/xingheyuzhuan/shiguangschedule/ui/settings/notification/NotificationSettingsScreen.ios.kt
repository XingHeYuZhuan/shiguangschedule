package com.xingheyuzhuan.shiguangschedule.ui.settings.notification

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformGeneralSettingsSection(uiState: NotificationSettingsUiState, viewModel: NotificationSettingsViewModel) {
    IosGeneralSettingsCard(
        uiState = uiState,
        onReminderToggle = viewModel::updateReminderEnabled,
        onRemindTimeClick = { viewModel.showDialog(NotificationDialogType.EditRemindMinutes) }
    )
}

@Composable
actual fun PlatformNotificationDialogDispatcher(uiState: NotificationSettingsUiState, viewModel: NotificationSettingsViewModel) {
    IosNotificationDialogs(uiState, viewModel)
}
