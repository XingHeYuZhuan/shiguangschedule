package com.xingheyuzhuan.shiguangschedule.ui.settings.update

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.data.model.RepoType
import com.xingheyuzhuan.shiguangschedule.data.model.RepositoryInfo
import com.xingheyuzhuan.shiguangschedule.data.repository.GitRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
open class UpdateRepoViewModel(
    private val gitRepository: GitRepositoryImpl,
    private val application: Application
) : ViewModel() {

    // UI状态，包含可供选择的仓库列表、当前选择的仓库和日志
    data class UpdateRepoState(
        val repoList: List<RepositoryInfo> = emptyList(),
        val selectedRepo: RepositoryInfo? = null,
        val logs: String = "",
        val isUpdating: Boolean = false,

        // URL 和 Branch 的编辑状态
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
                val jsonInputStream = application.assets.open("git_repos.json")
                val jsonString = jsonInputStream.reader().use { it.readText() }
                val repos = Json.decodeFromString<List<RepositoryInfo>>(jsonString)
                val defaultRepo = repos.firstOrNull() // 默认选中的仓库

                // 辅助函数，安全地从 credentials map 中提取值
                fun getCredentialValue(key: String): String = defaultRepo?.credentials?.get(key) ?: ""

                _uiState.update { currentState ->
                    currentState.copy(
                        repoList = repos,
                        selectedRepo = defaultRepo,
                        currentEditableUrl = defaultRepo?.url ?: "",
                        currentEditableBranch = defaultRepo?.branch ?: "",
                        currentEditableUsername = getCredentialValue("username"),
                        currentEditablePassword = getCredentialValue("password")
                    )
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(logs = "错误：加载仓库列表失败。\n${e.message}")
                }
            }
        }
    }

    // 更新当前选择的仓库
    fun selectRepository(repo: RepositoryInfo) {
        fun getCredentialValue(key: String): String = repo.credentials?.get(key) ?: ""

        _uiState.update { currentState ->
            currentState.copy(
                selectedRepo = repo,
                currentEditableUrl = repo.url,
                currentEditableBranch = repo.branch,
                currentEditableUsername = getCredentialValue("username"),
                currentEditablePassword = getCredentialValue("password")
            )
        }
    }

    // 更新当前编辑的 URL
    fun updateCurrentUrl(url: String) {
        _uiState.update { it.copy(currentEditableUrl = url) }
    }

    // 更新当前编辑的 Branch
    fun updateCurrentBranch(branch: String) {
        _uiState.update { it.copy(currentEditableBranch = branch) }
    }

    // 更新当前编辑的 Username/Token Key
    fun updateCurrentUsername(username: String) {
        _uiState.update { it.copy(currentEditableUsername = username) }
    }

    // 更新当前编辑的 Password/Token Value
    fun updateCurrentPassword(password: String) {
        _uiState.update { it.copy(currentEditablePassword = password) }
    }

    // 开始更新仓库
    fun startUpdate() {
        val currentState = _uiState.value
        val originalRepo = currentState.selectedRepo ?: return
        if (currentState.isUpdating) return

        val repoToUpdate = if (originalRepo.editable) {
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
            originalRepo
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUpdating = true,
                    logs = ""
                )
            }

            gitRepository.updateRepository(repoToUpdate) { log ->
                _uiState.update { state ->
                    val currentLogs = state.logs

                    val newLogs = if (log.startsWith("\r")) {
                        val cleanLog = log.removePrefix("\r")
                        val lastNewlineIndex = currentLogs.lastIndexOf('\n')
                        if (lastNewlineIndex != -1) {
                            currentLogs.substring(0, lastNewlineIndex + 1) + cleanLog
                        } else {
                            cleanLog
                        }
                    } else {
                        if (currentLogs.isEmpty()) {
                            log
                        } else if (currentLogs.endsWith("\n")) {
                            currentLogs + log
                        } else {
                            "$currentLogs\n$log"
                        }
                    }

                    state.copy(logs = newLogs)
                }
            }

            _uiState.update { it.copy(isUpdating = false) }
        }
    }
}