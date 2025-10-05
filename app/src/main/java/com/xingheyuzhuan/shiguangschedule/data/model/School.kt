// com/xingheyuzhuan/shiguangschedule/data/model/School.kt
package com.xingheyuzhuan.shiguangschedule.data.model

// 导入 Gson 的注解
import com.google.gson.annotations.SerializedName

/**
 * 定义学校的固定类别，确保数据一致性。
 * @param displayName 用于在UI上显示的名称。
 */
enum class SchoolCategory(val displayName: String) {
    BACHELOR_AND_ASSOCIATE("本科/专科"),
    POSTGRADUATE("研究生"),
    GENERAL_TOOL("通用工具");
}

data class School(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("initial") // 学校名称的拼音首字母
    val initial: String,

    @SerializedName("importUrl") // 教务导入的URL
    val importUrl: String,

    @SerializedName("assetJsPath")
    val assetJsPath: String? = null,// 脚本路径

    @SerializedName("maintainer")
    val maintainer: String? = null, // 维护者信息

    @SerializedName("category")
    val category: SchoolCategory = SchoolCategory.GENERAL_TOOL //教务类别
)