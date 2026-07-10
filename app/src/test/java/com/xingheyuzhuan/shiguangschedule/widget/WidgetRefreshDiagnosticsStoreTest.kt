package com.xingheyuzhuan.shiguangschedule.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class WidgetRefreshDiagnosticsStoreTest {

    @Test
    fun encodeAndDecode_preservesRefreshLogEntry() {
        val entry = WidgetRefreshLogEntry(
            timestampMillis = 1783340000000L,
            reason = WidgetRefreshReason.COURSE_TABLE_CONFIG_CHANGED,
            success = false,
            durationMillis = 234L,
            message = "周末显示设置同步失败：launcher cache"
        )

        val decoded = WidgetRefreshDiagnosticsStore.decode(
            WidgetRefreshDiagnosticsStore.encode(entry)
        )

        assertNotNull(decoded)
        assertEquals(entry, decoded)
    }

    @Test
    fun decode_whenReasonUnknown_fallsBackToUnknown() {
        val line = "1783340000000\tNOT_EXIST\ttrue\t12\t5Yi35paw5a6M5oiQ"

        val decoded = WidgetRefreshDiagnosticsStore.decode(line)

        assertNotNull(decoded)
        assertEquals(WidgetRefreshReason.UNKNOWN, decoded?.reason)
    }
}
