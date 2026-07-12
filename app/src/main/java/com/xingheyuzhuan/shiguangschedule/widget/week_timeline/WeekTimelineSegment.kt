package com.xingheyuzhuan.shiguangschedule.widget.week_timeline

internal data class WeekTimelineSegment(
    val startSlotIndex: Int,
    val endSlotIndex: Int
) {
    init {
        require(startSlotIndex >= 0) { "startSlotIndex must be >= 0" }
        require(endSlotIndex >= startSlotIndex) { "endSlotIndex must be >= startSlotIndex" }
    }

    val slotCount: Int get() = endSlotIndex - startSlotIndex + 1
}

internal fun buildWeekTimelineSegments(
    totalSlots: Int,
    fallbackTotalSlots: Int = 13
): List<WeekTimelineSegment> {
    val normalizedTotalSlots = if (totalSlots > 0) totalSlots else fallbackTotalSlots
    if (normalizedTotalSlots <= 0) return emptyList()

    val result = mutableListOf<WeekTimelineSegment>()
    var start = 0
    while (normalizedTotalSlots - start > 5) {
        result += WeekTimelineSegment(startSlotIndex = start, endSlotIndex = start + 3)
        start += 4
    }
    result += WeekTimelineSegment(startSlotIndex = start, endSlotIndex = normalizedTotalSlots - 1)
    return result
}
