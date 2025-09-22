package com.xingheyuzhuan.shiguangschedule.data

import com.google.gson.annotations.SerializedName
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.io.IOException

/**
 * 这是一个数据类，用于表示 API 返回的 JSON 数据。
 * 对应于 API 返回的外部 JSON 结构。
 */
data class ApiResponse(
    @SerializedName("holiday")
    val holidays: Map<String, HolidayInfo>
)

/**
 * 这是一个数据类，用于表示 JSON 中每个日期的假期信息。
 */
data class HolidayInfo(
    @SerializedName("date")
    val date: String,
    @SerializedName("holiday")
    val isHoliday: Boolean
)

/**
 * 这是 Retrofit 的 API 接口，用于定义网络请求。
 */
interface SkippedDatesApiService {
    @GET("year")
    suspend fun getHolidays(): ApiResponse
}

/**
 * 这是一个单例对象，包含所有与 API 相关的逻辑。
 */
object ApiDateImporter {
    private const val BASE_URL = "https://timor.tech/api/holiday/"

    private fun createOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val newRequest = originalRequest.newBuilder()
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 10; SM-G973F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36"
                    )
                    .build()
                chain.proceed(newRequest)
            }
            .addInterceptor(loggingInterceptor)
            .build()
    }

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(createOkHttpClient())
        .build()

    private val apiService: SkippedDatesApiService =
        retrofit.create(SkippedDatesApiService::class.java)

    /**
     * 从 API 获取跳过的日期（假期），并保存到 AppSettingsRepository 中。
     *
     * @param appSettingsRepository AppSettingsRepository 实例
     */
    suspend fun importAndSaveSkippedDates(appSettingsRepository: AppSettingsRepository) {
        try {
            val response = apiService.getHolidays()

            val skippedDates = response.holidays.values
                .filter { it.isHoliday }
                .map { it.date }
                .toSet()

            val currentSettings = appSettingsRepository.getAppSettings().first()
            val updatedSettings = currentSettings.copy(skippedDates = skippedDates)
            appSettingsRepository.insertOrUpdateAppSettings(updatedSettings)

            println("成功导入并保存了 ${skippedDates.size} 个跳过的日期。")
        } catch (e: IOException) {
            println("网络请求失败：${e.message}")
            e.printStackTrace()
        } catch (e: Exception) {
            println("导入跳过的日期时出错：${e.message}")
            e.printStackTrace()
        }
    }
}
