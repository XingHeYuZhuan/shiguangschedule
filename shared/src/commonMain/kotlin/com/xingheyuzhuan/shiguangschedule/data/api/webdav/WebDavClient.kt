package com.xingheyuzhuan.shiguangschedule.data.api.webdav

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.readAvailable
import okio.FileSystem
import okio.Path
import okio.SYSTEM

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

    private fun buildFullUrl(relativePath: String): String {
        val root = config.getCleanRootPath()
        val relative = relativePath.trim('/')
        return "$normalizedBaseUrl$root$relative"
    }

    suspend fun ensureRootDirectoryExists(): Boolean {
        val rootDir = config.getCleanRootPath().trim('/')
        return ensureRemoteDirChainExists(rootDir)
    }

    private suspend fun ensureRemoteDirChainExists(relativeDirChain: String): Boolean {
        if (relativeDirChain.isEmpty()) return true

        val pathSegments = relativeDirChain.split('/').filter { it.isNotEmpty() }
        var currentPath = ""

        try {
            for (segment in pathSegments) {
                currentPath = if (currentPath.isEmpty()) segment else "$currentPath/$segment"
                val fullUrl = "$normalizedBaseUrl$currentPath/"

                val response = client.request(fullUrl) {
                    method = HttpMethod("MKCOL")
                }

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
    suspend fun uploadFile(localPath: Path, remoteFileName: String): Boolean {
        if (!FileSystem.SYSTEM.exists(localPath)) return false

        val rootPrefix = config.getCleanRootPath()
        val fileRelativeDir = remoteFileName.substringBeforeLast('/', "")

        val fullRelativeDirChain = if (fileRelativeDir.isEmpty()) {
            rootPrefix.trim('/')
        } else {
            "${rootPrefix.trim('/')}/${fileRelativeDir.trim('/')}"
        }

        if (!ensureRemoteDirChainExists(fullRelativeDirChain)) return false

        return try {
            val bytes = FileSystem.SYSTEM.read(localPath) { readByteArray() }
            val fullUrl = buildFullUrl(remoteFileName)
            val response = client.put(fullUrl) {
                setBody(bytes)
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 下载远端文件 (GET)
     */
    suspend fun downloadFile(remoteFileName: String, targetLocalPath: Path): Boolean {
        return try {
            val fullUrl = buildFullUrl(remoteFileName)
            val response = client.get(fullUrl)

            if (response.status.isSuccess()) {
                val byteChannel = response.bodyAsChannel()
                val buffer = ByteArray(8192)
                FileSystem.SYSTEM.write(targetLocalPath) {
                    while (!byteChannel.isClosedForRead) {
                        val read = byteChannel.readAvailable(buffer)
                        if (read <= 0) break
                        write(buffer, 0, read)
                    }
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