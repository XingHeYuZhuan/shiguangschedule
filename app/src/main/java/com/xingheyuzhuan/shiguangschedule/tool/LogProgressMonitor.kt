// LogProgressMonitor.kt

package com.xingheyuzhuan.shiguangschedule.tool

import org.eclipse.jgit.lib.ProgressMonitor
import java.util.Locale

/**
 * 这是一个详细的 JGit ProgressMonitor，它将所有的进度信息直接路由到 onLog 回调，
 * 从而在 UI 上实时显示 Git 操作的全过程。
 */
class LogProgressMonitor(private val onLog: (String) -> Unit) : ProgressMonitor {

    private var currentTotalWork = 0
    private var currentCompleted = 0
    private var currentTitle = ""

    override fun start(totalTasks: Int) {
        onLog("[Git] 操作开始，总共包含 $totalTasks 个任务。")
    }

    override fun beginTask(title: String, totalWork: Int) {
        this.currentTitle = title
        this.currentTotalWork = totalWork
        this.currentCompleted = 0

        val totalMsg = if (totalWork == ProgressMonitor.UNKNOWN) {
            "未知进度"
        } else {
            "总进度: $totalWork"
        }

        // 直接输出任务开始信息
        onLog("[任务] $title - $totalMsg")
    }

    override fun update(completed: Int) {
        this.currentCompleted += completed

        // 只有当有总进度信息时，才输出百分比，否则只显示已完成的工作量
        val percentage = if (currentTotalWork > 0) {
            val p = (this.currentCompleted * 100) / currentTotalWork
            " ($p%)"
        } else {
            ""
        }

        // 输出详细的更新信息
        onLog(String.format(Locale.ROOT, "  - 进度: %s %d/%d%s",
            currentTitle, this.currentCompleted, currentTotalWork, percentage
        ))
    }

    override fun endTask() {
        // 输出任务结束信息
        onLog("[任务] $currentTitle - 已完成。")
    }

    override fun isCancelled(): Boolean {
        return false // 暂不支持取消
    }

    override fun showDuration(enabled: Boolean) {
        // JGit 接口方法，暂时不处理
    }
}