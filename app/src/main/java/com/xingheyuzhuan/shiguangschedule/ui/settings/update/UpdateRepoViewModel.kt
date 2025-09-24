package com.xingheyuzhuan.shiguangschedule.ui.settings.update

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.data.model.RepositoryInfo
import com.xingheyuzhuan.shiguangschedule.data.repository.GitRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.GitRepositoryImpl
import com.xingheyuzhuan.shiguangschedule.tool.GitUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.InputStreamReader

open class UpdateRepoViewModel(
    private val gitRepository: GitRepository,
    private val jsonInputStream: InputStream
) : ViewModel() {

    // UI状态，包含可供选择的仓库列表、当前选择的仓库和日志
    data class UpdateRepoState(
        val repoList: List<RepositoryInfo> = emptyList(),
        val selectedRepo: RepositoryInfo? = null,
        val logs: String = "",
        val isUpdating: Boolean = false
    )

    private val _uiState = MutableStateFlow(UpdateRepoState())
    val uiState: StateFlow<UpdateRepoState> = _uiState.asStateFlow()

    init {
        loadRepositories()
    }

    // 从JSON文件加载仓库列表
    private fun loadRepositories() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonString = jsonInputStream.reader().use { it.readText() }
                val repos = Json.decodeFromString<List<RepositoryInfo>>(jsonString)
                _uiState.value = _uiState.value.copy(
                    repoList = repos,
                    selectedRepo = repos.firstOrNull() // 默认选中第一个
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    logs = "错误：加载仓库列表失败。\n${e.message}"
                )
            }
        }
    }

    // 更新当前选择的仓库
    fun selectRepository(repo: RepositoryInfo) {
        _uiState.value = _uiState.value.copy(selectedRepo = repo)
    }

    // 开始更新仓库
    fun startUpdate() {
        val repoToUpdate = _uiState.value.selectedRepo ?: return
        if (_uiState.value.isUpdating) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isUpdating = true,
                logs = "" // 清空旧日志
            )

            // 传递日志回调，将日志追加到UI状态
            gitRepository.updateRepository(repoToUpdate) { log ->
                val newLog = "${_uiState.value.logs}${log}\n"
                _uiState.value = _uiState.value.copy(logs = newLog)
            }

            _uiState.value = _uiState.value.copy(isUpdating = false)
        }
    }
}

// 将 ViewModelFactory 放在 ViewModel 的 companion object 中
class UpdateRepoViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UpdateRepoViewModel::class.java)) {
            val gitUpdater = GitUpdater(application.applicationContext)
            val gitRepository: GitRepository = GitRepositoryImpl(gitUpdater)
            // 修改为从 assets 目录读取
            val jsonInputStream: InputStream = application.assets.open("git_repos.json")

            @Suppress("UNCHECKED_CAST")
            return UpdateRepoViewModel(gitRepository, jsonInputStream) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}