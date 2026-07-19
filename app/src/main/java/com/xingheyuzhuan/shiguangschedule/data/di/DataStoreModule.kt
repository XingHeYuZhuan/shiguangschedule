package com.xingheyuzhuan.shiguangschedule.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleGridStyleProto
import com.xingheyuzhuan.shiguangschedule.data.repository.scheduleGridStyleDataStore
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.koin.core.annotation.Named

// 定义 AppSettings Preferences DataStore 委托
private val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")
// 定义 SchoolHistory Preferences DataStore 委托
private val Context.schoolHistoryDataStore: DataStore<Preferences> by preferencesDataStore(name = "school_history")
// 定义通用的 api_config 文件委托
private val Context.apiConfigDataStore: DataStore<Preferences> by preferencesDataStore(name = "api_config")

@Module
@Configuration
@Suppress("unused")
class DataStoreModule {

    /**
     * 提供课表网格样式 DataStore (Proto 模式)
     */
    @Single
    fun provideScheduleStyleDataStore(context: Context): DataStore<ScheduleGridStyleProto> {
        return context.scheduleGridStyleDataStore
    }

    /**
     * 提供学校选择历史 DataStore
     */
    @Single
    @Named("SchoolHistory")
    fun provideSchoolHistoryDataStore(context: Context): DataStore<Preferences> {
        return context.schoolHistoryDataStore
    }

    /**
     * 提供全局设置 DataStore
     */
    @Single
    @Named("AppSettings")
    fun provideAppSettingsDataStore(context: Context): DataStore<Preferences> {
        return context.appSettingsDataStore
    }

    /**
     * 提供通用的 API 配置 DataStore 实例
     */
    @Single
    @Named("ApiConfig")
    fun provideApiConfigDataStore(context: Context): DataStore<Preferences> {
        return context.apiConfigDataStore
    }
}