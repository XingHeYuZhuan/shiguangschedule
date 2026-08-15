package com.xingheyuzhuan.shiguangschedule.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleGridStyleProto
import com.xingheyuzhuan.shiguangschedule.data.repository.SCHEDULE_STYLE_DATASTORE_FILE_NAME
import com.xingheyuzhuan.shiguangschedule.data.repository.ScheduleStyleSerializer
import okio.FileSystem
import okio.Path
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@Suppress("unused")
class DataStoreModule {

    @Single
    fun provideScheduleStyleDataStore(
        fileSystem: FileSystem,
        @Named("FilesDir") filesDir: Path
    ): DataStore<ScheduleGridStyleProto> {
        return DataStoreFactory.create(
            storage = OkioStorage(
                fileSystem = fileSystem,
                serializer = ScheduleStyleSerializer,
                producePath = { filesDir / SCHEDULE_STYLE_DATASTORE_FILE_NAME }
            )
        )
    }

    @Single
    @Named("SchoolHistory")
    fun provideSchoolHistoryDataStore(
        @Named("FilesDir") filesDir: Path
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.createWithPath(
            produceFile = { filesDir / "datastore" / "school_history.preferences_pb" }
        )
    }

    @Single
    @Named("AppSettings")
    fun provideAppSettingsDataStore(
        @Named("FilesDir") filesDir: Path
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.createWithPath(
            produceFile = { filesDir / "datastore" / "app_settings.preferences_pb" }
        )
    }

    @Single
    @Named("ApiConfig")
    fun provideApiConfigDataStore(
        @Named("FilesDir") filesDir: Path
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.createWithPath(
            produceFile = { filesDir / "datastore" / "api_config.preferences_pb" }
        )
    }
}