package com.xingheyuzhuan.shiguangschedule.tool

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import com.xingheyuzhuan.shiguangschedule.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/** 更新渠道信息 */
data class UpdateChannel(val id: String, val title: String, val url: String)

/** 更新检查结果状态 */
sealed class UpdateStatus {
    data class Found(val flavorInfo: FlavorUpdateInfo, val downloadUrl: String) : UpdateStatus()
    data class Latest(val versionName: String) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
    object Checking : UpdateStatus()
    object Idle : UpdateStatus()
}

/** 远程更新索引的根结构 */
@Serializable data class UpdateIndex(val prod: FlavorUpdateInfo, val dev: FlavorUpdateInfo)
@Serializable
data class FlavorUpdateInfo(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val changelog: String = "",
    val downloadLinks: Map<String, String>
)


class UpdateChecker(private val context: Context) {

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        // 远程更新索引文件的 URL
        private const val GITHUB_INDEX_URL = "https://raw.githubusercontent.com/XingHeYuZhuan/test_update/refs/heads/main/update/update_info_github.json"
        private const val GITEE_INDEX_URL = "https://gitee.com/XingHeYuZhuan-gh/test_update/raw/main/update/update_info_gitee.json"

        // 默认更新渠道
        val DEFAULT_PLATFORM_URL = GITEE_INDEX_URL

        // 可供选择的更新渠道列表
        val UPDATE_CHANNELS = listOf(
            UpdateChannel("gitee", "Gitee 镜像 (推荐)", GITEE_INDEX_URL),
            UpdateChannel("github", "GitHub 官方", GITHUB_INDEX_URL)
        )
    }

    private val httpClient = OkHttpClient.Builder().build()
    private val currentFlavorId = BuildConfig.CURRENT_FLAVOR_ID
    private val currentVersionCode = BuildConfig.VERSION_CODE

    /** 使用外部浏览器启动下载链接 */
    fun launchExternalDownload(downloadUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW, downloadUrl.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
        }
    }

    /** 获取设备支持的 ABI，用于匹配下载链接 */
    private fun getDeviceAbi(): String {
        val supportedAbis = Build.SUPPORTED_ABIS

        val supportedSplits = setOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
        return supportedAbis.firstOrNull { it in supportedSplits } ?: "universal"
    }

    /** 检查是否有新版本可用 */
    suspend fun checkUpdate(platformUrl: String): UpdateStatus = withContext(Dispatchers.Default) {
        try {
            // 下载并解析 JSON
            val request = Request.Builder().url(platformUrl).build()
            val indexJson = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("下载失败")
                response.body.string()
            }

            // 获取当前 Flavor 信息
            val updateIndex = json.decodeFromString<UpdateIndex>(indexJson)
            val flavorInfo = when (currentFlavorId) {
                "prod" -> updateIndex.prod
                "dev" -> updateIndex.dev
                else -> throw IllegalStateException("未知 Flavor ID")
            }

            // 比较版本代码
            if (flavorInfo.latestVersionCode <= currentVersionCode) {
                return@withContext UpdateStatus.Latest(BuildConfig.VERSION_NAME)
            }

            // 匹配设备 ABI
            val deviceAbi = getDeviceAbi()
            val finalDownloadUrl = flavorInfo.downloadLinks[deviceAbi]
                ?: flavorInfo.downloadLinks["universal"]
                ?: throw IllegalStateException("未找到适合 ABI 的下载链接")

            return@withContext UpdateStatus.Found(flavorInfo, finalDownloadUrl)

        } catch (e: Exception) {
            return@withContext UpdateStatus.Error("检查更新失败: ${e.message ?: "未知错误"}")
        }
    }
}