// com/xingheyuzhuan/shiguangschedule/data/model/School.kt
package com.xingheyuzhuan.shiguangschedule.data.model

import kotlinx.serialization.Serializable

/**
 * 定义学校的固定类别，确保数据一致性。
 * @param displayName 用于在UI上显示的名称。
 */
@Serializable
enum class SchoolCategory(val displayName: String) {
    BACHELOR_AND_ASSOCIATE("本科/专科"),
    POSTGRADUATE("研究生"),
    GENERAL_TOOL("通用工具");
}

@Serializable
data class School(
    val id: String,
    val name: String,
    val initial: String, // 学校名称的拼音首字母
    val importUrl: String, // 教务导入的URL
    val assetJsPath: String? = null,// 脚本路径
    val maintainer: String? = null, // 维护者信息
    val category: SchoolCategory = SchoolCategory.GENERAL_TOOL //教务类别
)