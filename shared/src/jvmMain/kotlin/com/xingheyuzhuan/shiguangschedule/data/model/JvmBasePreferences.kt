package com.xingheyuzhuan.shiguangschedule.data.model

import com.xingheyuzhuan.shiguangschedule.data.di.AppStorage
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.util.Properties

/**
 * JVM 平台特有配置持久化基类
 *
 * 基于 `.properties` 属性文件实现轻量级 Key-Value 同步读写落盘。
 * 存储路径由 [AppStorage] 自动分配至当前操作系统的标准 AppData/files 目录下。
 *
 * @param fileName 存储的属性文件名（如 "locale_settings.properties"）
 */
abstract class JvmBasePreferences(
    private val fileName: String
) : KoinComponent {

    private val appStorage: AppStorage by inject()

    protected val configFile: File by lazy {
        File(appStorage.filesDir.toString(), fileName)
    }

    protected val properties = Properties()

    init {
        load()
    }

    /**
     * 从本地文件加载配置到内存
     */
    protected fun load() {
        if (configFile.exists()) {
            runCatching {
                configFile.inputStream().use { properties.load(it) }
            }
        }
    }

    /**
     * 将内存配置同步保存至本地文件
     */
    protected fun save(comments: String = "JVM Platform Preferences Storage") {
        runCatching {
            configFile.outputStream().use {
                properties.store(it, comments)
            }
        }
    }

    // --- Key-Value 读写 API ---

    protected fun getString(key: String, defaultValue: String = ""): String {
        return properties.getProperty(key, defaultValue)
    }

    /**
     * 写入 String（传入 null 或空字符串时自动移除该 Key）
     */
    protected fun putString(key: String, value: String?) {
        if (value.isNullOrEmpty()) {
            properties.remove(key)
        } else {
            properties.setProperty(key, value)
        }
        save()
    }

    protected fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return properties.getProperty(key)?.toBoolean() ?: defaultValue
    }

    protected fun putBoolean(key: String, value: Boolean) {
        properties.setProperty(key, value.toString())
        save()
    }

    protected fun getInt(key: String, defaultValue: Int = 0): Int {
        return properties.getProperty(key)?.toIntOrNull() ?: defaultValue
    }

    protected fun putInt(key: String, value: Int) {
        properties.setProperty(key, value.toString())
        save()
    }

    protected fun getLong(key: String, defaultValue: Long = 0L): Long {
        return properties.getProperty(key)?.toLongOrNull() ?: defaultValue
    }

    protected fun putLong(key: String, value: Long) {
        properties.setProperty(key, value.toString())
        save()
    }

    protected fun removeKey(key: String) {
        if (properties.containsKey(key)) {
            properties.remove(key)
            save()
        }
    }

    /**
     * 清空当前属性文件中的所有配置
     */
    fun clear() {
        properties.clear()
        save()
    }
}