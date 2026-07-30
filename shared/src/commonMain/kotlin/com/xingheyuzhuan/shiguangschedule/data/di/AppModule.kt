package com.xingheyuzhuan.shiguangschedule.data.di

import okio.FileSystem
import okio.SYSTEM
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("com.xingheyuzhuan.shiguangschedule")
class AppModule {

    // 向全局注入 Okio 文件系统，供通用文件读写使用
    @Single
    fun provideFileSystem(): FileSystem = FileSystem.SYSTEM
}