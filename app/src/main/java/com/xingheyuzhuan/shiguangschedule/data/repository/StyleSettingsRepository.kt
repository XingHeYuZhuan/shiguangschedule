package com.xingheyuzhuan.shiguangschedule.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.xingheyuzhuan.shiguangschedule.data.model.DualColor
import com.xingheyuzhuan.shiguangschedule.data.model.ScheduleGridStyle
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.BorderTypeProto
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleGridStyleProto
import com.xingheyuzhuan.shiguangschedule.data.model.toCompose
import com.xingheyuzhuan.shiguangschedule.data.model.toProto
import com.xingheyuzhuan.shiguangschedule.widget.updateAllWidgets
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

// DataStore 文件名常量
const val SCHEDULE_STYLE_DATASTORE_FILE_NAME = "schedule_style_settings.pb"

// DataStore Serializer (序列化器)
object ScheduleStyleSerializer : Serializer<ScheduleGridStyleProto> {
    override val defaultValue: ScheduleGridStyleProto
        get() = ScheduleGridStyleProto.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): ScheduleGridStyleProto {
        return try {
            ScheduleGridStyleProto.parseFrom(input)
        } catch (exception: Exception) {
            ScheduleGridStyleProto.getDefaultInstance()
        }
    }

    override suspend fun writeTo(t: ScheduleGridStyleProto, output: OutputStream) {
        t.writeTo(output)
    }
}

// 定义扩展属性 (单例声明)
/**
 * 扩展属性：定义 ScheduleGridStyle 的 DataStore。
 * 放在这里可以确保单例性，同时让实现细节对外部隐藏。
 */
val Context.scheduleGridStyleDataStore: DataStore<ScheduleGridStyleProto> by dataStore(
    fileName = SCHEDULE_STYLE_DATASTORE_FILE_NAME,
    serializer = ScheduleStyleSerializer
)

// 4. StyleSettingsRepository (仓库类)
/**
 * 样式设置的数据仓库，负责与 Proto DataStore 进行交互。
 */
@Singleton
class StyleSettingsRepository @Inject constructor(
    private val dataStore: DataStore<ScheduleGridStyleProto>,
    @ApplicationContext private val context: Context
) {

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

    // --- 通用写入 API ---
    private suspend fun updateStyle(
        transform: ScheduleGridStyleProto.Builder.() -> Unit
    ) {
        dataStore.updateData { currentProto ->
            currentProto.toBuilder().apply(transform).build()
        }
    }

    // --- 原子化公共写入 API (Setters) ---

    /** 设置时间列宽度 (DP 值) */
    suspend fun setTimeColumnWidth(widthDp: Float) = updateStyle { timeColumnWidthDp = widthDp }
    /** 设置日表头高度 (DP 值) */
    suspend fun setDayHeaderHeight(heightDp: Float) = updateStyle { dayHeaderHeightDp = heightDp }
    /** 设置节次高度 (DP 值) */
    suspend fun setSectionHeight(heightDp: Float) = updateStyle { sectionHeightDp = heightDp }

    /** 设置圆角半径 (DP 值) */
    suspend fun setCourseBlockCornerRadius(radiusDp: Float) = updateStyle { courseBlockCornerRadiusDp = radiusDp }
    /** 设置外部边距 (DP 值) */
    suspend fun setCourseBlockOuterPadding(paddingDp: Float) = updateStyle { courseBlockOuterPaddingDp = paddingDp }
    /** 设置内部填充 (DP 值) */
    suspend fun setCourseBlockInnerPadding(paddingDp: Float) = updateStyle { courseBlockInnerPaddingDp = paddingDp }
    /** 设置透明度 (0.0f - 1.0f) */
    suspend fun setCourseBlockAlpha(alpha: Float) = updateStyle { courseBlockAlphaFloat = alpha }

    /** 设置重叠样式切换开关 */
    suspend fun setOverlapStyleToggle(enabled: Boolean) = updateStyle {
        overlapStyleToggle = enabled
    }

    /** 设置颜色列表映射 */
    suspend fun setCourseColorMaps(maps: List<DualColor>) {
        updateStyle {
            clearCourseColorMaps()
            addAllCourseColorMaps(maps.map { it.toProto() })
        }
        updateAllWidgets(context)
    }

    /** 重置为默认样式 */
    suspend fun resetAllStyleSettings() {
        dataStore.updateData {
            ScheduleGridStyleProto.getDefaultInstance()
        }
        updateAllWidgets(context)
    }

    /** 设置是否隐藏左侧时间列的具体时间 */
    suspend fun setHideSectionTime(hide: Boolean) = updateStyle {
        hideSectionTime = hide
    }

    /** 设置是否隐藏星期栏下的日期 */
    suspend fun setHideDateUnderDay(hide: Boolean) = updateStyle {
        hideDateUnderDay = hide
    }

    /** 设置是否隐藏网格线 */
    suspend fun setHideGridLines(hide: Boolean) = updateStyle {
        hideGridLines = hide
    }

    /** 设置是否在课程格内显示开始时间 */
    suspend fun setShowStartTime(show: Boolean) = updateStyle {
        showStartTime = show
    }

    /** 设置课程块字体的缩放比例 */
    suspend fun setCourseBlockFontScale(scale: Float) = updateStyle {
        courseBlockFontScale = scale
    }

    /** 设置是否隐藏上课地点 */
    suspend fun setHideLocation(hide: Boolean) = updateStyle {
        hideLocation = hide
    }

    /** 设置是否隐藏授课老师 */
    suspend fun setHideTeacher(hide: Boolean) = updateStyle {
        hideTeacher = hide
    }

    /** 设置是否移除地点前的 @ 符号 */
    suspend fun setRemoveLocationAt(remove: Boolean) = updateStyle {
        removeLocationAt = remove
    }

    /** 设置文字水平居中 */
    suspend fun setTextAlignCenterHorizontal(center: Boolean) = updateStyle {
        textAlignCenterHorizontal = center
    }

    /** 设置文字垂直居中 */
    suspend fun setTextAlignCenterVertical(center: Boolean) = updateStyle {
        textAlignCenterVertical = center
    }

    /** 设置边框类型 */
    suspend fun setBorderType(type: BorderTypeProto) = updateStyle {
        borderType = type
    }

    /** 设置背景壁纸的物理路径 */
    suspend fun setBackgroundImagePath(path: String) = updateStyle {
        backgroundImagePath = path
    }

    /**
     * 重置为默认样式（但保留壁纸）
     */
    suspend fun resetAllStyleSettingsExceptWallpaper() {
        dataStore.updateData { currentProto ->
            val currentPath = currentProto.backgroundImagePath
            ScheduleGridStyleProto.newBuilder()
                .setBackgroundImagePath(currentPath)
                .build()
        }
        updateAllWidgets(context)
    }
}