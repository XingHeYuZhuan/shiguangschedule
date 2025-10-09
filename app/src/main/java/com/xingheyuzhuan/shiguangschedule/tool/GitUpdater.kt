package com.xingheyuzhuan.shiguangschedule.tool

import android.content.Context
import com.xingheyuzhuan.shiguangschedule.data.model.RepoType
import com.xingheyuzhuan.shiguangschedule.data.model.RepositoryInfo
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import com.xingheyuzhuan.shiguangschedule.BuildConfig
import java.io.File

/**
 * 负责Git仓库的克隆、更新和合法性验证。
 *
 * @param context 应用上下文，用于获取文件路径。
 */
class GitUpdater(private val context: Context) {

    private val OFFICIAL_BASE_TAG_NAME = "lighthouse"

    private val OFFICIAL_BASE_TAG_SHA = "eb49b7c18272c624d12198b03aabf7fc114a7106"

    // 通过 Context 动态获取本地仓库路径。
    private val localRepoDir: File
        get() = File(context.filesDir, "repo")

    /**
     * 自定义的凭证提供者，处理用户名/密码或个人访问令牌。
     */
    private class MyCredentialsProvider(username: String, password: String) :
        UsernamePasswordCredentialsProvider(username, password.toCharArray())


    /**
     * 验证仓库的可访问性和历史合法性，不传输文件内容。
     * 逻辑：检查远程仓库是否包含官方的永久基准标签，并校验其 SHA-1。
     */
    private fun isLegitimateFork(userForkUrl: String, credentialsProvider: CredentialsProvider?): Boolean {
        try {
            val lsRemoteCommand = Git.lsRemoteRepository()
                .setRemote(userForkUrl)

            lsRemoteCommand.setTimeout(30)

            credentialsProvider?.let {
                lsRemoteCommand.setCredentialsProvider(it)

                lsRemoteCommand.setTransportConfigCallback { transport ->
                    transport.credentialsProvider = it
                }
            }

            // 关键：只查询标签 (Tag)，不查询分支 (Head)，避免拉取不必要数据
            lsRemoteCommand.setTags(true).setHeads(false)

            val lsRemote = lsRemoteCommand.call()

            if (lsRemote.isEmpty()) {
                return false
            }

            // 预期的引用名和 SHA 对象
            val expectedTagRefName = "refs/tags/$OFFICIAL_BASE_TAG_NAME"
            val expectedTagSha = ObjectId.fromString(OFFICIAL_BASE_TAG_SHA)

            // 检查远程仓库是否包含名称和 SHA-1 都匹配的标签
            return lsRemote.any {
                it.name == expectedTagRefName && it.objectId == expectedTagSha
            }

        } catch (e: Exception) {
            // 捕获 TransportException 等认证失败的错误
            return false
        }
    }


    /**
     * 更新或克隆仓库并提供详细日志。
     */
    fun updateRepository(repoInfo: RepositoryInfo, onLog: (String) -> Unit) {

        // 创建 credentialsProvider (灵活支持真实用户名和 x-token-auth 修复)
        val credentialsProvider: CredentialsProvider? = if (repoInfo.repoType == RepoType.PRIVATE_REPO && repoInfo.credentials != null) {

            var username = repoInfo.credentials["username"] ?: ""
            val password = repoInfo.credentials["password"] ?: ""

            // 如果用户提供了 Token 但没有用户名，应用 x-token-auth 修复 JGit 的 Bug
            if (password.isNotBlank() && username.isBlank()) {
                username = "x-token-auth"
            }

            // 如果用户名和密码都为空，则返回 null，否则创建凭证
            if (username.isBlank() && password.isBlank()) {
                null
            } else {
                MyCredentialsProvider(username, password)
            }

        } else {
            // 公开仓库的逻辑（如果提供了用户名/密码，则使用）
            val username = repoInfo.credentials?.get("username") ?: ""
            val password = repoInfo.credentials?.get("password") ?: ""
            if (username.isNotBlank() || password.isNotBlank()) MyCredentialsProvider(username, password) else null
        }

        // 2. 对所有非官方仓库进行合法性验证 (在克隆/拉取文件前执行)
        if (repoInfo.repoType != RepoType.OFFICIAL) {

            if (BuildConfig.ENABLE_LIGHTHOUSE_VERIFICATION) {

                onLog("正在执行安全验证（基准灯塔标签检查）...")

                if (!isLegitimateFork(repoInfo.url, credentialsProvider)) {
                    onLog("错误：仓库未通过合法性验证或认证失败。")
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

        val targetUrl = repoInfo.url
        val gitDir = File(localRepoDir, ".git")
        val isLocalRepoExist = localRepoDir.exists() && gitDir.exists()

        onLog(if (isLocalRepoExist) "本地仓库已存在，将执行更新。" else "本地仓库不存在，将执行克隆。")

        val progressMonitor = LogProgressMonitor(onLog)

        try {
            val git: Git = if (isLocalRepoExist) {
                onLog("正在打开本地仓库...")
                Git.open(localRepoDir)
            } else {
                // 克隆操作
                if (localRepoDir.exists()) {
                    onLog("检测到非Git目录，正在删除旧目录...")
                    localRepoDir.deleteRecursively()
                }

                onLog("正在克隆远程仓库...")
                val cloneCommand = Git.cloneRepository()
                    .setURI(targetUrl)
                    .setDirectory(localRepoDir)
                    .setBranch(repoInfo.branch)
                    .setProgressMonitor(progressMonitor)
                    .setTimeout(120)

                credentialsProvider?.let { cloneCommand.setCredentialsProvider(it) }
                cloneCommand.call()
            }

            git.use { git ->
                if (!isLocalRepoExist) {
                    onLog("成功：仓库已克隆到本地。")
                    return
                }

                onLog("正在添加临时远程仓库...")
                val tempRemoteName = "update_remote"
                git.remoteAdd()
                    .setName(tempRemoteName)
                    .setUri(URIish(targetUrl))
                    .call()
                onLog("临时远程仓库已添加。")

                onLog("正在拉取远程变更...")
                val fetchCommand = git.fetch()
                    .setRemote(tempRemoteName)
                    .setProgressMonitor(progressMonitor)
                    .setTimeout(120)
                credentialsProvider?.let { fetchCommand.setCredentialsProvider(it) }
                fetchCommand.call()

                onLog("远程变更已拉取。")

                val remoteRef = "refs/remotes/$tempRemoteName/${repoInfo.branch}"
                if (git.repository.findRef(remoteRef) == null) {
                    git.remoteRemove().setRemoteName(tempRemoteName).call()
                    onLog("错误：仓库中不存在分支 '${repoInfo.branch}'。")
                    return
                }

                onLog("正在强制重置本地分支...")
                git.reset()
                    .setMode(ResetCommand.ResetType.HARD)
                    .setRef(remoteRef)
                    .call()
                onLog("本地分支已重置。")

                onLog("正在移除临时远程仓库...")
                git.remoteRemove().setRemoteName(tempRemoteName).call()
                onLog("临时远程仓库已移除。")

                onLog("成功：已更新到仓库的 '${repoInfo.branch}' 分支。")
            }
        } catch (e: Exception) {
            onLog("错误：更新或克隆时发生异常。")
            onLog("异常类型：${e::class.java.simpleName}")
            onLog("错误信息：${e.message}")
            onLog("详细堆栈跟踪：\n${e.stackTraceToString()}")
        }
    }
}