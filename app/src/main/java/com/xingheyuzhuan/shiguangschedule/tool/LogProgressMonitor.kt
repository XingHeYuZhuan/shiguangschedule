package com.xingheyuzhuan.shiguangschedule.tool

import org.eclipse.jgit.lib.ProgressMonitor
import java.util.Locale

/**
 * 一个用于实时输出Git操作进度、日志和耗时的监听器。
 * 它实现了JGit的ProgressMonitor接口，将JGit内部的进度信息转换成可读的字符串，并通过回调函数传递。
 */
class LogProgressMonitor(private val onLog: (String) -> Unit) : ProgressMonitor {

    private var showDuration = false
    private val taskStartTimes = mutableMapOf<String, Long>()

    private var currentTotalWork = 0
    private var currentCompleted = 0
    private var currentTitle = ""

    override fun start(totalTasks: Int) {
        onLog("正在开始Git操作，总共包含 $totalTasks 个任务。")
    }

    override fun beginTask(title: String, totalWork: Int) {
        this.currentTitle = title
        this.currentTotalWork = totalWork
        this.currentCompleted = 0

        taskStartTimes[title] = System.currentTimeMillis() // 记录任务开始时间

        if (totalWork == ProgressMonitor.UNKNOWN) {
            onLog("任务：$title，进度未知。")
        } else {
            onLog("正在开始任务：$title，总进度：$totalWork。")
        }
    }

    override fun update(completed: Int) {
        this.currentCompleted += completed
        val percentage = if (currentTotalWork > 0) (this.currentCompleted * 100) / currentTotalWork else 0
        onLog("任务：$currentTitle，已完成 $this.currentCompleted/$currentTotalWork，当前进度：$percentage%。")
    }

    override fun endTask() {
        val endTime = System.currentTimeMillis()
        val duration = taskStartTimes[currentTitle]?.let { endTime - it } // 计算耗时

        val durationMessage = if (showDuration && duration != null) {
            val seconds = duration / 1000.0
            String.format(Locale.ROOT, "，耗时：%.2f 秒。", seconds)
        } else {
            ""
        }

        onLog("任务：$currentTitle，已完成$durationMessage")
    }

    override fun isCancelled(): Boolean {
        // 你可以在这里添加逻辑来支持任务取消
        return false
    }

    override fun showDuration(enabled: Boolean) {
        this.showDuration = enabled
        // 这是一个 JGit 接口方法，我们在内部使用它的值来控制是否显示耗时。
    }
}