package com.xingheyuzhuan.shiguangschedule.tool

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import androidx.core.graphics.toColorInt
import com.xingheyuzhuan.shiguangschedule.R

/**
 * 系统日历账户管理助手
 */
object CalendarAccountManager {

    /** 账户类型：本地账户 (Local) */
    private const val ACCOUNT_TYPE = CalendarContract.ACCOUNT_TYPE_LOCAL

    /**
     * 获取或创建专属日历 ID
     * * @param context 上下文
     * @return 专属日历的 ID；若失败则返回 -1
     */
    fun getOrCreateCalendarId(context: Context): Long {
        val contentResolver = context.contentResolver

        val accountName = "${context.packageName}.account"

        // 查询阶段：检查该包名对应的账户是否已存在
        val projection = arrayOf(Calendars._ID)
        val selection = "${Calendars.ACCOUNT_NAME} = ? AND ${Calendars.ACCOUNT_TYPE} = ?"
        val selectionArgs = arrayOf(accountName, ACCOUNT_TYPE)

        val cursor = contentResolver.query(
            Calendars.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                return it.getLong(0) // 已存在则直接返回 ID
            }
        }

        // 创建阶段：如果账户不存在，则新建一个
        val calendarDisplayName = context.getString(R.string.app_name)

        val values = ContentValues().apply {
            // --- 账户身份信息 ---
            put(Calendars.ACCOUNT_NAME, accountName)
            put(Calendars.ACCOUNT_TYPE, ACCOUNT_TYPE)
            put(Calendars.NAME, accountName)

            // --- 视觉展示信息 ---
            put(Calendars.CALENDAR_DISPLAY_NAME, calendarDisplayName)
            put(Calendars.CALENDAR_COLOR, "#4285F4".toColorInt())

            // --- 权限与状态设置 ---
            put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_OWNER)
            put(Calendars.OWNER_ACCOUNT, accountName)
            put(Calendars.VISIBLE, 1)
            put(Calendars.SYNC_EVENTS, 1)
            put(Calendars.CALENDAR_TIME_ZONE, java.util.TimeZone.getDefault().id)
            put(Calendars.CAN_ORGANIZER_RESPOND, 1)
        }

        val uri = Calendars.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(Calendars.ACCOUNT_NAME, accountName)
            .appendQueryParameter(Calendars.ACCOUNT_TYPE, ACCOUNT_TYPE)
            .build()

        return try {
            val resultUri = contentResolver.insert(uri, values)
            resultUri?.let { ContentUris.parseId(it) } ?: -1L
        } catch (e: Exception) {
            e.printStackTrace()
            -1L
        }
    }
}