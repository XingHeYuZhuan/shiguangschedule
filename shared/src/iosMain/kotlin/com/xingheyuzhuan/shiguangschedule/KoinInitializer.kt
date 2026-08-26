package com.xingheyuzhuan.shiguangschedule

import com.xingheyuzhuan.shiguangschedule.data.di.SharedModule
import com.xingheyuzhuan.shiguangschedule.data.di.DatabaseModule
import com.xingheyuzhuan.shiguangschedule.data.di.DataStoreModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import org.koin.plugin.module.dsl.startKoin
import platform.Foundation.NSBundle

@Module(includes = [
    DatabaseModule::class,
    DataStoreModule::class,
    SharedModule::class
])
@ComponentScan("com.xingheyuzhuan.shiguangschedule")
class IosAppModule {
    @Single
    @Named("AppVersionCode")
    fun provideAppVersionCode(): Int {
        return NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion")
            ?.toString()
            ?.toIntOrNull()
            ?: 1
    }

    @Single
    @Named("AppVersionName")
    fun provideAppVersionName(): String {
        return NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString")
            ?.toString()
            ?.takeIf { it.isNotBlank() }
            ?: "1.0.0"
    }
}

@KoinApplication(modules = [IosAppModule::class])
class IosScheduleAppConfig

fun initKoin() {
    startKoin<IosScheduleAppConfig> {}
}
