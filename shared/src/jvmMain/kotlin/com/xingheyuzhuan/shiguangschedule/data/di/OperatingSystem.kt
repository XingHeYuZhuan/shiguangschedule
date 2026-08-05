package com.xingheyuzhuan.shiguangschedule.data.di

/**
 * 操作系统类型枚举与当前系统判断工具
 */
enum class OperatingSystem {
    WINDOWS,
    MACOS,
    LINUX,
    UNKNOWN;

    companion object {
        /**
         * 获取当前运行环境的操作系统类型（使用 lazy 惰性加载，仅首次读取时解析一次）
         */
        val current: OperatingSystem by lazy {
            val name = System.getProperty("os.name").lowercase()
            when {
                name.contains("win") -> WINDOWS
                name.contains("mac") || name.contains("darwin") -> MACOS
                name.contains("nux") || name.contains("nix") -> LINUX
                else -> UNKNOWN
            }
        }
    }
}