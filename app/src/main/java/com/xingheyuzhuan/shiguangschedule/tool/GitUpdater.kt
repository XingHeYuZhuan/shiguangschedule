package com.xingheyuzhuan.shiguangschedule.tool

import android.content.Context
import com.xingheyuzhuan.shiguangschedule.data.model.RepoType
import com.xingheyuzhuan.shiguangschedule.data.model.RepositoryInfo
import com.xingheyuzhuan.kgit.Ext
import com.xingheyuzhuan.kgit.logging.ProgressMonitor
import okio.FileSystem
import okio.Path.Companion.toPath
import org.koin.core.annotation.Single
import school_index.SchoolIndex
import java.io.File

/**
 * 精简的单行对齐进度监听器
 */
private class LogProgressMonitor(
    private val stepPrefix: String,
    private val onLog: (String) -> Unit
) : ProgressMonitor {

    private var totalWork = ProgressMonitor.UNKNOWN
    private var currentWork = 0

    override fun beginTask(title: String, totalWork: Int) {
        this.totalWork = totalWork
        this.currentWork = 0
        renderProgress()
    }

    override fun update(completedWork: Int) {
        currentWork += completedWork
        renderProgress()
    }

    override fun endTask() {
        // 子任务结束时不发送换行，保持同一行刷新的连贯性
    }

    private fun renderProgress() {
        val logLine = if (totalWork > 0) {
            val percent = (currentWork * 100 / totalWork).coerceIn(0, 100)
            "\r$stepPrefix ➜ 同步中 $percent%"
        } else {
            "\r$stepPrefix ➜ 处理中..."
        }
        onLog(logLine.padEnd(50, ' '))
    }
}

/**
 * GitUpdater
 * 轻量工作区源码下载、协议版本校验和时间戳版本去重。
 */
@Single
class GitUpdater(
    private val context: Context
) {

    private val CLIENT_PROTOCOL_VERSION: Int = 1

    private val baseLocalDir: File
        get() = File(context.filesDir, "repo")
    private val indexFileTargetDir: File
        get() = File(baseLocalDir, "index")
    private val schoolsFileTargetDir: File
        get() = File(baseLocalDir, "schools")

    private data class GitUpdateResult(
        var indexFileContent: ByteArray? = null,
        var indexRemoteVersionId: String? = null,
        var resourceFiles: List<Pair<File, File>> = emptyList(),
        var isFatalIndexError: Boolean = false
    )

    private fun readSchoolIndex(file: File): SchoolIndex? {
        if (!file.exists()) return null
        return try {
            file.inputStream().use { stream ->
                SchoolIndex.ADAPTER.decode(stream)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun isNewerVersionId(newVersionId: String?, localVersionId: String?): Boolean {
        if (newVersionId.isNullOrBlank()) return false
        if (localVersionId.isNullOrBlank()) return true
        return newVersionId > localVersionId
    }

    private fun extractToken(repoInfo: RepositoryInfo): String? {
        if (repoInfo.repoType != RepoType.PRIVATE_REPO && repoInfo.credentials.isNullOrEmpty()) {
            return null
        }
        val password = repoInfo.credentials?.get("password")
        val username = repoInfo.credentials?.get("username")
        return password?.takeIf { it.isNotBlank() } ?: username?.takeIf { it.isNotBlank() }
    }

    /**
     * 异常解析：非配置异常统一归为连接中断
     */
    private fun parseNetworkErrorMessage(e: Exception): String {
        val fullErr = ((e.message ?: "") + " " + (e.cause?.message ?: "")).lowercase()

        return when {
            // 1. 权限与凭证配置问题
            fullErr.contains("401") || fullErr.contains("403") || fullErr.contains("not authorized") || fullErr.contains("authentication") ->
                "身份验证失败，请检查凭证配置"

            // 2. 仓库或分支不存在 (URL/Branch 配置问题)
            fullErr.contains("404") || fullErr.contains("not found") ->
                "仓库或分支不存在，请检查配置"

            // 3. 其他所有网络、超时、断连等非配置异常，统一提示更换仓库
            else -> "连接中断，请更换仓库"
        }
    }

    /**
     * 【步骤一】下载资源工作区文件
     */
    private suspend fun updateResourceFiles(
        repoInfo: RepositoryInfo,
        token: String?,
        onLog: (String) -> Unit,
        result: GitUpdateResult
    ): Boolean {
        val RESOURCES_PATH = "resources"
        val tempSchoolsRepoDir = File(context.cacheDir, "temp_schools_repo")
        val progressMonitor = LogProgressMonitor("[1/3]", onLog)

        onLog("[1/3] ➜ 开始拉取资源库 (${repoInfo.branch})\n")

        try {
            if (tempSchoolsRepoDir.exists()) {
                tempSchoolsRepoDir.deleteRecursively()
            }

            val downloadCmd = Ext.downloadRepository()
                .setUri(repoInfo.url)
                .setDirectory(tempSchoolsRepoDir.absolutePath.toPath())
                .setBranch(repoInfo.branch)
                .setProgressMonitor(progressMonitor)

            if (!token.isNullOrEmpty()) {
                downloadCmd.setToken(token)
            }

            downloadCmd.call(FileSystem.SYSTEM)

            val sourceResourcesDir = File(tempSchoolsRepoDir, RESOURCES_PATH)
            if (!sourceResourcesDir.exists() || !sourceResourcesDir.isDirectory) {
                onLog("\r[1/3] ✖ 错误：未找到 '${RESOURCES_PATH}' 目录\n")
                return false
            }

            val tempFiles = mutableListOf<Pair<File, File>>()
            sourceResourcesDir.walkTopDown().forEach { sourceFile ->
                if (sourceFile.isFile) {
                    if (sourceFile.name.equals("adapters.yaml", ignoreCase = true)) return@forEach
                    val relativePath = sourceFile.relativeTo(sourceResourcesDir)
                    val targetFile = File(File(schoolsFileTargetDir, RESOURCES_PATH), relativePath.path)
                    tempFiles.add(Pair(sourceFile, targetFile))
                }
            }

            result.resourceFiles = tempFiles
            val successMsg = "\r[1/3] ✔ 资源库同步完成 (${tempFiles.size}个文件)\n"
            onLog(successMsg.padEnd(50, ' '))
            return true

        } catch (e: Exception) {
            val friendlyError = parseNetworkErrorMessage(e)
            val errorMsg = "\r[1/3] ✖ 失败：$friendlyError\n"
            onLog(errorMsg.padEnd(50, ' '))
            return false
        }
    }

    /**
     * 【步骤二】下载并校验索引文件
     */
    private suspend fun downloadIndexFile(
        repoInfo: RepositoryInfo,
        token: String?,
        onLog: (String) -> Unit,
        result: GitUpdateResult
    ) {
        val INDEX_BRANCH = "index-pb-release"
        val INDEX_FILE_NAME = "school_index.pb"
        val tempIndexRepoDir = File(context.cacheDir, "temp_index_repo")
        val progressMonitor = LogProgressMonitor("[2/3]", onLog)

        onLog("[2/3] ➜ 开始校验数据索引...\n")

        try {
            if (tempIndexRepoDir.exists()) tempIndexRepoDir.deleteRecursively()

            val downloadCmd = Ext.downloadRepository()
                .setUri(repoInfo.url)
                .setDirectory(tempIndexRepoDir.absolutePath.toPath())
                .setBranch(INDEX_BRANCH)
                .setProgressMonitor(progressMonitor)

            if (!token.isNullOrEmpty()) {
                downloadCmd.setToken(token)
            }

            downloadCmd.call(FileSystem.SYSTEM)

            val sourceFile = File(tempIndexRepoDir, INDEX_FILE_NAME)
            if (!sourceFile.exists()) {
                val warnMsg = "\r[2/3] ⚠ 未找到远程索引，维持本地索引\n"
                onLog(warnMsg.padEnd(50, ' '))
                return
            }

            val remoteIndex = readSchoolIndex(sourceFile)
            if (remoteIndex == null) {
                val errorMsg = "\r[2/3] ✖ 远程索引解析失败\n"
                onLog(errorMsg.padEnd(50, ' '))
                return
            }

            val remoteProtocol = remoteIndex.protocol_version
            if (remoteProtocol > CLIENT_PROTOCOL_VERSION) {
                val fatalMsg = "\r[2/3] ✖ 协议不兼容，请升级 App\n"
                onLog(fatalMsg.padEnd(50, ' '))
                result.isFatalIndexError = true
                return
            }

            val localIndex = readSchoolIndex(File(indexFileTargetDir, INDEX_FILE_NAME))
            val localVersionId = localIndex?.version_id

            if (isNewerVersionId(remoteIndex.version_id, localVersionId)) {
                val okMsg = "\r[2/3] ✔ 发现新版本索引 (${remoteIndex.version_id})\n"
                onLog(okMsg.padEnd(50, ' '))
                result.indexFileContent = sourceFile.readBytes()
                result.indexRemoteVersionId = remoteIndex.version_id
            } else if (remoteIndex.version_id == localVersionId) {
                val okMsg = "\r[2/3] ✔ 索引已是最新 ($localVersionId)\n"
                onLog(okMsg.padEnd(50, ' '))
            } else {
                val errorMsg = "\r[2/3] ✖ 远程索引版本异常，终止更新\n"
                onLog(errorMsg.padEnd(50, ' '))
                result.isFatalIndexError = true
                return
            }

        } catch (e: Exception) {
            val friendlyError = parseNetworkErrorMessage(e)
            val warnMsg = "\r[2/3] ⚠ 跳过索引更新：$friendlyError\n"
            onLog(warnMsg.padEnd(50, ' '))
        }
    }

    /**
     * 【步骤三】统一写入
     */
    private fun commitUpdates(result: GitUpdateResult, onLog: (String) -> Unit): Boolean {
        onLog("[3/3] ➜ 正在写入本地存储...\n")

        val INDEX_FILE_NAME = "school_index.pb"
        val localIndexFile = File(indexFileTargetDir, INDEX_FILE_NAME)
        var localIndexContent: ByteArray? = null
        if (localIndexFile.exists()) {
            localIndexContent = try { localIndexFile.readBytes() } catch (e: Exception) { null }
        }

        if (baseLocalDir.exists()) {
            baseLocalDir.deleteRecursively()
        }

        if (!baseLocalDir.mkdirs()) {
            onLog("[3/3] ✖ 无法创建存储目录\n")
            return false
        }

        if (result.resourceFiles.isNotEmpty()) {
            try {
                schoolsFileTargetDir.mkdirs()
                result.resourceFiles.forEach { (sourceFile, targetFile) ->
                    targetFile.parentFile?.mkdirs()
                    sourceFile.copyTo(targetFile, overwrite = true)
                }
            } catch (e: Exception) {
                onLog("[3/3] ✖ 写入资源文件失败: ${e.message}\n")
                return false
            }
        }

        val indexContent = result.indexFileContent ?: localIndexContent
        if (indexContent != null) {
            try {
                indexFileTargetDir.mkdirs()
                File(indexFileTargetDir, INDEX_FILE_NAME).writeBytes(indexContent)
            } catch (e: Exception) {
                onLog("[3/3] ✖ 写入索引文件失败\n")
            }
        }

        onLog("[3/3] ✔ 本地存储写入完成\n")
        return true
    }

    suspend fun updateRepository(repoInfo: RepositoryInfo, onLog: (String) -> Unit) {
        val token = extractToken(repoInfo)
        val result = GitUpdateResult()

        val tempDirsToClean = listOf(
            File(context.cacheDir, "temp_schools_repo"),
            File(context.cacheDir, "temp_index_repo")
        )

        try {
            onLog("▶ 同步仓库: ${repoInfo.name}\n")

            if (!updateResourceFiles(repoInfo, token, onLog, result)) return
            downloadIndexFile(repoInfo, token, onLog, result)

            if (result.isFatalIndexError) return

            if (commitUpdates(result, onLog)) {
                onLog("✔ 仓库更新完成！\n")
            }

        } finally {
            tempDirsToClean.forEach { dir ->
                if (dir.exists()) dir.deleteRecursively()
            }
        }
    }
}