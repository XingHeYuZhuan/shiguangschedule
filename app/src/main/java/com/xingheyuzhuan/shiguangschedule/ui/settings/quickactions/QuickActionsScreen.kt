package com.xingheyuzhuan.shiguangschedule.ui.settings.quickactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.Screen
import com.xingheyuzhuan.shiguangschedule.ui.settings.SectionTitle
import com.xingheyuzhuan.shiguangschedule.ui.settings.SettingItemCard
import com.xingheyuzhuan.shiguangschedule.ui.settings.SettingsSectionContainer

/**
 * 快捷操作二级页面
 * 用于收纳高频使用的工具类功能，如调课、临时改动等
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionsScreen(
    navController: NavHostController
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.item_quick_actions)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.a11y_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 16.dp, bottom = 16.dp)
        ) {
            item {
                SectionTitle(stringResource(R.string.label_quick_action_category_schedule))
                SettingsSectionContainer {
                    // 调课功能项
                    SettingItemCard(isFirst = true, isLast = false) {
                        QuickActionItem(
                            title = stringResource(R.string.item_schedule_tweak),
                            subtitle = stringResource(R.string.desc_schedule_tweak),
                            onClick = { navController.navigate(Screen.TweakSchedule.route) }
                        )
                    }

                    // 2. 快速删除功能项
                    SettingItemCard(isFirst = false, isLast = true) {
                        QuickActionItem(
                            title = stringResource(R.string.item_quick_delete),
                            subtitle = stringResource(R.string.quick_delete_subtitle),
                            onClick = { navController.navigate(Screen.QuickDelete.route) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 快捷操作单项组件
 * 布局复用设置页逻辑：左侧标题+描述，右侧导航箭头
 */
@Composable
private fun QuickActionItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            modifier = Modifier.padding(start = 4.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}