package com.xingheyuzhuan.shiguangschedule.ui.settings.time

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeTable
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeTableComboRule
import com.xingheyuzhuan.shiguangschedule.ui.components.ToastManager
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_back
import shiguangschedule.shared.generated.resources.a11y_delete_rule
import shiguangschedule.shared.generated.resources.a11y_save
import shiguangschedule.shared.generated.resources.action_add_time_range_rule
import shiguangschedule.shared.generated.resources.action_cancel
import shiguangschedule.shared.generated.resources.action_confirm
import shiguangschedule.shared.generated.resources.add_24px
import shiguangschedule.shared.generated.resources.arrow_back_24px
import shiguangschedule.shared.generated.resources.delete_24px
import shiguangschedule.shared.generated.resources.dialog_title_select_effective_date_range
import shiguangschedule.shared.generated.resources.error_name_cannot_be_empty
import shiguangschedule.shared.generated.resources.error_rule_date_overlap
import shiguangschedule.shared.generated.resources.label_applied_schedule
import shiguangschedule.shared.generated.resources.label_base_schedule
import shiguangschedule.shared.generated.resources.label_combo_schedule_name
import shiguangschedule.shared.generated.resources.label_effective_date_range
import shiguangschedule.shared.generated.resources.msg_please_create_public_schedule_first
import shiguangschedule.shared.generated.resources.option_exclusive_schedule
import shiguangschedule.shared.generated.resources.option_select_base_schedule
import shiguangschedule.shared.generated.resources.option_select_schedule_table
import shiguangschedule.shared.generated.resources.option_unnamed_schedule
import shiguangschedule.shared.generated.resources.save_24px
import shiguangschedule.shared.generated.resources.title_add_combo_schedule
import shiguangschedule.shared.generated.resources.title_edit_combo_schedule
import shiguangschedule.shared.generated.resources.title_rule_list_by_date
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * 组合作息编辑/创建界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComboScheduleEditScreen(
    comboId: String? = null,
    copyFromId: String? = null,
    onBack: () -> Unit
) {
    val viewModel: ComboScheduleEditViewModel = koinViewModel(
        parameters = {
            parametersOf(
                comboId ?: "",
                copyFromId ?: ""
            )
        }
    )
    val uiState by viewModel.uiState.collectAsState()

    // 校验规则是否存在重叠
    val hasOverlapError = remember(uiState.rules) {
        checkHasAnyOverlap(uiState.rules)
    }

    // 校验名称是否非空
    val isNameValid = uiState.name.isNotBlank()

    // 获取无公共作息时的提示文案
    val noPublicTablesMsg = stringResource(Res.string.msg_please_create_public_schedule_first)

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onBack()
        }
    }

    LaunchedEffect(uiState.errorMsg) {
        val msg = uiState.errorMsg
        if (!msg.isNullOrBlank()) {
            ToastManager.show(msg)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (comboId == null) stringResource(Res.string.title_add_combo_schedule)
                        else stringResource(Res.string.title_edit_combo_schedule)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.arrow_back_24px),
                            contentDescription = stringResource(Res.string.a11y_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.save() },
                        enabled = !hasOverlapError && isNameValid
                    ) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.save_24px),
                            contentDescription = stringResource(Res.string.a11y_save)
                        )
                    }
                }
            )
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 基本信息设置
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = viewModel::onNameChange,
                        label = { Text(stringResource(Res.string.label_combo_schedule_name)) },
                        placeholder = { Text(stringResource(Res.string.option_unnamed_schedule)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = !isNameValid,
                        supportingText = if (!isNameValid) {
                            {
                                Text(
                                    text = stringResource(Res.string.error_name_cannot_be_empty),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        } else null
                    )

                    BaseTableDropdown(
                        selectedBaseId = uiState.baseTimeTableId,
                        availableTables = uiState.availablePublicTables,
                        onBaseSelected = { selectedId -> viewModel.onBaseTableChange(selectedId) }
                    )
                }
            }

            item {
                Text(
                    text = stringResource(Res.string.title_rule_list_by_date),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // 无公共作息时，在规则列表上方追加提示卡片
            if (uiState.availablePublicTables.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                        )
                    ) {
                        Text(
                            text = noPublicTablesMsg,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // 日期规则列表
            itemsIndexed(uiState.rules) { index, rule ->
                val isOverlap = checkSingleRuleOverlap(index, rule, uiState.rules)

                RuleEditCard(
                    rule = rule,
                    isOverlapError = isOverlap,
                    availableTables = uiState.availablePublicTables,
                    onUpdate = { updated -> viewModel.updateRule(index, updated) },
                    onDelete = { viewModel.removeRule(index) }
                )
            }

            // 添加规则按钮
            item {
                val hasPublicTables = uiState.availablePublicTables.isNotEmpty()

                OutlinedButton(
                    onClick = {
                        if (hasPublicTables) {
                            viewModel.addRule()
                        } else {
                            ToastManager.show(noPublicTablesMsg)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.add_24px),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(Res.string.action_add_time_range_rule))
                }
            }
        }
    }
}

/**
 * 基准作息选择下拉菜单
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BaseTableDropdown(
    selectedBaseId: String?,
    availableTables: List<TimeTable>,
    onBaseSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val currentTable = availableTables.find { it.id == selectedBaseId }
    val displayText = when {
        selectedBaseId == null -> stringResource(Res.string.option_exclusive_schedule)
        currentTable != null -> currentTable.name ?: stringResource(Res.string.option_unnamed_schedule)
        else -> stringResource(Res.string.option_select_base_schedule)
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(Res.string.label_base_schedule)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(
                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled = true
                )
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.option_exclusive_schedule)) },
                onClick = {
                    onBaseSelected(null)
                    expanded = false
                }
            )

            availableTables.forEach { table ->
                DropdownMenuItem(
                    text = { Text(table.name ?: stringResource(Res.string.option_unnamed_schedule)) },
                    onClick = {
                        onBaseSelected(table.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * 单条日期覆盖规则编辑卡片
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RuleEditCard(
    rule: TimeTableComboRule,
    isOverlapError: Boolean,
    availableTables: List<TimeTable>,
    onUpdate: (TimeTableComboRule) -> Unit,
    onDelete: () -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val currentTable = availableTables.find { it.id == rule.targetTimeTableId }
    val displayText = currentTable?.name ?: stringResource(Res.string.option_select_schedule_table)

    val dateRangePickerState = rememberDateRangePickerState()
    val isPublicTablesEmpty = availableTables.isEmpty()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isOverlapError) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = !dropdownExpanded }
            ) {
                OutlinedTextField(
                    value = displayText,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(Res.string.label_applied_schedule)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    isError = isPublicTablesEmpty,
                    supportingText = if (isPublicTablesEmpty) {
                        {
                            Text(
                                text = stringResource(Res.string.msg_please_create_public_schedule_first),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    } else null,
                    modifier = Modifier
                        .menuAnchor(
                            type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                            enabled = true
                        )
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    availableTables.forEach { table ->
                        DropdownMenuItem(
                            text = { Text(table.name ?: stringResource(Res.string.option_unnamed_schedule)) },
                            onClick = {
                                onUpdate(rule.copy(targetTimeTableId = table.id))
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // 日期范围显示与触发选择
            val currentDateStr = remember {
                Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
            }
            val startDateDisplay = rule.startDate.ifBlank { currentDateStr }
            val endDateDisplay = rule.endDate.ifBlank { currentDateStr }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            ) {
                OutlinedTextField(
                    value = "$startDateDisplay - $endDateDisplay",
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    isError = isOverlapError,
                    label = { Text(stringResource(Res.string.label_effective_date_range)) },
                    supportingText = if (isOverlapError) {
                        { Text(stringResource(Res.string.error_rule_date_overlap), color = MaterialTheme.colorScheme.error) }
                    } else null,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = if (isOverlapError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = if (isOverlapError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                        disabledLabelColor = if (isOverlapError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.delete_24px),
                        contentDescription = stringResource(Res.string.a11y_delete_rule),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val startMillis = dateRangePickerState.selectedStartDateMillis
                        val endMillis = dateRangePickerState.selectedEndDateMillis
                        if (startMillis != null && endMillis != null) {
                            val startDateStr = formatMillisToDateStr(startMillis)
                            val endDateStr = formatMillisToDateStr(endMillis)
                            onUpdate(rule.copy(startDate = startDateStr, endDate = endDateStr))
                        }
                        showDatePicker = false
                    },
                    enabled = dateRangePickerState.selectedStartDateMillis != null &&
                            dateRangePickerState.selectedEndDateMillis != null
                ) {
                    Text(stringResource(Res.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = {
                    Text(
                        text = stringResource(Res.string.dialog_title_select_effective_date_range),
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 检查单个规则与其他规则是否存在日期交叉
 */
private fun checkSingleRuleOverlap(
    currentIndex: Int,
    currentRule: TimeTableComboRule,
    allRules: List<TimeTableComboRule>
): Boolean {
    if (currentRule.startDate.blankOrInvalid() || currentRule.endDate.blankOrInvalid()) return false

    for (i in allRules.indices) {
        if (i == currentIndex) continue
        val otherRule = allRules[i]
        if (otherRule.startDate.blankOrInvalid() || otherRule.endDate.blankOrInvalid()) continue

        val maxStart = if (currentRule.startDate >= otherRule.startDate) currentRule.startDate else otherRule.startDate
        val minEnd = if (currentRule.endDate <= otherRule.endDate) currentRule.endDate else otherRule.endDate

        if (maxStart <= minEnd) {
            return true
        }
    }
    return false
}

/**
 * 检查当前所有规则中是否存在任意重叠
 */
private fun checkHasAnyOverlap(rules: List<TimeTableComboRule>): Boolean {
    for (i in rules.indices) {
        if (checkSingleRuleOverlap(i, rules[i], rules)) {
            return true
        }
    }
    return false
}

private fun String.blankOrInvalid(): Boolean = this.isBlank() || this.length < 10

private fun formatMillisToDateStr(millis: Long): String {
    val instant = Instant.fromEpochMilliseconds(millis)
    val localDate = instant.toLocalDateTime(TimeZone.UTC).date
    val year = localDate.year
    val month = localDate.month.number.toString().padStart(2, '0')
    val day = localDate.day.toString().padStart(2, '0')
    return "$year-$month-$day"
}