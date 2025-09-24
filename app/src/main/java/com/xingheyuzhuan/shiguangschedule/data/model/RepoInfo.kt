package com.xingheyuzhuan.shiguangschedule.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RepositoryInfo(
    val name: String, // 新增：用于匹配 JSON 的 "name" 键
    @SerialName("type")
    val repoType: RepoType,
    val url: String,
    val branch: String,
    val editable: Boolean, // 新增：用于匹配 JSON 的 "editable" 键
    val credentials: Map<String, String>? = null // 可选，用于私有仓库
)

@Serializable
enum class RepoType {
    OFFICIAL,
    PUBLIC_FORK,
    PRIVATE_REPO,
    CUSTOM
}