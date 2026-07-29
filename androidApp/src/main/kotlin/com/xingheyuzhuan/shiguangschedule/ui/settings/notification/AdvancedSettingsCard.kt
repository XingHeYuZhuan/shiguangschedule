package com.xingheyuzhuan.shiguangschedule.ui.settings.notification

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.R

@Composable
fun AdvancedSettingsCard(
    uiState: NotificationSettingsUiState,
    onUpdateHolidays: () -> Unit,
    onClearSkippedDates: () -> Unit,
    onViewSkippedDates: () -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.section_title_advanced),
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(8.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.section_title_skip_dates),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.text_skip_dates_experimental),
                    style = MaterialTheme.typography.bodyMedium
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                SettingItemRow(
                    title = stringResource(R.string.item_update_holiday_info),
                    onClick = onUpdateHolidays,
                    trailing = {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(26.dp),
                                strokeWidth = 4.dp
                            )
                        }
                    }
                )
                Text(
                    text = stringResource(R.string.update_holiday_info_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 16.dp)
                )
                HorizontalDivider()

                SettingItemRow(
                    title = stringResource(R.string.item_clear_skipped_dates),
                    onClick = onClearSkippedDates
                )
                HorizontalDivider()

                SettingItemRow(
                    title = stringResource(R.string.item_view_skipped_dates),
                    currentValue = if (uiState.skippedDates.isNotEmpty()) {
                        stringResource(R.string.skipped_dates_count_format, uiState.skippedDates.size)
                    } else {
                        stringResource(R.string.skipped_dates_none)
                    },
                    onClick = onViewSkippedDates
                )
            }
        }
    }
}