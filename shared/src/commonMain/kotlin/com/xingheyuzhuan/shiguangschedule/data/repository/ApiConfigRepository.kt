package com.xingheyuzhuan.shiguangschedule.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.xingheyuzhuan.shiguangschedule.data.api.webdav.WebDavClient
import com.xingheyuzhuan.shiguangschedule.data.api.webdav.WebDavConfig
import com.xingheyuzhuan.shiguangschedule.tool.SecureCrypto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

/**
 * 全局 API 配置持久化中心仓库（KMP 共享层）
 */
@Single
class ApiConfigRepository(
    @Named("ApiConfig") private val dataStore: DataStore<Preferences>,
    private val secureCrypto: SecureCrypto
) {

    /**
     * 所有的 API 存储 Key 划分严格的边界线
     */
    object ApiKeys {

        /** WebDAV 备份 business line 命名空间 */
        object WebDav {
            private const val PREFIX = "webdav_"
            val BASE_URL = stringPreferencesKey("${PREFIX}base_url")
            val USERNAME = stringPreferencesKey("${PREFIX}username")
            val ROOT_PATH = stringPreferencesKey("${PREFIX}root_path")
            val ENCRYPTED_PASSWORD = stringPreferencesKey("${PREFIX}encrypted_pwd")
            val CRYPTO_IV = stringPreferencesKey("${PREFIX}crypto_iv")
        }
    }

    /**
     * 响应式流：实时观察 WebDAV 的完整配置状态
     */
    val webDavConfigFlow: Flow<WebDavConfig?> = dataStore.data.map { preferences ->
        val baseUrl = preferences[ApiKeys.WebDav.BASE_URL]
        val username = preferences[ApiKeys.WebDav.USERNAME]
        val rootPath = preferences[ApiKeys.WebDav.ROOT_PATH] ?: "ShiguangSchedule"
        val encryptedPassword = preferences[ApiKeys.WebDav.ENCRYPTED_PASSWORD]
        val ivString = preferences[ApiKeys.WebDav.CRYPTO_IV]

        if (!baseUrl.isNullOrBlank() && !username.isNullOrBlank() &&
            !encryptedPassword.isNullOrBlank() && !ivString.isNullOrBlank()
        ) {
            val decryptedPassword = secureCrypto.decrypt(encryptedPassword, ivString)

            if (decryptedPassword != null) {
                WebDavConfig(
                    baseUrl = baseUrl,
                    username = username,
                    password = decryptedPassword,
                    rootPath = rootPath
                )
            } else {
                null
            }
        } else {
            null
        }
    }

    /**
     * 保存或更新 WebDAV 配置
     */
    suspend fun saveWebDavConfig(config: WebDavConfig) {
        val cryptoResult = secureCrypto.encrypt(config.password) ?: return

        dataStore.edit { preferences ->
            preferences[ApiKeys.WebDav.BASE_URL] = config.baseUrl.trim()
            preferences[ApiKeys.WebDav.USERNAME] = config.username.trim()
            preferences[ApiKeys.WebDav.ROOT_PATH] = config.rootPath.trim()
            preferences[ApiKeys.WebDav.ENCRYPTED_PASSWORD] = cryptoResult.encryptedData
            preferences[ApiKeys.WebDav.CRYPTO_IV] = cryptoResult.iv
        }
    }

    /**
     * 清除 WebDAV 配置
     */
    suspend fun clearWebDavConfig() {
        dataStore.edit { preferences ->
            preferences.remove(ApiKeys.WebDav.BASE_URL)
            preferences.remove(ApiKeys.WebDav.USERNAME)
            preferences.remove(ApiKeys.WebDav.ROOT_PATH)
            preferences.remove(ApiKeys.WebDav.ENCRYPTED_PASSWORD)
            preferences.remove(ApiKeys.WebDav.CRYPTO_IV)
        }
    }

    /**
     * 传输引擎工厂：动态创建一套可用的 WebDavClient
     */
    suspend fun createWebDavClient(explicitConfig: WebDavConfig? = null): WebDavClient? {
        val finalConfig = explicitConfig ?: webDavConfigFlow.firstOrNull()
        return finalConfig?.let {
            WebDavClient(config = it)
        }
    }
}