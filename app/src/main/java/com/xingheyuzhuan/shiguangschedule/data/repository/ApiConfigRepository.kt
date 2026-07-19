package com.xingheyuzhuan.shiguangschedule.data.repository

import android.content.Context
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.xingheyuzhuan.shiguangschedule.data.api.webdav.WebDavClient
import com.xingheyuzhuan.shiguangschedule.data.api.webdav.WebDavConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.koin.core.annotation.Single
import org.koin.core.annotation.Named

/**
 * 全局 API 配置持久化中心仓库
 */
@Single
class ApiConfigRepository(
    private val context: Context,
    @Named("ApiConfig") private val dataStore: DataStore<Preferences>
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
            val decryptedPassword = decrypt(encryptedPassword, ivString)

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
        val cryptoResult = encrypt(config.password) ?: return

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

    data class CryptoResult(val encryptedData: String, val iv: String)

    private val alias = "ShiguangApiCryptoKeyAlias"
    private val provider = "AndroidKeyStore"
    private val transformation = "AES/GCM/NoPadding"

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(provider).apply { load(null) }
        keyStore.getKey(alias, null)?.let { return it as SecretKey }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, provider
        )
        val spec = android.security.keystore.KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /**
     * 通用加密函数
     */
    fun encrypt(data: String): CryptoResult? {
        if (data.isEmpty()) return null
        return try {
            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

            CryptoResult(
                encryptedData = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP),
                iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 通用解密函数
     */
    fun decrypt(encryptedData: String, ivString: String): String? {
        if (encryptedData.isEmpty() || ivString.isEmpty()) return null
        return try {
            val cipher = Cipher.getInstance(transformation)
            val ivBytes = Base64.decode(ivString, Base64.NO_WRAP)
            val gcmSpec = GCMParameterSpec(128, ivBytes)

            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), gcmSpec)
            val decryptedBytes = cipher.doFinal(Base64.decode(encryptedData, Base64.NO_WRAP))
            String(decryptedBytes, Charsets.UTF_8)
                .replace("\u0000", "")
                .trim()
        } catch (e: Exception) {
            null
        }
    }
}