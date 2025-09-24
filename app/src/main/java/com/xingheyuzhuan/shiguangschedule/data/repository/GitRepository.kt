package com.xingheyuzhuan.shiguangschedule.data.repository

import com.xingheyuzhuan.shiguangschedule.data.model.RepositoryInfo
import com.xingheyuzhuan.shiguangschedule.tool.GitUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 仓库服务接口，定义了与 Git 仓库交互的契约。
 */
interface GitRepository {

    /**
     * 更新或克隆指定仓库。
     * @param repoInfo 仓库信息。
     * @param onLog 用于接收实时日志消息的回调函数。
     */
    suspend fun updateRepository(repoInfo: RepositoryInfo, onLog: (String) -> Unit)
}

/**
 * GitRepository 的具体实现类。
 * 负责将 ViewModel 的调用映射到底层 GitUpdater 的操作。
 */
class GitRepositoryImpl(private val gitUpdater: GitUpdater) : GitRepository {

    override suspend fun updateRepository(repoInfo: RepositoryInfo, onLog: (String) -> Unit) {
        withContext(Dispatchers.IO) {
            // 在IO线程上执行Git操作，并传递日志回调
            gitUpdater.updateRepository(repoInfo, onLog)
        }
    }
}