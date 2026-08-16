package com.xingheyuzhuan.shiguangschedule.data.api.webdav

/**
 * WebDAV 基础认证及路径配置
 * @param baseUrl 服务器基准地址
 * @param username 账号
 * @param password 应用独立密码
 * @param rootPath 备份的根路径设置
 */
data class WebDavConfig(
    val baseUrl: String,
    val username: String,
    val password: SenderPassword,
    val rootPath: String = "ShiguangSchedule"
) {
    /**
     * 自动化处理根路径的斜杠，确保返回的形式
     * 避免业务层拼接时出现 "//" 或漏掉 "/" 导致 URL 解析失败
     */
    fun getCleanRootPath(): String {
        val trimmed = rootPath.trim().trim('/')
        return if (trimmed.isEmpty()) "" else "$trimmed/"
    }
}

typealias SenderPassword = String