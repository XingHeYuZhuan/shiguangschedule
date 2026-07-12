package com.xingheyuzhuan.shiguangschedule.widget.week_timeline

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import androidx.core.graphics.ColorUtils
import com.xingheyuzhuan.shiguangschedule.widget.WidgetCourseProto
import com.xingheyuzhuan.shiguangschedule.widget.WidgetSnapshot
import com.xingheyuzhuan.shiguangschedule.widget.firstDayOfWeekValue
import com.xingheyuzhuan.shiguangschedule.widget.startOfConfiguredWeek
import com.xingheyuzhuan.shiguangschedule.widget.widgetCourseFontScale
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min

object WeekTimelineBodyRenderer {
    private const val BODY_BITMAP_WIDTH = 560
    private const val LEFT_INFO_WEIGHT = 64f
    private const val DAYS_TOTAL_WEIGHT = 497f
    private const val TOP_PADDING = 4f
    private const val BOTTOM_PADDING = 8f
    private const val LEFT_INFO_TEXT_CENTER_OFFSET = 2f
    private const val MIN_SLOT_HEIGHT = 76f
    private const val DEFAULT_SLOT_HEIGHT = 82f
    private const val DEFAULT_EMPTY_HINT_SEGMENT_COUNT = 1

    private val fallbackSectionSlots = listOf(
        SectionSlot(1, "08:00", "08:45"),
        SectionSlot(2, "08:50", "09:35"),
        SectionSlot(3, "09:55", "10:40"),
        SectionSlot(4, "10:45", "11:30"),
        SectionSlot(5, "14:00", "14:45"),
        SectionSlot(6, "14:50", "15:35"),
        SectionSlot(7, "15:55", "16:40"),
        SectionSlot(8, "16:55", "17:30"),
        SectionSlot(9, "19:00", "19:45"),
        SectionSlot(10, "19:50", "20:35")
    )

    fun renderBodyBitmap(snapshot: WidgetSnapshot): Bitmap {
        val layout = buildLayout(snapshot)
        val fullSegment = WeekTimelineSegment(
            startSlotIndex = 0,
            endSlotIndex = layout.sectionSlots.lastIndex
        )
        return renderSegmentBitmap(layout, fullSegment)
    }

    internal fun renderSegmentBitmap(snapshot: WidgetSnapshot, segment: WeekTimelineSegment): Bitmap {
        return renderSegmentBitmap(buildLayout(snapshot), segment)
    }

    internal fun buildSegments(snapshot: WidgetSnapshot): List<WeekTimelineSegment> {
        val slotCount = sectionSlotsFromSnapshot(snapshot).size
        return buildWeekTimelineSegments(slotCount)
    }

    private fun renderSegmentBitmap(
        layout: WeekTimelineLayout,
        requestedSegment: WeekTimelineSegment
    ): Bitmap {
        val segment = requestedSegment.coerceWithin(layout.sectionSlots.lastIndex)
        val segmentSlots = layout.sectionSlots.subList(segment.startSlotIndex, segment.endSlotIndex + 1)
        val bitmapHeight = (TOP_PADDING + segmentSlots.size * layout.slotHeight + BOTTOM_PADDING)
            .toInt()
            .coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(BODY_BITMAP_WIDTH, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)

        val leftColumnWidth = BODY_BITMAP_WIDTH * LEFT_INFO_WEIGHT / (LEFT_INFO_WEIGHT + DAYS_TOTAL_WEIGHT)
        val gridStart = leftColumnWidth
        val gridWidth = BODY_BITMAP_WIDTH - gridStart
        val dayWidth = gridWidth / layout.weekDates.size.coerceAtLeast(1)
        val timelineTop = TOP_PADDING
        val timelineBottom = bitmapHeight - BOTTOM_PADDING
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = layout.dividerColor
            strokeWidth = 1.15f
        }

        canvas.drawLine(gridStart - 3f, timelineTop, gridStart - 3f, timelineBottom, linePaint)
        (1 until layout.weekDates.size).forEach { index ->
            val x = gridStart + dayWidth * index
            canvas.drawLine(x, timelineTop, x, timelineBottom, linePaint)
        }

        segmentSlots.forEachIndexed { index, slot ->
            val top = timelineTop + index * layout.slotHeight
            val centerY = top + layout.slotHeight * 0.5f
            val centerX = leftColumnWidth / 2f - LEFT_INFO_TEXT_CENTER_OFFSET
            val sectionSize = if (layout.useCompactSlotText) 16f else 18.5f
            val timeSize = if (layout.useCompactSlotText) 8.5f else 9.6f
            val lineGap = if (layout.useCompactSlotText) 12f else 14.5f

            drawCenteredText(canvas, slot.section.toString(), centerX, centerY - lineGap, sectionSize, layout.primaryTextColor, bold = true)
            drawCenteredText(canvas, slot.start, centerX, centerY + 1f, timeSize, layout.hintTextColor)
            if (slot.end.isNotBlank()) {
                drawCenteredText(canvas, slot.end, centerX, centerY + lineGap, timeSize, layout.hintTextColor)
            }

            if (index > 0 || segment.startSlotIndex > 0) {
                canvas.drawLine(0f, top, BODY_BITMAP_WIDTH.toFloat(), top, linePaint)
            }
        }

        if (layout.events.isEmpty()) {
            if (segment.startSlotIndex < DEFAULT_EMPTY_HINT_SEGMENT_COUNT * 4) {
                drawEmptyWeekHint(
                    canvas = canvas,
                    gridStart = gridStart,
                    gridWidth = gridWidth,
                    timelineTop = timelineTop,
                    timelineBottom = timelineBottom,
                    primaryText = layout.primaryTextColor,
                    hintText = layout.hintTextColor
                )
            }
        } else {
            drawWeekCoursesForSegment(
                canvas = canvas,
                layout = layout,
                segment = segment,
                gridStart = gridStart,
                dayWidth = dayWidth,
                timelineTop = timelineTop,
                timelineBottom = timelineBottom
            )
        }

        return bitmap
    }

    private fun buildLayout(snapshot: WidgetSnapshot): WeekTimelineLayout {
        val weekDates = buildWeekDates(snapshot)
        val sectionSlots = sectionSlotsFromSnapshot(snapshot)
        val courses = coursesForWeek(snapshot, weekDates, sectionSlots)
        val events = courses.mapNotNull { course ->
            val dayIndex = weekDates.indexOfFirst { it.toString() == course.date }
            if (dayIndex < 0) return@mapNotNull null
            val start = sectionPositionByStartTime(course.start_time, sectionSlots)
            val end = sectionPositionByEndTime(course.end_time, sectionSlots).coerceAtLeast(start + 0.78f)
            val maxEnd = sectionSlots.size.toFloat() + 1f
            val normalizedStart = start.coerceIn(1f, maxEnd - 0.1f)
            val normalizedEnd = end.coerceIn(normalizedStart + 0.08f, maxEnd)
            CourseEvent(
                course = course,
                dayIndex = dayIndex,
                startPosition = normalizedStart,
                slotSpan = (normalizedEnd - normalizedStart).coerceAtLeast(0.08f)
            )
        }

        return WeekTimelineLayout(
            snapshot = snapshot,
            weekDates = weekDates,
            sectionSlots = sectionSlots,
            events = events,
            columns = resolveColumns(events),
            slotHeight = slotHeightForSectionCount(sectionSlots.size),
            useCompactSlotText = sectionSlots.size > 9,
            courseFontScale = snapshot.widgetCourseFontScale(),
            primaryTextColor = Color.BLACK,
            hintTextColor = ColorUtils.setAlphaComponent(Color.BLACK, 148),
            dividerColor = ColorUtils.setAlphaComponent(Color.BLACK, 24)
        )
    }

    private fun buildWeekDates(snapshot: WidgetSnapshot): List<LocalDate> {
        val today = LocalDate.now()
        val weekStart = startOfConfiguredWeek(today, snapshot.firstDayOfWeekValue())
        return if (snapshot.show_weekends) {
            (0..6).map { weekStart.plusDays(it.toLong()) }
        } else {
            (0..4).map { weekStart.plusDays(it.toLong()) }
        }
    }

    private fun slotHeightForSectionCount(sectionCount: Int): Float {
        return if (sectionCount > 9) MIN_SLOT_HEIGHT else DEFAULT_SLOT_HEIGHT
    }

    private fun sectionSlotsFromSnapshot(snapshot: WidgetSnapshot): List<SectionSlot> {
        val slots = snapshot.time_slots
            .filter { it.number > 0 && it.start_time.isNotBlank() }
            .sortedBy { it.number }
            .map { slot ->
                SectionSlot(
                    section = slot.number,
                    start = slot.start_time.toShortTime(),
                    end = slot.end_time.toShortTime()
                )
            }
        return slots.ifEmpty { fallbackSectionSlots }
    }

    private fun coursesForWeek(
        snapshot: WidgetSnapshot,
        weekDates: List<LocalDate>,
        slots: List<SectionSlot>
    ): List<WidgetCourseProto> {
        val validDates = weekDates.map { it.toString() }.toSet()
        return snapshot.courses
            .filter { it.date in validDates && !it.is_skipped }
            .sortedWith(compareBy({ it.date }, { sectionPositionByStartTime(it.start_time, slots) }, { it.start_time }))
    }

    private fun drawWeekCoursesForSegment(
        canvas: Canvas,
        layout: WeekTimelineLayout,
        segment: WeekTimelineSegment,
        gridStart: Float,
        dayWidth: Float,
        timelineTop: Float,
        timelineBottom: Float
    ) {
        val segmentStartBoundary = segment.startSlotIndex + 1f
        val segmentEndBoundary = segment.endSlotIndex + 2f

        layout.events.forEachIndexed { index, event ->
            val visibleStart = max(event.startPosition, segmentStartBoundary)
            val visibleEnd = min(event.endPosition, segmentEndBoundary)
            if (visibleEnd - visibleStart <= 0.06f) return@forEachIndexed

            val pair = layout.columns[event.identity] ?: (0 to 1)
            val columnIndex = pair.first
            val columnCount = pair.second
            val outerGap = 2.5f
            val innerGap = 2f
            val availableWidth = dayWidth - outerGap * 2f
            val cardWidth = if (columnCount <= 1) {
                availableWidth
            } else {
                (availableWidth - innerGap * (columnCount - 1)) / columnCount
            }
            val left = gridStart + event.dayIndex * dayWidth + outerGap + columnIndex * (cardWidth + innerGap)
            val top = timelineTop + (visibleStart - segmentStartBoundary) * layout.slotHeight + 2.4f
            val maxHeight = timelineBottom - top - 2f
            val height = min(max(layout.slotHeight * (visibleEnd - visibleStart) - 4.8f, 26f), maxHeight)
            val color = courseColor(layout.snapshot, event.course.color_int, index)
            drawCourseCard(canvas, left, top, cardWidth, height, event.course.displayText(), color, layout.primaryTextColor, layout.courseFontScale)
        }
    }

    private fun drawEmptyWeekHint(
        canvas: Canvas,
        gridStart: Float,
        gridWidth: Float,
        timelineTop: Float,
        timelineBottom: Float,
        primaryText: Int,
        hintText: Int
    ) {
        val centerX = gridStart + gridWidth / 2f
        val centerY = (timelineTop + timelineBottom) / 2f
        drawCenteredText(canvas, "本周暂无课程", centerX, centerY - 8f, 21f, primaryText, bold = true)
        drawCenteredText(canvas, "继续下滑可查看后续节次", centerX, centerY + 20f, 14f, hintText)
    }

    private fun drawCourseCard(
        canvas: Canvas,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        text: String,
        color: Int,
        primaryText: Int,
        fontScale: Float
    ) {
        val rect = RectF(left, top, left + width, top + height)
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }.also { paint ->
            canvas.drawRoundRect(rect, 12f, 12f, paint)
        }

        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = ColorUtils.setAlphaComponent(Color.BLACK, 18)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }.also { paint ->
            canvas.drawRoundRect(rect, 12f, 12f, paint)
        }

        val textSize = 10.5f * fontScale
        val lineHeight = 13.2f * fontScale
        val firstBaseline = 17f * fontScale.coerceAtLeast(0.9f)
        val horizontalInset = 6f
        val saveCount = canvas.save()
        canvas.clipRect(RectF(left + horizontalInset, top + 6f, left + width - horizontalInset, top + height - 6f))
        val lines = text.replace(" @", "\n@").split('\n')
            .flatMap { wrapText(it.trim(), width - horizontalInset * 2f, textSize, bold = true) }
            .filter { it.isNotBlank() }
        val maxLines = max(1, ((height - 10f) / lineHeight).toInt())
        lines.take(maxLines).forEachIndexed { index, line ->
            drawText(canvas, line, left + horizontalInset + 1f, top + firstBaseline + index * lineHeight, textSize, primaryText, bold = true)
        }
        canvas.restoreToCount(saveCount)
    }

    private fun resolveColumns(events: List<CourseEvent>): Map<String, Pair<Int, Int>> {
        val result = mutableMapOf<String, Pair<Int, Int>>()
        events.groupBy { it.dayIndex }.forEach { (_, dayEvents) ->
            dayEvents.forEach { event ->
                val overlaps = dayEvents.filter { it.overlaps(event) }
                if (overlaps.size <= 1) {
                    result[event.identity] = 0 to 1
                    return@forEach
                }
                val ordered = overlaps.sortedWith(compareBy({ it.startPosition }, { it.course.id }, { it.course.name }))
                ordered.forEachIndexed { index, overlap ->
                    result[overlap.identity] = index to ordered.size
                }
            }
        }
        return result
    }

    private fun CourseEvent.overlaps(other: CourseEvent): Boolean {
        return dayIndex == other.dayIndex && startPosition < other.endPosition && endPosition > other.startPosition
    }

    private fun sectionPositionByStartTime(timeText: String, slots: List<SectionSlot>): Float {
        val time = parseTime(timeText) ?: return 1f
        slots.forEachIndexed { index, slot ->
            val start = parseTime(slot.start) ?: return@forEachIndexed
            val end = parseTime(slot.end)
            if (time.isBefore(start)) return index + 1f
            if (end == null) {
                if (!time.isBefore(start)) return index + 1f
            } else if (!time.isBefore(start) && time <= end) {
                val minutes = ChronoUnit.MINUTES.between(start, end).coerceAtLeast(1)
                val offset = ChronoUnit.MINUTES.between(start, time).coerceAtLeast(0)
                return index + 1f + (offset.toFloat() / minutes.toFloat()).coerceIn(0f, 0.95f)
            }
        }
        return slots.size.toFloat() + 1f
    }

    private fun sectionPositionByEndTime(timeText: String, slots: List<SectionSlot>): Float {
        val time = parseTime(timeText) ?: return 2f
        slots.forEachIndexed { index, slot ->
            val start = parseTime(slot.start) ?: return@forEachIndexed
            val end = parseTime(slot.end)
            if (time.isBefore(start)) return index + 1f
            if (end == null) {
                if (!time.isBefore(start)) return index + 2f
            } else if (!time.isBefore(start) && time <= end) {
                val minutes = ChronoUnit.MINUTES.between(start, end).coerceAtLeast(1)
                val offset = ChronoUnit.MINUTES.between(start, time).coerceAtLeast(0)
                return index + 1f + (offset.toFloat() / minutes.toFloat()).coerceIn(0.05f, 1f)
            }
        }
        return slots.size.toFloat() + 1f
    }

    private fun String.toShortTime(): String {
        return trim().takeIf { it.isNotBlank() }?.take(5).orEmpty()
    }

    private fun parseTime(timeText: String): LocalTime? {
        val normalized = timeText.trim()
        if (normalized.isBlank()) return null
        return runCatching { LocalTime.parse(normalized) }.getOrNull()
    }

    private fun courseColor(snapshot: WidgetSnapshot, colorInt: Int, index: Int): Int {
        val colorMaps = snapshot.style?.course_color_maps.orEmpty()
        if (colorInt >= 0 && colorInt < colorMaps.size) {
            return colorMaps[colorInt].light_color.toInt()
        }
        val fallback = intArrayOf(
            Color.rgb(168, 205, 235),
            Color.rgb(178, 217, 188),
            Color.rgb(238, 207, 141),
            Color.rgb(224, 184, 190),
            Color.rgb(196, 188, 222)
        )
        return fallback[index % fallback.size]
    }

    private fun WidgetCourseProto.displayText(): String {
        val location = position.trim()
        return if (location.isBlank()) name else "$name @$location"
    }

    private fun drawText(
        canvas: Canvas,
        text: String,
        x: Float,
        baseline: Float,
        size: Float,
        color: Int,
        bold: Boolean = false
    ) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            typeface = if (bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
        }
        canvas.drawText(text, x, baseline, paint)
    }

    private fun drawCenteredText(
        canvas: Canvas,
        text: String,
        centerX: Float,
        baseline: Float,
        size: Float,
        color: Int,
        bold: Boolean = false
    ) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            textAlign = Paint.Align.CENTER
            typeface = if (bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
        }
        canvas.drawText(text, centerX, baseline, paint)
    }

    private fun wrapText(text: String, maxWidth: Float, size: Float, bold: Boolean): List<String> {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            typeface = if (bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
        }
        val result = mutableListOf<String>()
        var current = ""
        text.forEach { char ->
            val next = current + char
            if (paint.measureText(next) <= maxWidth || current.isEmpty()) {
                current = next
            } else {
                result += current
                current = char.toString()
            }
        }
        if (current.isNotEmpty()) result += current
        return result
    }

    private fun WeekTimelineSegment.coerceWithin(lastSlotIndex: Int): WeekTimelineSegment {
        if (lastSlotIndex <= 0) return WeekTimelineSegment(0, 0)
        val start = startSlotIndex.coerceIn(0, lastSlotIndex)
        val end = endSlotIndex.coerceIn(start, lastSlotIndex)
        return WeekTimelineSegment(start, end)
    }

    private data class SectionSlot(
        val section: Int,
        val start: String,
        val end: String
    )

    private data class CourseEvent(
        val course: WidgetCourseProto,
        val dayIndex: Int,
        val startPosition: Float,
        val slotSpan: Float
    ) {
        val endPosition: Float = startPosition + slotSpan
        val identity: String = "${course.id}_${course.date}_${course.start_time}_${course.name}"
    }

    private data class WeekTimelineLayout(
        val snapshot: WidgetSnapshot,
        val weekDates: List<LocalDate>,
        val sectionSlots: List<SectionSlot>,
        val events: List<CourseEvent>,
        val columns: Map<String, Pair<Int, Int>>,
        val slotHeight: Float,
        val useCompactSlotText: Boolean,
        val courseFontScale: Float,
        val primaryTextColor: Int,
        val hintTextColor: Int,
        val dividerColor: Int
    )
}
