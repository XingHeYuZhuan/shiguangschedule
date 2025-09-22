// com/xingheyuzhuan/shiguangschedule/data/model/School.kt
package com.xingheyuzhuan.shiguangschedule.data.model

import kotlinx.serialization.Serializable

@Serializable
data class School(
    val id: String,
    val name: String,
    val initial: String, // 学校名称的拼音首字母或汉字首字母
    val importUrl: String, // 教务导入的URL
    val assetJsPath: String? = null,// 脚本路径
    val maintainer: String? = null // 维护者信息
)