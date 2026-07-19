package com.xingheyuzhuan.shiguangschedule.data.repository

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.xingheyuzhuan.shiguangschedule.data.model.DualColor
import com.xingheyuzhuan.shiguangschedule.data.model.ScheduleGridStyle
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.BorderTypeProto
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleGridStyleProto
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleModeProto
import com.xingheyuzhuan.shiguangschedule.data.model.toCompose
import com.xingheyuzhuan.shiguangschedule.data.model.toProto
import com.xingheyuzhuan.shiguangschedule.widget.updateAllWidgets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import java.io.InputStream
import java.io.OutputStream
import org.koin.core.annotation.Single

/** DataStore 文件名常量 */
const val SCHEDULE_STYLE_DATASTORE_FILE_NAME = "schedule_style_settings.pb"

/**
 * DataStore Serializer (适配 Wire 协议)
 * * 使用 Wire 生成的 ADAPTER 替代 Google Protobuf 的 parseFrom/writeTo。
 */
object ScheduleStyleSerializer : Serializer<ScheduleGridStyleProto> {
    /** 默认值：Wire 中直接构造实例即代表所有字段为默认值的 Proto 对象 */
    override val defaultValue: ScheduleGridStyleProto
        get() = ScheduleGridStyleProto()

    override suspend fun readFrom(input: InputStream): ScheduleGridStyleProto {
        return try {
            // 使用 Wire 生成的 ADAPTER 解码
            ScheduleGridStyleProto.ADAPTER.decode(input)
        } catch (e: Exception) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: ScheduleGridStyleProto, output: OutputStream) {
        // 使用 Wire 生成的 ADAPTER 编码
        ScheduleGridStyleProto.ADAPTER.encode(output, t)
    }
}

/**
 * 扩展属性：定义 ScheduleGridStyle 的 DataStore。
 * 放在这里可以确保单例性，同时让实现细节对外部隐藏。
 */
val Context.scheduleGridStyleDataStore: DataStore<ScheduleGridStyleProto> by dataStore(
    fileName = SCHEDULE_STYLE_DATASTORE_FILE_NAME,
    serializer = ScheduleStyleSerializer
)

/**
 * 样式自持版本号的中央备份信封
 * 放在数据源头，对齐课表 envelope 的设计，保持物理传输字段名 appVersionCode
 */
@Serializable
data class StyleBackupEnvelope(
    val backupTimestamp: Long,
    val appVersionCode: Int,
    val styleProtoBytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StyleBackupEnvelope

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
 * 样式设置的数据仓库，负责与 Proto DataStore (Wire) 进行交互。
 * * 注意：由于 Wire 生成的是不可变类，所有更新操作均通过 .copy() 及其生成的下划线字段完成。
 *
 */
@Single
class StyleSettingsRepository(
    private val dataStore: DataStore<ScheduleGridStyleProto>,
    private val context: Context
) {

    companion object {
        /** 当前样式备份的版本号 */
        const val STYLE_SCHEMA_VERSION = 1
    }

    // --- 备份与恢复扩展 API ---

    /**
     * 仅导出当前原生的样式配置字节数组 (排除壁纸路径)
     */
    suspend fun exportRawStyleBytes(): ByteArray {
        val currentProto = dataStore.data.first()
        val exportProto = currentProto.copy(background_image_path = "")
        return ScheduleGridStyleProto.ADAPTER.encode(exportProto)
    }

    /**
     * 将清洗/升级完毕后的原生字节数组还原 (缝合本地壁纸并写入)
     */
    suspend fun restoreRawStyleBytes(bytes: ByteArray): Result<Unit> = runCatching {
        val currentLocalProto = dataStore.data.first()
        val localWallpaperPath = currentLocalProto.background_image_path

        val backupProto = ScheduleGridStyleProto.ADAPTER.decode(bytes)
        val finalProto = backupProto.copy(background_image_path = localWallpaperPath)

        dataStore.updateData { finalProto }
        updateAllWidgets(context)
    }

    /**
     * 获取当前样式的快照（一次性读取，用于业务逻辑校验）
     */
    suspend fun getStyleOnce(): ScheduleGridStyle {
        return dataStore.data.map { it.toCompose() }.first()
    }

    /**
     * 响应式样式流（用于 UI 订阅刷新）
     */
    val styleFlow: Flow<ScheduleGridStyle> = dataStore.data
        .map { proto -> proto.toCompose() }

    /**
     * 通用写入 API
     * 适配 Wire：将原来的 Builder 模式改为 Kotlin 特性的 Lambda 转换。
     */
    private suspend fun updateStyle(
        transform: (ScheduleGridStyleProto) -> ScheduleGridStyleProto
    ) {
        dataStore.updateData { currentProto ->
            transform(currentProto)
        }
    }

    // --- 原子化公共写入 API (Setters) ---

    /** 设置时间列宽度 (DP 值) */
    suspend fun setTimeColumnWidth(widthDp: Float) = updateStyle {
        it.copy(time_column_width_dp = widthDp)
    }

    /** 设置日表头高度 (DP 值) */
    suspend fun setDayHeaderHeight(heightDp: Float) = updateStyle {
        it.copy(day_header_height_dp = heightDp)
    }

    /** 设置节次高度 (DP 值) */
    suspend fun setSectionHeight(heightDp: Float) = updateStyle {
        it.copy(section_height_dp = heightDp)
    }

    /** 设置圆角半径 (DP 值) */
    suspend fun setCourseBlockCornerRadius(radiusDp: Float) = updateStyle {
        it.copy(course_block_corner_radius_dp = radiusDp)
    }

    /** 设置外部边距 (DP 值) */
    suspend fun setCourseBlockOuterPadding(paddingDp: Float) = updateStyle {
        it.copy(course_block_outer_padding_dp = paddingDp)
    }

    /** 设置内部填充 (DP 值) */
    suspend fun setCourseBlockInnerPadding(paddingDp: Float) = updateStyle {
        it.copy(course_block_inner_padding_dp = paddingDp)
    }

    /** 设置透明度 (0.0f - 1.0f) */
    suspend fun setCourseBlockAlpha(alpha: Float) = updateStyle {
        it.copy(course_block_alpha_float = alpha)
    }


    /** 设置颜色列表映射 */
    suspend fun setCourseColorMaps(maps: List<DualColor>) {
        updateStyle {
            // Wire 的列表是不可变的，直接通过 copy 替换整个列表
            it.copy(course_color_maps = maps.map { dc -> dc.toProto() })
        }
        updateAllWidgets(context)
    }

    /** 重置为默认样式 */
    suspend fun resetAllStyleSettings() {
        dataStore.updateData {
            // 直接构造空对象即为默认
            ScheduleGridStyleProto()
        }
        updateAllWidgets(context)
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

    /** 设置课程块字体的缩放比例 */
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

    /** 设置课表展示模式（传统节次模式 vs 24小时绝对时间轴模式） */
    suspend fun setScheduleMode(mode: ScheduleModeProto) = updateStyle {
        it.copy(schedule_mode = mode)
    }

    /**
     * 设置自定义页面文本颜色
     * @param color 传入 Color 对象；传 null 则清除自定义颜色，恢复系统默认
     */
    suspend fun setPageTextColor(color: Color?) = updateStyle {
        it.copy(page_text_color_long = color?.toArgb()?.toLong())
    }

    /** 设置课程块文字颜色 */
    suspend fun setCourseTextColor(color: Color?) = updateStyle {
        it.copy(course_text_color_long = color?.toArgb()?.toLong())
    }

    /** 设置背景壁纸的物理路径 */
    suspend fun setBackgroundImagePath(path: String) = updateStyle {
        it.copy(background_image_path = path)
    }

    /**
     * 重置为默认样式（但保留壁纸）
     */
    suspend fun resetAllStyleSettingsExceptWallpaper() {
        dataStore.updateData { currentProto ->
            val currentPath = currentProto.background_image_path
            // 先创建一个全默认对象，再 copy 之前的路径进去
            ScheduleGridStyleProto().copy(background_image_path = currentPath)
        }
        updateAllWidgets(context)
    }
}