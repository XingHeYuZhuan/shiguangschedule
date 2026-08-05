package com.xingheyuzhuan.shiguangschedule.tool

import org.koin.core.annotation.Single
import java.io.File
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

@Single
actual class SecureCrypto {

    private val transformation = "AES/GCM/NoPadding"
    private val keyStoreType = "PKCS12"
    private val alias = "ShiguangApiCryptoKeyAlias"

    // 保存在软件当前运行目录下的 data 文件夹中
    private val keyStoreFile: File by lazy {
        val appDir = File(System.getProperty("user.dir"), "data")
        if (!appDir.exists()) {
            appDir.mkdirs()
        }
        File(appDir, "keystore.p12")
    }

    private val keyStorePassword = "ShiguangScheduleStorePassword".toCharArray()

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(keyStoreType)
        if (keyStoreFile.exists()) {
            keyStoreFile.inputStream().use { fis ->
                keyStore.load(fis, keyStorePassword)
            }
        } else {
            keyStore.load(null, keyStorePassword)
        }

        if (keyStore.containsAlias(alias)) {
            val entry = keyStore.getEntry(alias, KeyStore.PasswordProtection(keyStorePassword))
            if (entry is KeyStore.SecretKeyEntry) {
                return entry.secretKey
            }
        }

        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256)
        val secretKey = keyGenerator.generateKey()

        val entry = KeyStore.SecretKeyEntry(secretKey)
        keyStore.setEntry(alias, entry, KeyStore.PasswordProtection(keyStorePassword))

        keyStoreFile.outputStream().use { fos ->
            keyStore.store(fos, keyStorePassword)
        }

        return secretKey
    }

    actual fun encrypt(data: String): CryptoResult? {
        if (data.isEmpty()) return null
        return try {
            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

            CryptoResult(
                encryptedData = Base64.getEncoder().encodeToString(encryptedBytes),
                iv = Base64.getEncoder().encodeToString(cipher.iv)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    actual fun decrypt(encryptedData: String, ivString: String): String? {
        if (encryptedData.isEmpty() || ivString.isEmpty()) return null
        return try {
            val cipher = Cipher.getInstance(transformation)
            val ivBytes = Base64.getDecoder().decode(ivString)
            val gcmSpec = GCMParameterSpec(128, ivBytes)

            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), gcmSpec)
            val decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData))
            String(decryptedBytes, Charsets.UTF_8)
                .replace("\u0000", "")
                .trim()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}