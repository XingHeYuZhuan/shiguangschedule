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
import java.io.File

/**
 * 负责Git仓库的克隆、更新和合法性验证。
 *
 * @param context 应用上下文，用于获取文件路径。
 */
class GitUpdater(private val context: Context) {

    // 官方仓库中一个已知的、早期的提交ID，用于合法性验证。
    private val OFFICIAL_REPO_BASE_COMMIT_ID = "eb49b7c18272c624d12198b03aabf7fc114a7106"

    // 通过 Context 动态获取本地仓库路径。
    private val localRepoDir: File
        get() = File(context.filesDir, "repo")

    /**
     * 自定义的凭证提供者，处理用户名/密码或个人访问令牌。
     * 这是一个私有嵌套类，仅供 GitUpdater 内部使用。
     */
    private class MyCredentialsProvider(username: String, password: String) :
        UsernamePasswordCredentialsProvider(username, password.toCharArray())

    /**
     * 验证用户提供的 Git 仓库是否是官方仓库的合法 fork 或镜像。
     *
     * @param userForkUrl 用户仓库的 URL。
     * @return 如果用户仓库包含官方提交 ID，则返回 true。
     */
    private fun isLegitimateFork(userForkUrl: String): Boolean {
        try {
            val lsRemote = Git.lsRemoteRepository()
                .setRemote(userForkUrl)
                .call()
            val expectedObjectId = ObjectId.fromString(OFFICIAL_REPO_BASE_COMMIT_ID)
            return lsRemote.any { it.objectId == expectedObjectId }
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * 更新或克隆仓库并提供详细日志。
     * @param repoInfo 仓库信息，包含URL、分支和类型。
     * @param onLog 用于接收实时日志消息的回调函数。
     */
    fun updateRepository(repoInfo: RepositoryInfo, onLog: (String) -> Unit) {
        // 对所有非官方仓库进行合法性验证
        if (repoInfo.repoType != RepoType.OFFICIAL) {
            onLog("正在验证仓库合法性...")
            if (!isLegitimateFork(repoInfo.url)) {
                onLog("错误：仓库未通过合法性验证。")
                return
            }
            onLog("仓库合法性验证成功。")
        }

        val targetUrl = repoInfo.url
        val gitDir = File(localRepoDir, ".git")
        val isLocalRepoExist = localRepoDir.exists() && gitDir.exists()

        onLog(if (isLocalRepoExist) "本地仓库已存在，将执行更新。" else "本地仓库不存在，将执行克隆。")

        try {
            // 只有当仓库类型为 PRIVATE_REPO 且凭证不为空时才创建凭证提供者
            val credentialsProvider: CredentialsProvider? = if (repoInfo.repoType == RepoType.PRIVATE_REPO && repoInfo.credentials != null) {
                val username = repoInfo.credentials["username"] ?: ""
                val password = repoInfo.credentials["password"] ?: ""
                MyCredentialsProvider(username, password)
            } else {
                null
            }

            val git: Git = if (isLocalRepoExist) {
                onLog("正在打开本地仓库...")
                Git.open(localRepoDir)
            } else {
                // 新增：在克隆前检查并删除已存在的非Git目录
                if (localRepoDir.exists()) {
                    onLog("检测到非Git目录，正在删除旧目录...")
                    localRepoDir.deleteRecursively()
                }

                onLog("正在克隆远程仓库...")
                val cloneCommand = Git.cloneRepository()
                    .setURI(targetUrl)
                    .setDirectory(localRepoDir)
                    .setBranch(repoInfo.branch)
                    .setProgressMonitor(LogProgressMonitor(onLog))
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
                    .setProgressMonitor(LogProgressMonitor(onLog))
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