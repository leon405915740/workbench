package com.accounting.app.capture

import com.accounting.app.log.AppLogger

/**
 * 窗口变化去重器
 *
 * 微信/支付宝支付成功页可能触发多次 TYPE_WINDOW_CONTENT_CHANGED 事件，
 * 在 500ms 内相同 windowId + packageName 组合只处理一次，降低 CPU 占用。
 */
class WindowChangeDeduplicator(
    private val minIntervalMs: Long = 500L
) {
    private val lock = Any()

    @Volatile private var lastWindowId: Int = -1
    @Volatile private var lastPackageName: String? = null
    @Volatile private var lastProcessTime: Long = 0L

    /**
     * 判断当前事件是否应处理
     *
     * @param windowId 无障碍事件 windowId
     * @param packageName 当前窗口包名
     * @return true = 应处理，false = 应跳过
     */
    fun shouldProcess(windowId: Int, packageName: String?): Boolean {
        synchronized(lock) {
            val now = System.currentTimeMillis()
            if (windowId == lastWindowId &&
                packageName == lastPackageName &&
                now - lastProcessTime < minIntervalMs
            ) {
                AppLogger.d("", "WindowDeduplicator", "跳过重复窗口: windowId=$windowId, package=$packageName, 间隔=${System.currentTimeMillis() - lastProcessTime}ms")
                return false
            }
            lastWindowId = windowId
            lastPackageName = packageName
            lastProcessTime = now
            AppLogger.d("", "WindowDeduplicator", "处理新窗口: windowId=$windowId, package=$packageName")
            return true
        }
    }
}
