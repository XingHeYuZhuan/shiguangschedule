package com.xingheyuzhuan.shiguangschedule.data.api.webdav

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.cio.*
import io.ktor.utils.io.jvm.javaio.*
import java.io.File

/**
 * WebDAV 协议通信客户端
 * 职责：处理物理文件与目录的传输，支持多级目录级联创建。
 */
class WebDavClient(
    private val config: WebDavConfig
) {

    private val client by lazy {
        HttpClient {
            install(Auth) {
                basic {
                    credentials {
                        BasicAuthCredentials(username = config.username, password = config.password)
                    }
                    sendWithoutRequest { true }
                }
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30000
                connectTimeoutMillis = 15000
            }
        }
    }

    private val normalizedBaseUrl: String
        get() = if (config.baseUrl.endsWith("/")) config.baseUrl else "${config.baseUrl}/"

    /**
     * 拼接完整的 WebDAV 资源 URL
     */
    private fun buildFullUrl(relativePath: String): String {
        val root = config.getCleanRootPath()
        val relative = relativePath.trim('/')
        return "$normalizedBaseUrl$root$relative"
    }

    /**
     * 验证并确保用户配置的根目录存在
     */
    suspend fun ensureRootDirectoryExists(): Boolean {
        val rootDir = config.getCleanRootPath().trim('/')
        return ensureRemoteDirChainExists(rootDir)
    }

    /**
     * 级联创建远端目录链
     * @param relativeDirChain 相对目录路径（例如："dir1/dir2"）
     */
    private suspend fun ensureRemoteDirChainExists(relativeDirChain: String): Boolean {
        if (relativeDirChain.isEmpty()) return true

        val pathSegments = relativeDirChain.split('/').filter { it.isNotEmpty() }
        var currentPath = ""

        try {
            for (segment in pathSegments) {
                currentPath = if (currentPath.isEmpty()) segment else "$currentPath/$segment"
                // 部分 WebDAV 服务器强制要求 MKCOL 请求的 URL 必须以 '/' 结尾
                val fullUrl = "$normalizedBaseUrl$currentPath/"

                val response = client.request(fullUrl) {
                    method = HttpMethod("MKCOL")
                }

                // 201: 创建成功; 405: 目录已存在（均视为有效目标）
                val isSuccess = response.status == HttpStatusCode.Created ||
                        response.status == HttpStatusCode.MethodNotAllowed

                if (!isSuccess) return false
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * 上传本地文件 (PUT)
     */
    suspend fun uploadFile(localFile: File, remoteFileName: String): Boolean {
        if (!localFile.exists()) return false

        // 解析并保障目标文件所需的完整父级目录链
        val rootPrefix = config.getCleanRootPath()
        val fileRelativeDir = remoteFileName.substringBeforeLast('/', "")

        val fullRelativeDirChain = if (fileRelativeDir.isEmpty()) {
            rootPrefix.trim('/')
        } else {
            "${rootPrefix.trim('/')}/${fileRelativeDir.trim('/')}"
        }

        if (!ensureRemoteDirChainExists(fullRelativeDirChain)) return false

        return try {
            val fullUrl = buildFullUrl(remoteFileName)
            val response = client.put(fullUrl) {
                setBody(localFile.readChannel())
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 下载远端文件 (GET)
     */
    suspend fun downloadFile(remoteFileName: String, targetLocalFile: File): Boolean {
        return try {
            val fullUrl = buildFullUrl(remoteFileName)
            val response = client.get(fullUrl)

            if (response.status.isSuccess()) {
                val byteChannel = response.bodyAsChannel()
                targetLocalFile.outputStream().use { outputStream ->
                    byteChannel.copyTo(outputStream)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun close() = client.close()
}