package com.xingheyuzhuan.shiguangschedule.data.repository

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioSerializer
import com.xingheyuzhuan.shiguangschedule.data.model.DualColor
import com.xingheyuzhuan.shiguangschedule.data.model.ScheduleGridStyle
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.BorderTypeProto
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleGridStyleProto
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleModeProto
import com.xingheyuzhuan.shiguangschedule.data.model.toCompose
import com.xingheyuzhuan.shiguangschedule.data.model.toProto
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.Serializable
import okio.BufferedSink
import okio.BufferedSource
import org.koin.core.annotation.Single

/** DataStore 文件名常量 */
const val SCHEDULE_STYLE_DATASTORE_FILE_NAME = "schedule_style_settings.pb"

/**
 * 样式配置的 DataStore 序列化器，基于 Wire 协议与 Okio 跨平台流实现。
 */
object ScheduleStyleSerializer : OkioSerializer<ScheduleGridStyleProto> {
    override val defaultValue: ScheduleGridStyleProto
        get() = ScheduleGridStyleProto()

    override suspend fun readFrom(source: BufferedSource): ScheduleGridStyleProto {
        return try {
            ScheduleGridStyleProto.ADAPTER.decode(source)
        } catch (e: Exception) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: ScheduleGridStyleProto, sink: BufferedSink) {
        ScheduleGridStyleProto.ADAPTER.encode(sink, t)
    }
}

/**
 * 样式备份信封，包含版本号及序列化后的字节数据。
 */
@Serializable
data class StyleBackupEnvelope(
    val backupTimestamp: Long,
    val appVersionCode: Int,
    val styleProtoBytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StyleBackupEnvelope) return false

        if (backupTimestamp != other.backupTimestamp) return false
        if (appVersionCode != other.appVersionCode) return false
        if (!styleProtoBytes.contentEquals(other.styleProtoBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = backupTimestamp.hashCode()
        result = 31 * result + appVersionCode
        result = 31 * result + styleProtoBytes.contentHashCode()
        return result
    }
}

/**
 * 样式设置的数据仓库，负责与 Proto DataStore 进行交互及状态管理。
 */
@Single
class StyleSettingsRepository(
    private val dataStore: DataStore<ScheduleGridStyleProto>
) {

    companion object {
        /** 当前样式备份的版本号 */
        const val STYLE_SCHEMA_VERSION = 1
    }

    private val _styleUpdatedChannel = Channel<Unit>(Channel.CONFLATED)
    val styleUpdatedFlow: Flow<Unit> = _styleUpdatedChannel.receiveAsFlow()

    // --- 备份与恢复扩展 API ---

    /**
     * 仅导出当前原生的样式配置字节数组，排除壁纸路径。
     */
    suspend fun exportRawStyleBytes(): ByteArray {
        val currentProto = dataStore.data.first()
        val exportProto = currentProto.copy(background_image_path = "")
        return ScheduleGridStyleProto.ADAPTER.encode(exportProto)
    }

    /**
     * 将还原的字节数组与本地壁纸路径合并后写入 DataStore。
     */
    suspend fun restoreRawStyleBytes(bytes: ByteArray): Result<Unit> = runCatching {
        val currentLocalProto = dataStore.data.first()
        val localWallpaperPath = currentLocalProto.background_image_path

        val backupProto = ScheduleGridStyleProto.ADAPTER.decode(bytes)
        val finalProto = backupProto.copy(background_image_path = localWallpaperPath)

        dataStore.updateData { finalProto }
        _styleUpdatedChannel.trySend(Unit)
    }

    /**
     * 获取当前样式的单次快照。
     */
    suspend fun getStyleOnce(): ScheduleGridStyle {
        return dataStore.data.map { it.toCompose() }.first()
    }

    /**
     * 响应式样式数据流。
     */
    val styleFlow: Flow<ScheduleGridStyle> = dataStore.data
        .map { proto -> proto.toCompose() }

    private suspend fun updateStyle(
        transform: (ScheduleGridStyleProto) -> ScheduleGridStyleProto
    ) {
        dataStore.updateData { currentProto ->
            transform(currentProto)
        }
        _styleUpdatedChannel.trySend(Unit)
    }

    // --- 原子化公共写入 API (Setters) ---

    /** 设置时间列宽度 (DP) */
    suspend fun setTimeColumnWidth(widthDp: Float) = updateStyle {
        it.copy(time_column_width_dp = widthDp)
    }

    /** 设置日表头高度 (DP) */
    suspend fun setDayHeaderHeight(heightDp: Float) = updateStyle {
        it.copy(day_header_height_dp = heightDp)
    }

    /** 设置节次高度 (DP) */
    suspend fun setSectionHeight(heightDp: Float) = updateStyle {
        it.copy(section_height_dp = heightDp)
    }

    /** 设置课程块圆角半径 (DP) */
    suspend fun setCourseBlockCornerRadius(radiusDp: Float) = updateStyle {
        it.copy(course_block_corner_radius_dp = radiusDp)
    }

    /** 设置课程块外部边距 (DP) */
    suspend fun setCourseBlockOuterPadding(paddingDp: Float) = updateStyle {
        it.copy(course_block_outer_padding_dp = paddingDp)
    }

    /** 设置课程块内部填充 (DP) */
    suspend fun setCourseBlockInnerPadding(paddingDp: Float) = updateStyle {
        it.copy(course_block_inner_padding_dp = paddingDp)
    }

    /** 设置课程块透明度 */
    suspend fun setCourseBlockAlpha(alpha: Float) = updateStyle {
        it.copy(course_block_alpha_float = alpha)
    }

    /** 设置课程颜色映射列表 */
    suspend fun setCourseColorMaps(maps: List<DualColor>) {
        updateStyle {
            it.copy(course_color_maps = maps.map { dc -> dc.toProto() })
        }
    }

    /** 重置所有样式设置为默认值 */
    suspend fun resetAllStyleSettings() {
        dataStore.updateData {
            ScheduleGridStyleProto()
        }
        _styleUpdatedChannel.trySend(Unit)
    }

    /** 设置是否隐藏左侧时间列的具体时间 */
    suspend fun setHideSectionTime(hide: Boolean) = updateStyle {
        it.copy(hide_section_time = hide)
    }

    /** 设置是否隐藏星期栏下的日期 */
    suspend fun setHideDateUnderDay(hide: Boolean) = updateStyle {
        it.copy(hide_date_under_day = hide)
    }

    /** 设置是否隐藏网格线 */
    suspend fun setHideGridLines(hide: Boolean) = updateStyle {
        it.copy(hide_grid_lines = hide)
    }

    /** 设置是否在课程格内显示开始时间 */
    suspend fun setShowStartTime(show: Boolean) = updateStyle {
        it.copy(show_start_time = show)
    }

    /** 设置课程块字体缩放比例 */
    suspend fun setCourseBlockFontScale(scale: Float) = updateStyle {
        it.copy(course_block_font_scale = scale)
    }

    /** 设置是否隐藏上课地点 */
    suspend fun setHideLocation(hide: Boolean) = updateStyle {
        it.copy(hide_location = hide)
    }

    /** 设置是否隐藏授课老师 */
    suspend fun setHideTeacher(hide: Boolean) = updateStyle {
        it.copy(hide_teacher = hide)
    }

    /** 设置是否移除地点前的 @ 符号 */
    suspend fun setRemoveLocationAt(remove: Boolean) = updateStyle {
        it.copy(remove_location_at = remove)
    }

    /** 设置文字水平居中 */
    suspend fun setTextAlignCenterHorizontal(center: Boolean) = updateStyle {
        it.copy(text_align_center_horizontal = center)
    }

    /** 设置文字垂直居中 */
    suspend fun setTextAlignCenterVertical(center: Boolean) = updateStyle {
        it.copy(text_align_center_vertical = center)
    }

    /** 设置边框类型 */
    suspend fun setBorderType(type: BorderTypeProto) = updateStyle {
        it.copy(border_type = type)
    }

    /** 设置课表展示模式 */
    suspend fun setScheduleMode(mode: ScheduleModeProto) = updateStyle {
        it.copy(schedule_mode = mode)
    }

    /** 设置页面文本颜色 */
    suspend fun setPageTextColor(color: Color?) = updateStyle {
        it.copy(page_text_color_long = color?.toArgb()?.toLong())
    }

    /** 设置课程块文字颜色 */
    suspend fun setCourseTextColor(color: Color?) = updateStyle {
        it.copy(course_text_color_long = color?.toArgb()?.toLong())
    }

    /** 设置背景壁纸路径 */
    suspend fun setBackgroundImagePath(path: String) = updateStyle {
        it.copy(background_image_path = path)
    }

    /** 重置样式设置但保留壁纸 */
    suspend fun resetAllStyleSettingsExceptWallpaper() {
        dataStore.updateData { currentProto ->
            val currentPath = currentProto.background_image_path
            ScheduleGridStyleProto().copy(background_image_path = currentPath)
        }
        _styleUpdatedChannel.trySend(Unit)
    }
}