package com.xingheyuzhuan.shiguangschedule

class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return sayHello(platform.name)
    }
}