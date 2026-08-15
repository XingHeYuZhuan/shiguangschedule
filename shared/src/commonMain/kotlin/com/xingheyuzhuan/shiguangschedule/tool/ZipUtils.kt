package com.xingheyuzhuan.shiguangschedule.tool

/**
 * 跨平台 Zip 压缩工具类
 */
expect object ZipUtils {
    /**
     * 将文件映射打包为 ZIP 格式字节数组
     *
     * @param entries 文件路径/名称 -> 对应的文件字节数组（例: "meta.json" to bytes）
     * @return 导出的 ZIP 压缩包二进制数据
     */
    fun createZip(entries: Map<String, ByteArray>): ByteArray
}