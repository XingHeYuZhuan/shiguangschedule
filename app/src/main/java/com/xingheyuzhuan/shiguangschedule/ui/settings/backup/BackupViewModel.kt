package com.xingheyuzhuan.shiguangschedule.ui.settings.backup

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.api.webdav.WebDavConfig
import com.xingheyuzhuan.shiguangschedule.data.repository.ApiConfigRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.AppBackupPackage
import com.xingheyuzhuan.shiguangschedule.data.repository.BackupMeta
import com.xingheyuzhuan.shiguangschedule.data.repository.BackupModule
import com.xingheyuzhuan.shiguangschedule.data.repository.BackupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.core.annotation.KoinViewModel
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupUiState(
    val baseUrl: String = "",
    val username: String = "",
    val rootPath: String = "ShiguangSchedule",
    val hasSavedPassword: Boolean = false,
    val isTesting: Boolean = false,
    val isBusy: Boolean = false,
    val testResult: TestResult = TestResult.Idle
)

sealed interface TestResult {
    data object Idle : TestResult
    data object Success : TestResult
    data class Error(val message: String) : TestResult
}

@KoinViewModel
class BackupViewModel(
    private val context: Application,
    private val apiConfigRepository: ApiConfigRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private var cachedConfig: WebDavConfig? = null
    private val FIXED_BACKUP_DIR = "Backup"

    init {
        viewModelScope.launch {
            apiConfigRepository.webDavConfigFlow.collectLatest { config ->
                cachedConfig = config
                _uiState.update { state ->
                    state.copy(
                        baseUrl = config?.baseUrl ?: "",
                        username = config?.username ?: "",
                        rootPath = config?.rootPath ?: "ShiguangSchedule",
                        hasSavedPassword = !config?.password.isNullOrBlank()
                    )
                }
            }
        }
    }

    fun testWebDavConnection(baseUrl: String, username: String, pwd: String, rootPath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTesting = true, testResult = TestResult.Idle) }
            val finalPassword = if (pwd.isEmpty()) cachedConfig?.password ?: "" else pwd.trim()
            val processedRootPath = rootPath.trim().removeSuffix("/")

            val testConfig = WebDavConfig(baseUrl.trim(), username.trim(), finalPassword, processedRootPath)
            val testClient = apiConfigRepository.createWebDavClient(testConfig)

            val isConnected = testClient?.ensureRootDirectoryExists() ?: false
            testClient?.close()

            if (isConnected) {
                apiConfigRepository.saveWebDavConfig(testConfig)
                _uiState.update { it.copy(isTesting = false, testResult = TestResult.Success) }
            } else {
                _uiState.update {
                    it.copy(
                        isTesting = false,
                        testResult = TestResult.Error(context.getString(R.string.backup_err_connect_failed))
                    )
                }
            }
        }
    }

    fun disconnectWebDav() { viewModelScope.launch { apiConfigRepository.clearWebDavConfig() } }

    fun backupToWebDav() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, testResult = TestResult.Idle) }

            val client = apiConfigRepository.createWebDavClient() ?: run {
                _uiState.update { it.copy(isBusy = false, testResult = TestResult.Error(context.getString(R.string.error_webdav_unconfigured))) }
                return@launch
            }

            val backupPackage = backupRepository.createFullSoftwareBackup(BackupModule.entries)
            if (backupPackage == null) {
                client.close()
                _uiState.update { it.copy(isBusy = false, testResult = TestResult.Error(context.getString(R.string.backup_err_empty))) }
                return@launch
            }

            val success = withContext(Dispatchers.IO) {
                try {
                    val metaFile = File(context.cacheDir, "meta.json")
                    metaFile.writeText(Json.encodeToString(BackupMeta.serializer(), backupPackage.meta))
                    if (!client.uploadFile(metaFile, "$FIXED_BACKUP_DIR/meta.json")) return@withContext false

                    for ((key, bytes) in backupPackage.payloadMap) {
                        val moduleFile = File(context.cacheDir, "$key.cbor")
                        moduleFile.writeBytes(bytes)
                        if (!client.uploadFile(moduleFile, "$FIXED_BACKUP_DIR/$key.cbor")) return@withContext false
                    }
                    true
                } catch (e: Exception) {
                    false
                }
            }

            client.close()
            _uiState.update {
                it.copy(
                    isBusy = false,
                    testResult = if (success) TestResult.Success else TestResult.Error(context.getString(R.string.backup_err_upload_failed))
                )
            }
        }
    }

    fun restoreFromWebDav() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, testResult = TestResult.Idle) }

            val client = apiConfigRepository.createWebDavClient() ?: run {
                _uiState.update { it.copy(isBusy = false, testResult = TestResult.Error(context.getString(R.string.error_webdav_unconfigured))) }
                return@launch
            }

            val result = withContext(Dispatchers.IO) {
                try {
                    val metaFile = File(context.cacheDir, "meta_restore.json")
                    if (!client.downloadFile("$FIXED_BACKUP_DIR/meta.json", metaFile)) {
                        return@withContext Result.failure(Exception(context.getString(R.string.backup_err_corrupted)))
                    }
                    val meta = Json.decodeFromString(BackupMeta.serializer(), metaFile.readText())

                    val payloadMap = mutableMapOf<String, ByteArray>()
                    for (module in meta.modules) {
                        val moduleFile = File(context.cacheDir, "${module.key}_restore.cbor")
                        if (client.downloadFile("$FIXED_BACKUP_DIR/${module.key}.cbor", moduleFile)) {
                            payloadMap[module.key] = moduleFile.readBytes()
                        }
                    }

                    if (payloadMap.isEmpty()) {
                        return@withContext Result.failure(Exception(context.getString(R.string.backup_err_corrupted)))
                    }

                    val availableModules = meta.modules.filter { payloadMap.containsKey(it.key) }
                    val safeMeta = meta.copy(modules = availableModules)

                    val packageObj = AppBackupPackage(safeMeta, payloadMap)
                    backupRepository.restoreFullSoftwareBackup(packageObj)
                } catch (e: Exception) { Result.failure(e) }
            }

            client.close()
            _uiState.update {
                val exception = result.exceptionOrNull()
                it.copy(
                    isBusy = false,
                    testResult = if (result.isSuccess) TestResult.Success else TestResult.Error(
                        context.getString(R.string.backup_err_restore_failed_prefix, exception?.message ?: "")
                    )
                )
            }
        }
    }

    suspend fun exportToLocalZip(outputStream: OutputStream): Boolean = withContext(Dispatchers.IO) {
        _uiState.update { it.copy(isBusy = true, testResult = TestResult.Idle) }

        val backupPackage = backupRepository.createFullSoftwareBackup(BackupModule.entries)
        if (backupPackage == null) {
            _uiState.update { it.copy(isBusy = false, testResult = TestResult.Error(context.getString(R.string.backup_err_empty))) }
            return@withContext false
        }

        try {
            ZipOutputStream(outputStream).use { zos ->
                val metaJson = Json.encodeToString(BackupMeta.serializer(), backupPackage.meta)
                zos.putNextEntry(ZipEntry("meta.json"))
                zos.write(metaJson.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                for ((key, bytes) in backupPackage.payloadMap) {
                    zos.putNextEntry(ZipEntry("$key.cbor"))
                    zos.write(bytes)
                    zos.closeEntry()
                }
            }
            _uiState.update { it.copy(isBusy = false, testResult = TestResult.Success) }
            true
        } catch (e: Exception) {
            _uiState.update { it.copy(isBusy = false, testResult = TestResult.Error(context.getString(R.string.backup_err_local_export_failed))) }
            false
        }
    }

    suspend fun importFromLocalZip(inputStream: InputStream): Boolean = withContext(Dispatchers.IO) {
        _uiState.update { it.copy(isBusy = true, testResult = TestResult.Idle) }

        try {
            var meta: BackupMeta? = null
            val payloadMap = mutableMapOf<String, ByteArray>()

            ZipInputStream(inputStream).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val entryPath = entry.name
                        if (!entryPath.contains("/")) {
                            when {
                                entryPath == "meta.json" -> {
                                    val content = zis.readBytes()
                                    meta = Json.decodeFromString(BackupMeta.serializer(), String(content, Charsets.UTF_8))
                                }
                                entryPath.endsWith(".cbor") -> {
                                    val key = entryPath.removeSuffix(".cbor")
                                    payloadMap[key] = zis.readBytes()
                                }
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            val finalMeta = meta ?: throw Exception(context.getString(R.string.backup_err_corrupted))

            if (payloadMap.isEmpty()) {
                throw Exception(context.getString(R.string.backup_err_corrupted))
            }

            val availableModules = finalMeta.modules.filter { payloadMap.containsKey(it.key) }
            val safeMeta = finalMeta.copy(modules = availableModules)

            val packageObj = AppBackupPackage(safeMeta, payloadMap)
            backupRepository.restoreFullSoftwareBackup(packageObj).getOrThrow()

            _uiState.update { it.copy(isBusy = false, testResult = TestResult.Success) }
            true
        } catch (e: Exception) {
            _uiState.update {
                val errMsg = e.message ?: ""
                it.copy(
                    isBusy = false,
                    testResult = TestResult.Error(context.getString(R.string.backup_err_restore_failed_prefix, errMsg))
                )
            }
            false
        }
    }
}