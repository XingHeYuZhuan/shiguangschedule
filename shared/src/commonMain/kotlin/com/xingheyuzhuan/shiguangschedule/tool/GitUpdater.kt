package com.xingheyuzhuan.shiguangschedule.tool

import com.xingheyuzhuan.shiguangschedule.data.model.RepoType
import com.xingheyuzhuan.shiguangschedule.data.model.RepositoryInfo
import com.xingheyuzhuan.kgit.Ext
import com.xingheyuzhuan.kgit.logging.ProgressMonitor
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import school_index.SchoolIndex

/**
 * 内置索引声明的脚本由当前 App 版本负责提供，在线仓库同步不得用同路径文件覆盖。
 */
internal fun builtInAdapterResourcePaths(index: SchoolIndex): Set<String> = buildSet {
    index.schools.forEach { school ->
        school.adapters.forEach { adapter ->
            val resourcePath = "${school.resource_folder}/${adapter.asset_js_path}"
                .replace('\\', '/')
                .trimStart('/')
            if (resourcePath.isNotBlank()) add(resourcePath)
        }
    }
}

/**
 * 单行对齐进度监听器
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

    override fun endTask() {}

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
 * Git 仓库更新与资源同步工具（基于 Okio 跨平台实现）
 */
@Single
class GitUpdater(
    private val fileSystem: FileSystem,
    @Named("FilesDir") private val filesDir: Path,
    @Named("CacheDir") private val cacheDir: Path
) {

    // 升级客户端支持的最高协议版本为 2
    private val clientProtocolVersion: Int = 2

    private val baseLocalDir: Path
        get() = filesDir / "repo"
    private val indexFileTargetDir: Path
        get() = baseLocalDir / "index"
    private val schoolsFileTargetDir: Path
        get() = baseLocalDir / "schools"

    private data class GitUpdateResult(
        var indexFileContent: ByteArray? = null,
        var indexRemoteVersionId: String? = null,
        var resourceFiles: List<Pair<Path, Path>> = emptyList(),
        var isFatalIndexError: Boolean = false
    )

    private fun readSchoolIndex(path: Path): SchoolIndex? {
        if (!fileSystem.exists(path)) return null
        return try {
            fileSystem.read(path) {
                SchoolIndex.ADAPTER.decode(this)
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

    private fun parseNetworkErrorMessage(e: Exception): String {
        val fullErr = ((e.message ?: "") + " " + (e.cause?.message ?: "")).lowercase()
        return when {
            fullErr.contains("401") || fullErr.contains("403") || fullErr.contains("not authorized") || fullErr.contains("authentication") ->
                "身份验证失败，请检查凭证配置"
            fullErr.contains("404") || fullErr.contains("not found") ->
                "仓库或分支不存在，请检查配置"
            else -> "连接中断，请更换仓库"
        }
    }

    private suspend fun updateResourceFiles(
        repoInfo: RepositoryInfo,
        token: String?,
        onLog: (String) -> Unit,
        result: GitUpdateResult
    ): Boolean {
        val resourcesPath = "resources"
        val tempSchoolsRepoDir = cacheDir / "temp_schools_repo"
        val progressMonitor = LogProgressMonitor("[1/3]", onLog)

        onLog("[1/3] ➜ 开始拉取资源库 (${repoInfo.branch})\n")

        try {
            if (fileSystem.exists(tempSchoolsRepoDir)) {
                fileSystem.deleteRecursively(tempSchoolsRepoDir)
            }

            val downloadCmd = Ext.downloadRepository()
                .setUri(repoInfo.url)
                .setDirectory(tempSchoolsRepoDir)
                .setBranch(repoInfo.branch)
                .setProgressMonitor(progressMonitor)

            if (!token.isNullOrEmpty()) {
                downloadCmd.setToken(token)
            }

            downloadCmd.call(fileSystem)

            val sourceResourcesDir = tempSchoolsRepoDir / resourcesPath
            if (!fileSystem.exists(sourceResourcesDir) || !fileSystem.metadata(sourceResourcesDir).isDirectory) {
                onLog("\r[1/3] ✖ 错误：未找到 '${resourcesPath}' 目录\n")
                return false
            }

            val tempFiles = mutableListOf<Pair<Path, Path>>()
            val protectedBuiltInResources = readSchoolIndex(
                indexFileTargetDir / "builtin_school_index.pb"
            )?.let(::builtInAdapterResourcePaths).orEmpty()

            fileSystem.listRecursively(sourceResourcesDir).forEach { sourcePath ->
                if (fileSystem.metadata(sourcePath).isRegularFile) {
                    if (sourcePath.name.equals("adapters.yaml", ignoreCase = true)) return@forEach
                    val relativeSegments = sourcePath.segments.drop(sourceResourcesDir.segments.size)
                    val relativeResourcePath = relativeSegments.joinToString("/")
                    if (relativeResourcePath in protectedBuiltInResources) return@forEach
                    val relativePath = relativeSegments.fold("".toPath()) { acc, segment -> acc / segment }
                    val targetPath = schoolsFileTargetDir / resourcesPath / relativePath
                    tempFiles.add(Pair(sourcePath, targetPath))
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

    private suspend fun downloadIndexFile(
        repoInfo: RepositoryInfo,
        token: String?,
        onLog: (String) -> Unit,
        result: GitUpdateResult
    ) {
        val indexBranch = "index-pb-release"
        val indexFileName = "school_index.pb"
        val tempIndexRepoDir = cacheDir / "temp_index_repo"
        val progressMonitor = LogProgressMonitor("[2/3]", onLog)

        onLog("[2/3] ➜ 开始校验数据索引...\n")

        try {
            if (fileSystem.exists(tempIndexRepoDir)) {
                fileSystem.deleteRecursively(tempIndexRepoDir)
            }

            val downloadCmd = Ext.downloadRepository()
                .setUri(repoInfo.url)
                .setDirectory(tempIndexRepoDir)
                .setBranch(indexBranch)
                .setProgressMonitor(progressMonitor)

            if (!token.isNullOrEmpty()) {
                downloadCmd.setToken(token)
            }

            downloadCmd.call(fileSystem)

            val sourceFile = tempIndexRepoDir / indexFileName
            if (!fileSystem.exists(sourceFile)) {
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
            // 判断远程索引协议是否大于客户端支持的最大协议版本 (2)
            if (remoteProtocol > clientProtocolVersion) {
                val fatalMsg = "\r[2/3] ✖ 协议不兼容 (远程 v$remoteProtocol > 本地 v$clientProtocolVersion)，请升级 App\n"
                onLog(fatalMsg.padEnd(50, ' '))
                result.isFatalIndexError = true
                return
            }

            val localIndex = readSchoolIndex(indexFileTargetDir / indexFileName)
            val localVersionId = localIndex?.version_id

            if (isNewerVersionId(remoteIndex.version_id, localVersionId)) {
                val okMsg = "\r[2/3] ✔ 发现新版本索引 (${remoteIndex.version_id})\n"
                onLog(okMsg.padEnd(50, ' '))
                result.indexFileContent = fileSystem.read(sourceFile) { readByteArray() }
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

    private fun commitUpdates(result: GitUpdateResult, onLog: (String) -> Unit): Boolean {
        onLog("[3/3] ➜ 正在写入本地存储...\n")

        val indexFileName = "school_index.pb"

        try {
            if (!fileSystem.exists(baseLocalDir)) {
                fileSystem.createDirectories(baseLocalDir)
            }
        } catch (e: Exception) {
            onLog("[3/3] ✖ 无法创建存储目录: ${e.message}\n")
            return false
        }

        if (result.resourceFiles.isNotEmpty()) {
            try {
                if (!fileSystem.exists(schoolsFileTargetDir)) {
                    fileSystem.createDirectories(schoolsFileTargetDir)
                }

                result.resourceFiles.forEach { (sourceFile, targetFile) ->
                    targetFile.parent?.let { fileSystem.createDirectories(it) }

                    if (fileSystem.exists(targetFile)) {
                        fileSystem.delete(targetFile)
                    }

                    fileSystem.copy(sourceFile, targetFile)
                }
            } catch (e: Exception) {
                onLog("[3/3] ✖ 写入资源文件失败: ${e.message}\n")
                return false
            }
        }

        result.indexFileContent?.let { newIndexBytes ->
            try {
                if (!fileSystem.exists(indexFileTargetDir)) {
                    fileSystem.createDirectories(indexFileTargetDir)
                }
                fileSystem.write(indexFileTargetDir / indexFileName) {
                    write(newIndexBytes, 0, newIndexBytes.size)
                }
            } catch (e: Exception) {
                onLog("[3/3] ✖ 写入索引文件失败: ${e.message}\n")
                return false
            }
        }

        onLog("[3/3] ✔ 本地存储写入完成\n")
        return true
    }

    suspend fun updateRepository(repoInfo: RepositoryInfo, onLog: (String) -> Unit) {
        val token = extractToken(repoInfo)
        val result = GitUpdateResult()

        val tempDirsToClean = listOf(
            cacheDir / "temp_schools_repo",
            cacheDir / "temp_index_repo"
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
                if (fileSystem.exists(dir)) {
                    fileSystem.deleteRecursively(dir)
                }
            }
        }
    }
}
