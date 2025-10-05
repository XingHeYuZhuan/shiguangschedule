package com.xingheyuzhuan.shiguangschedule.data.model

import com.google.gson.annotations.SerializedName

/**
 * 贡献者列表的顶级数据结构。
 * 对应整个 JSON 根对象，并在内部嵌套了 Contributor 数据类。
 */
data class ContributionList(
    // 使用 @SerializedName 将 JSON 中的 "app_dev" 键映射到 Kotlin 的命名规范 appDev
    @SerializedName("app_dev")
    val appDev: List<Contributor>,

    // 使用 @SerializedName 将 JSON 中的 "jiaowu_adapter" 键映射到 Kotlin 的命名规范 jiaowuAdapter
    @SerializedName("jiaowu_adapter")
    val jiaowuadapter: List<Contributor>
) {
    /**
     * 单个贡献者基础信息数据结构。
     * 作为嵌套类，它清晰地表明了其只为 ContributionList 数据服务。
     * 这也保证了 Gson 解析的非严格模式（只获取定义的字段）。
     */
    data class Contributor(
        @SerializedName("name")
        val name: String,

        @SerializedName("url")
        val url: String,

        @SerializedName("avatar")
        val avatar: String
    )
}