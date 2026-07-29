package com.xingheyuzhuan.shiguangschedule.data.repository

import android.content.Context
import com.xingheyuzhuan.shiguangschedule.data.model.ContributionList
import kotlinx.serialization.json.Json
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

/**
 * 贡献者数据仓库
 * 职责：直接处理 Asset 文件 I/O 和 Kotlinx Serialization 解析。
 */
@Single
class ContributionRepository(
    private val context: Context
) {

    // 明确的 Asset 文件路径常量
    private val ASSET_FILE_PATH = "contributors_data/contributors.json"

    // 建议复用 Json 实例，配置 ignoreUnknownKeys 以增强容错性
    private val json = Json {
        ignoreUnknownKeys = true
    }

    /**
     * 从 Asset 文件读取贡献者 JSON 数据并进行反序列化。
     *
     * @return 成功解析后的 ContributionList 对象，失败则抛出 IOException。
     */
    suspend fun getContributions(): ContributionList {

        // 将文件读取和 JSON 解析切换到 IO 调度器上执行
        return withContext(Dispatchers.IO) {
            val jsonString: String
            try {
                context.assets.open(ASSET_FILE_PATH).use { inputStream ->
                    jsonString = inputStream.bufferedReader().use { it.readText() }
                }
            } catch (ioException: IOException) {
                throw IOException("无法从 Asset 文件加载贡献者数据: $ASSET_FILE_PATH", ioException)
            }

            try {
                return@withContext json.decodeFromString<ContributionList>(jsonString)
            } catch (e: Exception) {
                throw IOException("解析贡献者 JSON 数据出错: ${e.message}", e)
            }
        }
    }
}