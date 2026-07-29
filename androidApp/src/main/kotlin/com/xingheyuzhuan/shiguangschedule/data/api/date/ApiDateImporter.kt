package com.xingheyuzhuan.shiguangschedule.data.api.date

import com.xingheyuzhuan.shiguangschedule.BuildConfig
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ApiResponse(
    @SerialName("holiday")
    val holidays: Map<String, HolidayInfo>
)

@Serializable
data class HolidayInfo(
    @SerialName("date")
    val date: String,
    @SerialName("holiday")
    val isHoliday: Boolean
)

/**
 * API 导入对象，基于 Ktor 3.0 实现。
 */
object ApiDateImporter {
    private const val BASE_URL = "https://timor.tech/api/holiday/year"

    private val client = HttpClient {
        // 动态日志配置：Debug 模式开启，Release 模式彻底关闭
        install(Logging) {
            level = if (BuildConfig.DEBUG) LogLevel.INFO else LogLevel.NONE
            logger = Logger.DEFAULT
        }

        // 序列化配置
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }

        // 默认请求配置
        defaultRequest {
            url(BASE_URL)
            header("User-Agent", "Mozilla/5.0 (Linux; Android 10; SM-G973F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36")
        }

        // 4. 超时处理（增强健壮性）
        install(HttpTimeout) {
            requestTimeoutMillis = 15000
            connectTimeoutMillis = 15000
        }
    }

    /**
     * 从 API 获取跳过的日期（假期），并保存到 AppSettingsRepository 中。
     */
    suspend fun importAndSaveSkippedDates(appSettingsRepository: AppSettingsRepository) {
        try {
            // Ktor 直接发起请求并解析
            val response: ApiResponse = client.get("").body()

            val skippedDates = response.holidays.values
                .filter { it.isHoliday }
                .map { it.date }
                .toSet()

            val currentSettings = appSettingsRepository.getAppSettings().first()
            val updatedSettings = currentSettings.copy(skippedDates = skippedDates)
            appSettingsRepository.insertOrUpdateAppSettings(updatedSettings)

            if (BuildConfig.DEBUG) {
                println("成功导入并保存了 ${skippedDates.size} 个跳过的日期。")
            }
        } catch (e: Exception) {
            // Ktor 将网络错误、解析错误统一通过 Exception 抛出
            if (BuildConfig.DEBUG) {
                println("数据导入失败: ${e.localizedMessage}")
                e.printStackTrace()
            }
        }
    }

    /**
     * 在 App 进程结束时可手动关闭连接池（可选）
     */
    fun close() = client.close()
}