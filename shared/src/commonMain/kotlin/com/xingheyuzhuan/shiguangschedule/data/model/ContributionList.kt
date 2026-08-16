package com.xingheyuzhuan.shiguangschedule.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 贡献者列表的顶级数据结构。
 * 对应整个 JSON 根对象。
 */
@Serializable
data class ContributionList(
    @SerialName("app_dev")
    val appDev: List<Contributor>,

    @SerialName("jiaowu_adapter")
    val jiaowuadapter: List<Contributor>
) {
    /**
     * 单个贡献者基础信息数据结构。
     */
    @Serializable // 4. 嵌套类也必须添加此注解
    data class Contributor(
        @SerialName("name")
        val name: String,

        @SerialName("url")
        val url: String,

        @SerialName("avatar")
        val avatar: String
    )
}