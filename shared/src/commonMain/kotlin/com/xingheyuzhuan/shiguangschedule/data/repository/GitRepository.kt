package com.xingheyuzhuan.shiguangschedule.data.repository

import com.xingheyuzhuan.shiguangschedule.data.model.RepositoryInfo
import com.xingheyuzhuan.shiguangschedule.tool.GitUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

/**
 * 仓库服务接口，定义了与 Git 仓库交互的契约。
 */
interface GitRepository {
    suspend fun updateRepository(repoInfo: RepositoryInfo, onLog: (String) -> Unit)
}

/**
 * GitRepository 的具体实现类。
 */
@Single
class GitRepositoryImpl(
    private val gitUpdater: GitUpdater
) : GitRepository {

    override suspend fun updateRepository(repoInfo: RepositoryInfo, onLog: (String) -> Unit) {
        withContext(Dispatchers.IO) {
            gitUpdater.updateRepository(repoInfo, onLog)
        }
    }
}