package com.xingheyuzhuan.shiguangschedule.tool

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

enum class UpdatePlatform(val title: String) {
    GITEE("Gitee 镜像 (推荐)"),
    GITHUB("GitHub 官方")
}

sealed class UpdateStatus {
    data class Found(
        val versionName: String,
        val changelog: String,
        val targetUrl: String,
        val isDirectDownload: Boolean
    ) : UpdateStatus()

    data class Latest(val currentVersion: String) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
    data object NotSupported : UpdateStatus()
    data object Checking : UpdateStatus()
    data object Idle : UpdateStatus()
}

@Serializable
data class ApiReleaseResponse(
    @SerialName("tag_name") val tagName: String = "",
    @SerialName("body") val body: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    @SerialName("assets") val assets: List<ApiAsset> = emptyList()
)

@Serializable
data class ApiAsset(
    @SerialName("name") val name: String = "",
    @SerialName("browser_download_url") val downloadUrl: String = ""
)

expect object PlatformUpdateStrategy {
    val isUpdateSupported: Boolean
    fun parseTargetUrl(response: ApiReleaseResponse): String?
    fun openUrl(url: String)
}

/**
 * 添加 @Single 注解，SharedModule 的 @ComponentScan 会自动发现并注册为单例
 */
@Single
class UpdateChecker(
    private val httpClient: HttpClient = defaultHttpClient
) {
    companion object {
        private const val GITHUB_REPO = "XingHeYuZhuan/shiguangschedule"
        private const val GITEE_REPO = "XingHeYuZhuan-gh/shiguangschedule"

        val defaultHttpClient by lazy {
            HttpClient {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        coerceInputValues = true
                    })
                }
            }
        }
    }

    suspend fun checkUpdate(
        platform: UpdatePlatform,
        currentVersionName: String
    ): UpdateStatus = withContext(Dispatchers.IO) {
        if (!PlatformUpdateStrategy.isUpdateSupported) {
            return@withContext UpdateStatus.NotSupported
        }

        try {
            val apiUrl = when (platform) {
                UpdatePlatform.GITHUB -> "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
                UpdatePlatform.GITEE -> "https://gitee.com/api/v5/repos/$GITEE_REPO/releases/latest"
            }

            val response = httpClient.get(apiUrl).body<ApiReleaseResponse>()

            if (response.tagName.isBlank()) {
                return@withContext UpdateStatus.Error("远程数据异常，请更换仓库重试")
            }

            val latestVersion = response.tagName.removePrefix("v").removePrefix("V").trim()
            val currentVersion = currentVersionName.removePrefix("v").removePrefix("V").trim()

            if (!isNewerVersion(latestVersion, currentVersion)) {
                return@withContext UpdateStatus.Latest(currentVersionName)
            }

            val downloadUrl = PlatformUpdateStrategy.parseTargetUrl(response)

            val (targetUrl, isDirectDownload) = if (!downloadUrl.isNullOrEmpty()) {
                Pair(downloadUrl, true)
            } else if (response.tagName.isNotEmpty()) {
                val fallbackTagUrl = when (platform) {
                    UpdatePlatform.GITHUB -> "https://github.com/$GITHUB_REPO/releases/tag/${response.tagName}"
                    UpdatePlatform.GITEE -> "https://gitee.com/$GITEE_REPO/releases/tag/${response.tagName}"
                }
                Pair(fallbackTagUrl, false)
            } else {
                return@withContext UpdateStatus.Error("远程数据异常，请更换仓库重试")
            }

            UpdateStatus.Found(
                versionName = response.tagName,
                changelog = response.body,
                targetUrl = targetUrl,
                isDirectDownload = isDirectDownload
            )

        } catch (_: Exception) {
            UpdateStatus.Error("远程数据异常，请更换仓库重试")
        }
    }

    /**
     * 比较版本号：判断 latest 是否大于 current
     */
    private fun isNewerVersion(latest: String, current: String): Boolean {
        val latestParts = latest.split('.', '-').mapNotNull { it.toIntOrNull() }
        val currentParts = current.split('.', '-').mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    fun launchUpdate(targetUrl: String) {
        PlatformUpdateStrategy.openUrl(targetUrl)
    }
}