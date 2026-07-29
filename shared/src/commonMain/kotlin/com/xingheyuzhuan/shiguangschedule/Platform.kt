package com.xingheyuzhuan.shiguangschedule

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform