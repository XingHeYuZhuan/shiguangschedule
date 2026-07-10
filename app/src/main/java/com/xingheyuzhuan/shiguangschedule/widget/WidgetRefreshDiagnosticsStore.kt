package com.xingheyuzhuan.shiguangschedule.widget

import android.content.Context
import android.util.Log
import java.nio.charset.StandardCharsets
import java.util.Base64

/** 单次桌面小组件刷新诊断记录。 */
data class WidgetRefreshLogEntry(
    val timestampMillis: Long,
    val reason: WidgetRefreshReason,
    val success: Boolean,
    val durationMillis: Long,
    val message: String
)

/**
 * 桌面小组件刷新诊断日志。
 *
 * 使用 SharedPreferences 保存最近若干条记录，避免引入 Room schema 迁移；
 * 进程被杀后仍能保留最近刷新状态，方便无人值守排障。
 */
object WidgetRefreshDiagnosticsStore {
    private const val TAG = "WidgetRefreshDiag"
    private const val PREF_NAME = "widget_refresh_diagnostics"
    private const val KEY_RECENT_RECORDS = "recent_records"
    private const val MAX_RECORDS = 30
    private const val DELIMITER = "\t"

    fun append(context: Context, entry: WidgetRefreshLogEntry) {
        synchronized(this) {
            val records = readRecent(context).toMutableList()
            records.add(0, entry)
            val encoded = records.take(MAX_RECORDS).joinToString("\n") { encode(it) }
            context.applicationContext
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_RECENT_RECORDS, encoded)
                .apply()
        }
    }

    fun readRecent(context: Context): List<WidgetRefreshLogEntry> {
        val raw = context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_RECENT_RECORDS, null)
            .orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.lineSequence().mapNotNull { decode(it) }.toList()
    }

    fun latest(context: Context): WidgetRefreshLogEntry? = readRecent(context).firstOrNull()

    internal fun encode(entry: WidgetRefreshLogEntry): String {
        val encodedMessage = Base64.getEncoder().encodeToString(entry.message.toByteArray(StandardCharsets.UTF_8))
        return listOf(
            entry.timestampMillis.toString(),
            entry.reason.name,
            entry.success.toString(),
            entry.durationMillis.toString(),
            encodedMessage
        ).joinToString(DELIMITER)
    }

    internal fun decode(line: String): WidgetRefreshLogEntry? = runCatching {
        val parts = line.split(DELIMITER, limit = 5)
        if (parts.size != 5) return@runCatching null
        WidgetRefreshLogEntry(
            timestampMillis = parts[0].toLong(),
            reason = WidgetRefreshReason.fromName(parts[1]),
            success = parts[2].toBooleanStrict(),
            durationMillis = parts[3].toLong(),
            message = String(Base64.getDecoder().decode(parts[4]), StandardCharsets.UTF_8)
        )
    }.onFailure { error ->
        Log.w(TAG, "解析小组件刷新诊断记录失败: $line", error)
    }.getOrNull()
}
