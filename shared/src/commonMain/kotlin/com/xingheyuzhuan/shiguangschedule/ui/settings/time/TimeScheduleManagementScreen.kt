package com.xingheyuzhuan.shiguangschedule.ui.settings.time

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_add_schedule_scheme
import shiguangschedule.shared.generated.resources.a11y_back
import shiguangschedule.shared.generated.resources.a11y_cancel_selection
import shiguangschedule.shared.generated.resources.a11y_delete
import shiguangschedule.shared.generated.resources.a11y_delete_selected
import shiguangschedule.shared.generated.resources.a11y_edit_schedule
import shiguangschedule.shared.generated.resources.action_add_combo_schedule
import shiguangschedule.shared.generated.resources.action_add_public_schedule
import shiguangschedule.shared.generated.resources.action_cancel
import shiguangschedule.shared.generated.resources.action_copy
import shiguangschedule.shared.generated.resources.add_24px
import shiguangschedule.shared.generated.resources.arrow_back_24px
import shiguangschedule.shared.generated.resources.confirm_delete
import shiguangschedule.shared.generated.resources.content_copy_24px
import shiguangschedule.shared.generated.resources.delete_24px
import shiguangschedule.shared.generated.resources.dialog_msg_confirm_delete_schedules
import shiguangschedule.shared.generated.resources.edit_24px
import shiguangschedule.shared.generated.resources.follow_course_table
import shiguangschedule.shared.generated.resources.schedule_type_combo
import shiguangschedule.shared.generated.resources.schedule_type_exclusive
import shiguangschedule.shared.generated.resources.schedule_type_public
import shiguangschedule.shared.generated.resources.title_exclusive_schedule
import shiguangschedule.shared.generated.resources.title_schedule_management
import shiguangschedule.shared.generated.resources.title_selected_items_count

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeScheduleManagementScreen(
    onBack: () -> Unit,
    onEditSingleSchedule: (tableId: String?, isPublic: Boolean, copyFromId: String?) -> Unit,
    onEditComboSchedule: (comboId: String?, copyFromId: String?) -> Unit,
    viewModel: TimeScheduleManagementViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 多选模式状态
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedKeys = remember { mutableStateListOf<String>() }

    var showAddMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // 当退出多选模式时清空已选
    fun exitSelectionMode() {
        isSelectionMode = false
        selectedKeys.clear()
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                // 多选模式下的 TopAppBar
                TopAppBar(
                    title = {
                        Text(
                            stringResource(
                                Res.string.title_selected_items_count,
                                selectedKeys.size
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { exitSelectionMode() }) {
                            Icon(
                                imageVector = vectorResource(Res.drawable.arrow_back_24px),
                                contentDescription = stringResource(Res.string.a11y_cancel_selection)
                            )
                        }
                    },
                    actions = {
                        // 右上角批量删除入口
                        IconButton(
                            enabled = selectedKeys.isNotEmpty(),
                            onClick = { showDeleteConfirmDialog = true }
                        ) {
                            Icon(
                                imageVector = vectorResource(Res.drawable.delete_24px),
                                contentDescription = stringResource(Res.string.a11y_delete_selected),
                                tint = if (selectedKeys.isNotEmpty()) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                    }
                )
            } else {
                // 常规模式下的 TopAppBar
                TopAppBar(
                    title = { Text(stringResource(Res.string.title_schedule_management)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = vectorResource(Res.drawable.arrow_back_24px),
                                contentDescription = stringResource(Res.string.a11y_back)
                            )
                        }
                    },
                    actions = {
                        Box {
                            IconButton(onClick = { showAddMenu = true }) {
                                Icon(
                                    imageVector = vectorResource(Res.drawable.add_24px),
                                    contentDescription = stringResource(Res.string.a11y_add_schedule_scheme)
                                )
                            }
                            DropdownMenu(
                                expanded = showAddMenu,
                                onDismissRequest = { showAddMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.action_add_public_schedule)) },
                                    onClick = {
                                        showAddMenu = false
                                        onEditSingleSchedule(null, true, null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.action_add_combo_schedule)) },
                                    onClick = {
                                        showAddMenu = false
                                        onEditComboSchedule(null, null)
                                    }
                                )
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        if (!uiState.isDataLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = uiState.items,
                key = { "${it.type.name}_${it.id}" }
            ) { item ->
                val itemKey = "${item.type.name}_${item.id}"
                val isItemMultiSelected = selectedKeys.contains(itemKey)

                ScheduleCard(
                    item = item,
                    isSelectionMode = isSelectionMode,
                    isMultiSelected = isItemMultiSelected,
                    onSelect = {
                        if (isSelectionMode) {
                            // 课表专属作息不允许删除/多选
                            if (item.type == ScheduleType.EXCLUSIVE) return@ScheduleCard

                            if (isItemMultiSelected) {
                                selectedKeys.remove(itemKey)
                                if (selectedKeys.isEmpty()) isSelectionMode = false
                            } else {
                                selectedKeys.add(itemKey)
                            }
                        } else {
                            viewModel.bindTimeSchedule(item)
                        }
                    },
                    onLongClick = {
                        // 专属作息不开启多选模式
                        if (item.type != ScheduleType.EXCLUSIVE) {
                            isSelectionMode = true
                            if (!selectedKeys.contains(itemKey)) {
                                selectedKeys.add(itemKey)
                            }
                        }
                    },
                    onEdit = {
                        // 普通编辑模式：传入原本的 id，copyFromId 为 null
                        when (item.type) {
                            ScheduleType.EXCLUSIVE -> onEditSingleSchedule(item.id, false, null)
                            ScheduleType.PUBLIC -> onEditSingleSchedule(item.id, true, null)
                            ScheduleType.COMBO -> onEditComboSchedule(item.id, null)
                        }
                    },
                    onCopy = {
                        // 复制模式（走新增逻辑）：tableId/comboId 传 null，把当前 item.id 作为 copyFromId 传入
                        when (item.type) {
                            ScheduleType.PUBLIC -> onEditSingleSchedule(null, true, item.id)
                            ScheduleType.COMBO -> onEditComboSchedule(null, item.id)
                            ScheduleType.EXCLUSIVE -> {} // 专属作息无复制按钮
                        }
                    }
                )
            }
        }

        // 删除确认弹窗
        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text(stringResource(Res.string.confirm_delete)) },
                text = {
                    Text(
                        stringResource(
                            Res.string.dialog_msg_confirm_delete_schedules,
                            selectedKeys.size
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmDialog = false
                            viewModel.deleteSchedules(selectedKeys.toSet())
                            exitSelectionMode()
                        }
                    ) {
                        Text(
                            stringResource(Res.string.a11y_delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text(stringResource(Res.string.action_cancel))
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScheduleCard(
    item: TimeScheduleItemUiModel,
    isSelectionMode: Boolean,
    isMultiSelected: Boolean,
    onSelect: () -> Unit,
    onLongClick: () -> Unit,
    onEdit: () -> Unit,
    onCopy: () -> Unit
) {
    val isExclusive = item.type == ScheduleType.EXCLUSIVE

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isSelectionMode && isExclusive) 0.5f else 1.0f)
            .combinedClickable(
                onClick = {
                    if (isSelectionMode && isExclusive) return@combinedClickable
                    onSelect()
                },
                onLongClick = {
                    if (isExclusive) return@combinedClickable
                    onLongClick()
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (isSelectionMode && isMultiSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSelectionMode) {
                    val toggleableState = when {
                        isExclusive -> ToggleableState.Indeterminate
                        isMultiSelected -> ToggleableState.On
                        else -> ToggleableState.Off
                    }

                    TriStateCheckbox(
                        state = toggleableState,
                        onClick = if (isExclusive) null else { { onSelect() } },
                        enabled = !isExclusive
                    )
                } else {
                    RadioButton(
                        selected = item.isSelected,
                        onClick = onSelect
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = item.name ?: stringResource(Res.string.title_exclusive_schedule),
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScheduleTypeTag(type = item.type)

                    Text(
                        text = item.createdAtFormatted ?: stringResource(Res.string.follow_course_table),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(
                visible = !isSelectionMode && item.isEditable,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 非专属作息（公共作息、组合作息）在编辑按钮左侧展示复制按钮
                    if (!isExclusive) {
                        IconButton(onClick = onCopy) {
                            Icon(
                                imageVector = vectorResource(Res.drawable.content_copy_24px),
                                contentDescription = stringResource(Res.string.action_copy),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.edit_24px),
                            contentDescription = stringResource(Res.string.a11y_edit_schedule),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleTypeTag(type: ScheduleType) {
    val (textRes, containerColor, contentColor) = when (type) {
        ScheduleType.EXCLUSIVE -> Triple(
            Res.string.schedule_type_exclusive,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        ScheduleType.PUBLIC -> Triple(
            Res.string.schedule_type_public,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        ScheduleType.COMBO -> Triple(
            Res.string.schedule_type_combo,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
    }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}