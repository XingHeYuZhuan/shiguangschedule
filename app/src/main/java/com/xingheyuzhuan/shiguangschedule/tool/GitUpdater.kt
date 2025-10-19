package com.xingheyuzhuan.shiguangschedule.tool

import android.content.Context
import com.xingheyuzhuan.shiguangschedule.data.model.RepoType
import com.xingheyuzhuan.shiguangschedule.data.model.RepositoryInfo
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import school_index.SchoolIndex
import java.io.File
import com.xingheyuzhuan.shiguangschedule.BuildConfig

/**
 * GitUpdater
 * 延迟写入、协议版本校验和时间戳版本去重。
 * @param context 应用上下文，用于获取文件路径。
 */
class GitUpdater(private val context: Context) {

    // --- 客户端的协议版本定义 ---
    private val CLIENT_PROTOCOL_VERSION: Int = 1

    private val OFFICIAL_BASE_TAG_NAME = "lighthouse"
    private val OFFICIAL_BASE_TAG_SHA = "eb49b7c18272c624d12198b03aabf7fc114a7106"

    private val baseLocalDir: File
        get() = File(context.filesDir, "repo")
    private val indexFileTargetDir: File
        get() = File(baseLocalDir, "index")
    private val schoolsFileTargetDir: File
        get() = File(baseLocalDir, "schools")

    // --- 内部数据结构：用于延迟写入 ---
    private data class GitUpdateResult(
        var indexFileContent: ByteArray? = null,
        var indexRemoteVersionId: String? = null,
        var resourceFiles: List<Pair<File, File>> = emptyList(), // Pair<临时文件, 目标文件路径>
        var isFatalIndexError: Boolean = false // 用于标记索引校验是否遇到致命错误（阻止写入）
    )

    // --- 辅助方法：读取 Protobuf 索引并获取版本信息 ---

    /**
     * 从给定的文件路径读取 Protobuf 索引。
     */
    private fun readSchoolIndex(file: File): SchoolIndex? {
        if (!file.exists()) return null
        return try {
            file.inputStream().use { stream ->
                SchoolIndex.parseFrom(stream)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 辅助方法：比较两个 version_id 字符串（TIME_YYYYMMDDHHMMSS_XXX 格式）。
     * 返回 true 如果 newVersionId 比 localVersionId 新。
     */
    private fun isNewerVersionId(newVersionId: String?, localVersionId: String?): Boolean {
        if (newVersionId.isNullOrBlank()) return false
        if (localVersionId.isNullOrBlank()) return true

        return newVersionId.compareTo(localVersionId) > 0
    }

    // --- Git 辅助逻辑 ---

    private class MyCredentialsProvider(username: String, password: String) :
        UsernamePasswordCredentialsProvider(username, password.toCharArray())

    /**
     * 创建凭证提供者。
     */
    private fun createCredentialsProvider(repoInfo: RepositoryInfo): CredentialsProvider? {
        // 只有私有仓库或明确提供了凭证的公开仓库需要凭证
        if (repoInfo.repoType != RepoType.PRIVATE_REPO && repoInfo.credentials.isNullOrEmpty()) {
            return null
        }
        var username = repoInfo.credentials?.get("username") ?: ""
        val password = repoInfo.credentials?.get("password") ?: ""

        // 修复 JGit Bug：如果提供了 Token 但没有用户名，使用 x-token-auth
        if (password.isNotBlank() && username.isBlank()) {
            username = "x-token-auth"
        }

        return if (username.isBlank() && password.isBlank()) null else MyCredentialsProvider(username, password)
    }

    /**
     * 验证仓库的可访问性和历史合法性（基准灯塔标签检查）。
     */
    private fun isLegitimateFork(userForkUrl: String, credentialsProvider: CredentialsProvider?): Boolean {
        try {
            val lsRemoteCommand = Git.lsRemoteRepository()
                .setRemote(userForkUrl)
            lsRemoteCommand.setTimeout(30)

            // 配置凭证和传输回调
            credentialsProvider?.let {
                lsRemoteCommand.setCredentialsProvider(it)
                lsRemoteCommand.setTransportConfigCallback { transport ->
                    transport.credentialsProvider = it
                }
            }

            lsRemoteCommand.setTags(true).setHeads(false) // 只查标签
            val lsRemote = lsRemoteCommand.call()
            if (lsRemote.isEmpty()) return false

            val expectedTagRefName = "refs/tags/$OFFICIAL_BASE_TAG_NAME"
            val expectedTagSha = ObjectId.fromString(OFFICIAL_BASE_TAG_SHA)

            // 检查远程仓库是否包含名称和 SHA-1 都匹配的标签
            return lsRemote.any {
                it.name == expectedTagRefName && it.objectId == expectedTagSha
            }
        } catch (e: Exception) {
            // 认证失败或连接失败，均视为验证失败
            return false
        }
    }
    // 假设 LogProgressMonitor 已经定义在其他地方

    // --- 核心更新逻辑 ---

    /**
     * 【步骤一】更新资源文件：克隆/拉取资源，并暂存文件列表。
     * 失败时返回 false。
     */
    private fun updateResourceFiles(
        repoInfo: RepositoryInfo,
        credentialsProvider: CredentialsProvider?,
        onLog: (String) -> Unit,
        result: GitUpdateResult
    ): Boolean {
        val RESOURCES_PATH = "resources"
        val tempSchoolsRepoDir = File(context.cacheDir, "temp_schools_repo")
        val progressMonitor = LogProgressMonitor(onLog)

        onLog("\n--- 开始资源文件更新（第一阶段：拉取） ---")
        onLog("目标分支: ${repoInfo.branch}")

        try {
            val gitDir = File(tempSchoolsRepoDir, ".git")
            val isLocalRepoExist = tempSchoolsRepoDir.exists() && gitDir.exists()

            val git: Git = if (isLocalRepoExist) {
                onLog("临时资源仓库已存在，将执行更新...")
                val openedGit = Git.open(tempSchoolsRepoDir)

                // --- FETCH 命令 (使用显式命令对象) ---
                onLog("正在拉取远程变更...")
                val fetchCommand = openedGit.fetch()
                    .setProgressMonitor(progressMonitor)
                    .setTimeout(120)
                    .apply { credentialsProvider?.let { setCredentialsProvider(it) } }
                fetchCommand.call()

                val remoteRef = "refs/remotes/origin/${repoInfo.branch}"
                if (openedGit.repository.findRef(remoteRef) == null) {
                    onLog("错误：资源仓库中不存在分支 '${repoInfo.branch}'。")
                    return false
                }

                // --- RESET 命令 ---
                onLog("正在强制重置本地分支...")
                openedGit.reset()
                    .setMode(ResetCommand.ResetType.HARD)
                    .setRef(remoteRef)
                    .call()
                onLog("本地分支已重置。")

                openedGit // 返回 Git 对象
            } else {
                if (tempSchoolsRepoDir.exists()) tempSchoolsRepoDir.deleteRecursively()
                onLog("正在克隆资源仓库...")

                // --- CLONE 命令
                val cloneCommand = Git.cloneRepository()
                    .setURI(repoInfo.url)
                    .setDirectory(tempSchoolsRepoDir)
                    .setBranch(repoInfo.branch)
                    .setProgressMonitor(progressMonitor)
                    .setTimeout(120)
                    .apply { credentialsProvider?.let { setCredentialsProvider(it) } }

                cloneCommand.call()
            }

            git.use {
                // --- 文件暂存逻辑（代替立即写入） ---
                val sourceResourcesDir = File(tempSchoolsRepoDir, RESOURCES_PATH)

                if (!sourceResourcesDir.exists() || !sourceResourcesDir.isDirectory) {
                    onLog("错误：在仓库中未找到 '${RESOURCES_PATH}' 文件夹。")
                    return false
                }

                // 递归遍历并暂存所有文件
                val tempFiles = mutableListOf<Pair<File, File>>()
                sourceResourcesDir.walkTopDown().forEach { sourceFile ->
                    if (sourceFile.isFile) {
                        if (sourceFile.name.equals("adapters.yaml", ignoreCase = true)) {
                            return@forEach
                        }
                        // 目标路径：filesDir/repo/schools/resources/...
                        val relativePath = sourceFile.relativeTo(sourceResourcesDir)
                        val targetFile = File(File(schoolsFileTargetDir, RESOURCES_PATH), relativePath.path)

                        // 暂存 Pair<临时文件, 目标文件>
                        tempFiles.add(Pair(sourceFile, targetFile))
                    }
                }

                if (tempFiles.isEmpty()) {
                    onLog("警告：资源文件夹为空，没有文件需要暂存。")
                }

                result.resourceFiles = tempFiles
                onLog("资源文件已成功暂存（共 ${tempFiles.size} 个文件）。")
            }

            return true // 资源文件拉取和暂存成功

        } catch (e: Exception) {
            onLog("错误：资源文件更新失败。")
            onLog("异常类型：${e::class.java.simpleName}")
            onLog("错误信息：${e.message}")
            onLog("详细堆栈跟踪：\n${e.stackTraceToString()}")
            return false
        } finally {
            // 临时仓库清理交给外部统一处理
        }
    }


    /**
     * 【步骤二】下载索引文件：克隆/拉取索引，校验版本，并暂存内容。
     */
    private fun downloadIndexFile(
        repoInfo: RepositoryInfo,
        credentialsProvider: CredentialsProvider?,
        onLog: (String) -> Unit,
        result: GitUpdateResult
    ) {
        val INDEX_BRANCH = "index-pb-release"
        val INDEX_FILE_NAME = "school_index.pb"
        val tempIndexRepoDir = File(context.cacheDir, "temp_index_repo")

        onLog("\n--- 开始索引文件下载（第二阶段：拉取与校验） ---")
        onLog("目标分支: $INDEX_BRANCH")

        try {
            if (tempIndexRepoDir.exists()) tempIndexRepoDir.deleteRecursively()

            onLog("正在执行克隆并检出索引分支...")

            // --- CLONE 命令
            val cloneCommand = Git.cloneRepository()
                .setURI(repoInfo.url)
                .setDirectory(tempIndexRepoDir)
                .setBranch(INDEX_BRANCH)
                .setTimeout(30)

            // 使用显式 if 语句设置凭证，以确保稳定性
            if (credentialsProvider != null) {
                cloneCommand.setCredentialsProvider(credentialsProvider)
            }

            cloneCommand.call().use {} // 执行克隆

            val sourceFile = File(tempIndexRepoDir, INDEX_FILE_NAME)
            if (!sourceFile.exists()) {
                onLog("警告：临时仓库中未找到文件 '$INDEX_FILE_NAME'。请确认分支和文件路径正确。")
                onLog("流程：无索引文件，使用本地索引（若存在）。继续主流程。")
                return // 找不到文件，按无分支处理（容错）
            }

            // --- 校验逻辑 ---
            val remoteIndex = readSchoolIndex(sourceFile)
            if (remoteIndex == null) {
                onLog("错误：无法解析远程索引文件。可能文件损坏。")
                return
            }

            // A. 校验协议版本：远程 > 客户端视为致命错误
            if (remoteIndex.protocolVersion > CLIENT_PROTOCOL_VERSION) {
                onLog("致命错误：远程索引协议版本 (${remoteIndex.protocolVersion}) 高于客户端支持版本 ($CLIENT_PROTOCOL_VERSION)。")
                onLog("操作：更新被中止。请提示用户更新软件版本以兼容新协议。")
                result.isFatalIndexError = true
                return
            }

            // B. 校验数据版本 (时间戳)
            val localIndex = readSchoolIndex(File(indexFileTargetDir, INDEX_FILE_NAME))
            val localVersionId = localIndex?.versionId

            onLog("远程数据版本ID: ${remoteIndex.versionId}")
            onLog("本地数据版本ID: ${localVersionId ?: "N/A"}")

            if (isNewerVersionId(remoteIndex.versionId, localVersionId)) {
                onLog("结果：远程版本更新，将执行索引文件写入。")
                // 暂存文件内容和版本ID
                result.indexFileContent = sourceFile.readBytes()
                result.indexRemoteVersionId = remoteIndex.versionId
            } else if (remoteIndex.versionId == localVersionId) {
                onLog("结果：当前索引已经是最新版本。跳过索引文件写入。")
            } else {
                onLog("致命错误：远程仓库索引时间戳 (${remoteIndex.versionId}) 更旧。检查远程仓库是否正确。")
                onLog("操作：为保证数据一致性，终止全部文件写入。")
                result.isFatalIndexError = true // 标记致命错误
                return
            }

        } catch (e: Exception) {
            val errorMessage = e.message ?: ""
            // 容错处理：找不到分支或认证失败
            val INDEX_BRANCH_REF = "refs/heads/$INDEX_BRANCH"
            val isBranchNotFound = errorMessage.contains(INDEX_BRANCH) ||
                    errorMessage.contains(INDEX_BRANCH_REF) ||
                    e::class.java.simpleName.contains("RefNotAdvertisedException")

            if (isBranchNotFound) {
                onLog("警告：仓库中未找到索引分支 '$INDEX_BRANCH'。")
                onLog("流程：无索引分支，使用本地索引（若存在）。继续主流程。")
            } else {
                onLog("错误：索引文件下载失败。")
                onLog("异常类型：${e::class.java.simpleName}")
                onLog("错误信息：${e.message}")
                onLog("详细堆栈跟踪：\n${e.stackTraceToString()}")
            }
        } finally {
            // 临时仓库清理交给外部统一处理
        }
    }


    /**
     * 【步骤三】执行所有暂存文件的统一写入。
     */
    private fun commitUpdates(result: GitUpdateResult, onLog: (String) -> Unit): Boolean {
        onLog("\n--- 阶段三：统一写入本地存储 ---")
        var success = true

        val INDEX_FILE_NAME = "school_index.pb"

        val localIndexFile = File(indexFileTargetDir, INDEX_FILE_NAME)
        var localIndexContent: ByteArray? = null
        if (localIndexFile.exists()) {
            try {
                localIndexContent = localIndexFile.readBytes()
                onLog("本地索引文件内容已备份到内存。")
            } catch (e: Exception) {
                onLog("警告：读取本地索引文件失败，若版本未更新，索引可能丢失。")
            }
        }

        onLog("正在执行彻底清理：删除整个本地仓库目录: ${baseLocalDir.name}")
        if (baseLocalDir.exists()) {
            baseLocalDir.deleteRecursively()
        }

        // 重新创建所需的基准目录
        if (!baseLocalDir.mkdirs()) {
            onLog("致命错误：无法创建基准目录。")
            return false
        }

        // --- 3. 写入资源文件 ---
        if (result.resourceFiles.isNotEmpty()) {
            onLog("正在写入 ${result.resourceFiles.size} 个资源文件...")
            try {
                schoolsFileTargetDir.mkdirs()

                result.resourceFiles.forEach { (sourceFile, targetFile) ->
                    targetFile.parentFile?.mkdirs()
                    sourceFile.copyTo(targetFile, overwrite = true)
                }
                onLog("成功：资源文件已写入到 /${baseLocalDir.name}/${schoolsFileTargetDir.name}")
            } catch (e: Exception) {
                onLog("致命错误：写入资源文件失败。")
                onLog("错误信息：${e.message}")
                success = false
            }
        } else {
            onLog("资源文件暂存列表为空，跳过写入。")
        }

        // --- 4. 写入索引文件 (如果有新版本，则写入；如果版本相同，则恢复备份) ---
        val indexContent = result.indexFileContent

        if (indexContent != null) {
            // A. 有新版本 (indexContent != null)，写入新索引
            try {
                indexFileTargetDir.mkdirs()
                val targetFile = File(indexFileTargetDir, INDEX_FILE_NAME)
                targetFile.writeBytes(indexContent)
                onLog("成功：索引文件 (版本 ${result.indexRemoteVersionId}) 已写入到 /${baseLocalDir.name}/${indexFileTargetDir.name}/$INDEX_FILE_NAME")
            } catch (e: Exception) {
                onLog("错误：写入新索引文件失败。")
            }
        } else {
            // B. 版本相同或更旧 (indexContent == null)，恢复备份
            if (localIndexContent != null) {
                try {
                    indexFileTargetDir.mkdirs()
                    val targetFile = File(indexFileTargetDir, INDEX_FILE_NAME)
                    targetFile.writeBytes(localIndexContent)
                    onLog("索引版本未更新，已恢复备份的旧索引文件。")
                } catch (e: Exception) {
                    onLog("错误：恢复旧索引文件失败。")
                }
            } else {
                onLog("索引文件内容为空且无旧索引备份，跳过索引写入。")
            }
        }

        return success
    }


    /**
     * 更新或克隆仓库并提供详细日志。
     */
    fun updateRepository(repoInfo: RepositoryInfo, onLog: (String) -> Unit) {
        val credentialsProvider = createCredentialsProvider(repoInfo)
        val result = GitUpdateResult()

        // 临时仓库目录列表，用于最终清理
        val tempDirsToClean = listOf(
            File(context.cacheDir, "temp_schools_repo"),
            File(context.cacheDir, "temp_index_repo")
        )

        try {
            if (repoInfo.repoType != RepoType.OFFICIAL) {

                if (BuildConfig.ENABLE_LIGHTHOUSE_VERIFICATION) {
                    onLog("正在执行安全验证（基准灯塔标签检查）...")

                    if (!isLegitimateFork(repoInfo.url, credentialsProvider)) {
                        onLog("!!! 致命错误：仓库未通过合法性验证或认证失败。")
                        if (repoInfo.repoType == RepoType.PRIVATE_REPO) {
                            onLog("提示：请检查 PAT 权限和 Token 字符串是否正确。")
                        }
                        return
                    }
                    onLog("安全验证通过：找到官方基准灯塔标签。")
                } else {
                    onLog("安全提示：已根据构建配置跳过基准灯塔标签验证（非官方仓库）。")
                }
            }


            // 2. 资源文件更新 (核心业务，失败则全部失败)
            val resourceUpdateSuccess = updateResourceFiles(repoInfo, credentialsProvider, onLog, result)

            if (!resourceUpdateSuccess) {
                onLog("\n!!! 致命错误：资源文件更新失败，终止全部更新流程。")
                return
            }

            // 3. 索引文件下载 (可容错/版本校验)
            downloadIndexFile(repoInfo, credentialsProvider, onLog, result)

            // !!! 关键检查：索引校验是否触发了致命错误 !!!
            if (result.isFatalIndexError) {
                onLog("\n!!! 致命错误：索引校验失败 (协议不兼容或远程版本过旧)，终止全部更新流程（不写入磁盘）。")
                return
            }

            // 4. 统一写入本地存储
            val commitSuccess = commitUpdates(result, onLog)

            if (!commitSuccess) {
                onLog("\n!!! 致命错误：统一写入操作失败，资源文件未正确写入。")
                return
            }

            onLog("\n--- 全部更新流程完成。---")

        } finally {
            // 统一清理临时目录
            tempDirsToClean.forEach { dir ->
                if (dir.exists()) {
                    dir.deleteRecursively()
                    onLog("已清理临时目录: ${dir.name}")
                }
            }
        }
    }
}