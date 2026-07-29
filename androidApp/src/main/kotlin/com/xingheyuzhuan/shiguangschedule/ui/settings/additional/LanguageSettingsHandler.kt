package com.xingheyuzhuan.shiguangschedule.ui.settings.additional

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import com.xingheyuzhuan.shiguangschedule.R

/**
 * 外部调用的处理函数
 */
fun handleLanguageSettingClick(
    context: Context,
    onShowDialog: () -> Unit
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // Android 13+: 直接跳转系统应用语言设置
        try {
            val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                data = "package:${context.packageName}".toUri()
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // 预防万一跳转失败（某些精简版 ROM），回退到应用内对话框
            onShowDialog()
        }
    } else {
        // Android 13 以下：显示应用内对话框
        onShowDialog()
    }
}

/**
 * 语言选择对话框组件
 */
@Composable
fun LanguageSelectionDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit
) {
    if (!showDialog) return

    val currentLanguageTag = remember {
        AppCompatDelegate.getApplicationLocales().toLanguageTags()
    }
    val context = LocalContext.current

    val supportedLocales = listOf(
        Triple(stringResource(R.string.language_follow_system), "", Icons.Default.Update),
        Triple("简体中文", "zh-CN", Icons.Default.Language),
        Triple("繁體中文", "zh-TW", Icons.Default.Language),
        Triple("English", "en", Icons.Default.Language)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.item_language_settings)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                supportedLocales.forEach { (name, tag, _) ->
                    val isSelected = if (tag.isEmpty()) {
                        currentLanguageTag.isEmpty()
                    } else {
                        currentLanguageTag.startsWith(tag)
                    }

                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val locales = if (tag.isEmpty()) {
                                    LocaleListCompat.getEmptyLocaleList()
                                } else {
                                    LocaleListCompat.forLanguageTags(tag)
                                }

                                AppCompatDelegate.setApplicationLocales(locales)

                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                    (context as? android.app.Activity)?.recreate()
                                }

                                onDismiss()
                            },
                        headlineContent = { Text(text = name) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            RadioButton(selected = isSelected, onClick = null)
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}