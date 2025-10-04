package com.xingheyuzhuan.shiguangschedule.ui.settings.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.xingheyuzhuan.shiguangschedule.data.model.RepositoryInfo
import com.xingheyuzhuan.shiguangschedule.data.model.RepoType
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

open class UpdateRepoViewModel(
    private val gitRepository: GitRepository,
    private val jsonInputStream: InputStream
) : ViewModel() {

    // UI状态，包含可供选择的仓库列表、当前选择的仓库和日志
    data class UpdateRepoState(
        val repoList: List<RepositoryInfo> = emptyList(),
        val selectedRepo: RepositoryInfo? = null,
        val logs: String = "",
        val isUpdating: Boolean = false,

        // URL 和 Branch 的编辑状态（已存在）
        val currentEditableUrl: String = "",
        val currentEditableBranch: String = "",

        // 凭证的编辑状态
        val currentEditableUsername: String = "",
        val currentEditablePassword: String = "" // 密码或 Token Value
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
                val defaultRepo = repos.firstOrNull() // 默认选中的仓库

                // 辅助函数，安全地从 credentials map 中提取值
                fun getCredentialValue(key: String): String = defaultRepo?.credentials?.get(key) ?: ""

                _uiState.value = _uiState.value.copy(
                    repoList = repos,
                    selectedRepo = defaultRepo,
                    currentEditableUrl = defaultRepo?.url ?: "",
                    currentEditableBranch = defaultRepo?.branch ?: "",
                    // 初始化时填充凭证编辑字段
                    currentEditableUsername = getCredentialValue("username"),
                    currentEditablePassword = getCredentialValue("password")
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
        // 辅助函数，安全地从 credentials map 中提取值
        fun getCredentialValue(key: String): String = repo.credentials?.get(key) ?: ""

        _uiState.value = _uiState.value.copy(
            selectedRepo = repo,
            currentEditableUrl = repo.url,
            currentEditableBranch = repo.branch,
            // 切换仓库时，同步凭证信息
            currentEditableUsername = getCredentialValue("username"),
            currentEditablePassword = getCredentialValue("password")
        )
    }

    // 更新当前编辑的 URL
    fun updateCurrentUrl(url: String) {
        _uiState.value = _uiState.value.copy(currentEditableUrl = url)
    }

    // 更新当前编辑的 Branch
    fun updateCurrentBranch(branch: String) {
        _uiState.value = _uiState.value.copy(currentEditableBranch = branch)
    }

    // 更新当前编辑的 Username/Token Key
    fun updateCurrentUsername(username: String) {
        _uiState.value = _uiState.value.copy(currentEditableUsername = username)
    }

    // 更新当前编辑的 Password/Token Value
    fun updateCurrentPassword(password: String) {
        _uiState.value = _uiState.value.copy(currentEditablePassword = password)
    }

    // 开始更新仓库
    fun startUpdate() {
        val currentState = _uiState.value
        val originalRepo = currentState.selectedRepo ?: return
        if (currentState.isUpdating) return

        val repoToUpdate = if (originalRepo.editable) {

            // 只有私有仓库才需要凭证，且只有在用户输入不为空时才设置
            val newCredentials = if (originalRepo.repoType == RepoType.PRIVATE_REPO) {
                mapOf(
                    "username" to currentState.currentEditableUsername,
                    "password" to currentState.currentEditablePassword
                )
            } else {
                null
            }

            originalRepo.copy(
                url = currentState.currentEditableUrl,
                branch = currentState.currentEditableBranch,
                credentials = newCredentials
            )
        } else {
            // 如果不可编辑，则使用原始仓库信息
            originalRepo
        }

        viewModelScope.launch {
            _uiState.value = currentState.copy(
                isUpdating = true,
                logs = "" // 清空旧日志
            )

            // 传递日志回调，将日志追加到UI状态
            // 现在传入的是可能包含用户修改后 URL、Branch 和 Credentials 的 repoToUpdate 对象
            gitRepository.updateRepository(repoToUpdate) { log ->
                val newLog = "${_uiState.value.logs}${log}\n"
                _uiState.value = _uiState.value.copy(logs = newLog)
            }

            _uiState.value = _uiState.value.copy(isUpdating = false)
        }
    }
}

object UpdateRepoViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])

        if (modelClass.isAssignableFrom(UpdateRepoViewModel::class.java)) {
            val gitUpdater = GitUpdater(application.applicationContext)
            val gitRepository: GitRepository = GitRepositoryImpl(gitUpdater)
            val jsonInputStream: InputStream = application.assets.open("git_repos.json")

            @Suppress("UNCHECKED_CAST")
            return UpdateRepoViewModel(gitRepository, jsonInputStream) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}