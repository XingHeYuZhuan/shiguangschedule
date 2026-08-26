package com.xingheyuzhuan.shiguangschedule.tool

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.interpretObjCPointer
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.objcPtr
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import org.koin.core.annotation.Single
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.base64EncodedStringWithOptions
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemUpdate
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecDuplicateItem
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecRandomDefault
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

private const val KEYCHAIN_SERVICE = "com.xingheyuzhuan.shiguangschedule.securecrypto"
private const val IV_MARKER = "ios-keychain-v1"
private const val TOKEN_BYTES = 32

@Single
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class SecureCrypto {
    actual fun encrypt(data: String): CryptoResult? {
        if (data.isEmpty()) return null

        val account = generateToken() ?: return null
        val passwordData = data.toNSData() ?: return null

        return if (savePassword(account, passwordData)) {
            CryptoResult(encryptedData = account, iv = IV_MARKER)
        } else {
            null
        }
    }

    actual fun decrypt(encryptedData: String, ivString: String): String? {
        if (encryptedData.isEmpty() || ivString.isEmpty()) return null
        if (ivString != IV_MARKER) return null

        return loadPassword(encryptedData)?.toUtf8String()
    }

    private fun generateToken(): String? {
        val bytes = ByteArray(TOKEN_BYTES)
        val status = bytes.usePinned { pinned ->
            SecRandomCopyBytes(kSecRandomDefault, bytes.size.convert(), pinned.addressOf(0))
        }
        if (status != errSecSuccess) return null

        return bytes.toNSData()?.base64EncodedStringWithOptions(0u)
    }

    private fun savePassword(account: String, data: NSData): Boolean {
        val query = keychainQuery(account)
        val addQuery = keychainQuery(account, includeValueData = data)

        val addStatus = try {
            SecItemAdd(addQuery, null)
        } finally {
            CFRelease(addQuery)
        }
        if (addStatus == errSecSuccess) return true

        if (addStatus != errSecDuplicateItem) {
            CFRelease(query)
            return false
        }

        val attributes = keychainAttributes(data)
        val updateStatus = try {
            SecItemUpdate(query, attributes)
        } finally {
            CFRelease(attributes)
            CFRelease(query)
        }
        return updateStatus == errSecSuccess
    }

    private fun loadPassword(account: String): NSData? = memScoped {
        val result = alloc<CFTypeRefVar>()
        val query = keychainLoadQuery(account)

        val status = try {
            SecItemCopyMatching(query, result.ptr)
        } finally {
            CFRelease(query)
        }
        if (status != errSecSuccess) {
            return@memScoped null
        }
        val value = result.ptr.pointed.value ?: return@memScoped null
        interpretObjCPointer<NSData>(value.rawValue)
    }

    private fun keychainQuery(account: String, includeValueData: NSData? = null): CFMutableDictionaryRef =
        createKeychainDictionary().apply {
            CFDictionaryAddValue(this, kSecClass, kSecClassGenericPassword)
            CFDictionaryAddValue(this, kSecAttrService, nsString(KEYCHAIN_SERVICE).asCFValue())
            CFDictionaryAddValue(this, kSecAttrAccount, nsString(account).asCFValue())
            if (includeValueData != null) {
                CFDictionaryAddValue(this, kSecValueData, includeValueData.asCFValue())
            }
        }

    private fun keychainAttributes(data: NSData): CFMutableDictionaryRef =
        createKeychainDictionary().apply {
            CFDictionaryAddValue(this, kSecValueData, data.asCFValue())
        }

    private fun createKeychainDictionary(): CFMutableDictionaryRef {
        val query = CFDictionaryCreateMutable(
            kCFAllocatorDefault,
            0,
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr
        )!!
        return query
    }

    private fun keychainLoadQuery(account: String): CFMutableDictionaryRef {
        val query = keychainQuery(account)
        CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)
        return query
    }

    private fun nsString(value: String): NSString = NSString.create(string = value)

    private fun Any.asCFValue(): CValuesRef<*> = interpretCPointer<CPointed>(objcPtr())!!

    private fun String.toNSData(): NSData? =
        NSString.create(string = this).dataUsingEncoding(NSUTF8StringEncoding)

    private fun ByteArray.toNSData(): NSData? = usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.convert())
    }

    private fun NSData.toUtf8String(): String? {
        val bytes = bytes ?: return null
        return NSString.create(
            bytes = bytes,
            length = length,
            encoding = NSUTF8StringEncoding
        )?.toString()
    }
}