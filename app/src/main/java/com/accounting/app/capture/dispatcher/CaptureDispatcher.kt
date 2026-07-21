package com.accounting.app.capture.dispatcher

import com.accounting.app.capture.model.CaptureSource
import com.accounting.app.capture.model.PaymentInfo
import com.accounting.app.log.AppLogger
import java.util.concurrent.ConcurrentHashMap

class CaptureDispatcher(
    private val onDispatch: (PaymentInfo) -> Unit
) {

    private val dedupCache = ConcurrentHashMap<String, Long>()

    /**
     * 跨来源去重时间窗口
     *
     * 无障碍从支付成功页提取信息，通知可能在 10-30 秒后才到达。
     * 60 秒窗口足够覆盖大部分免密支付场景的延迟差。
     */
    private val CROSS_SOURCE_DEDUP_WINDOW_MS = 60_000L

    fun dispatch(info: PaymentInfo) {
        val dedupKey = info.dedupKey
        val now = System.currentTimeMillis()

        val lastProcessTime = dedupCache[dedupKey]
        if (lastProcessTime != null && now - lastProcessTime < CROSS_SOURCE_DEDUP_WINDOW_MS) {
            AppLogger.d(info.captureId, "AutoCapture_Dispatch", 
                "去重命中，dedupKey=$dedupKey，距离上次${now - lastProcessTime}ms，source=${info.source}")
            return
        }

        dedupCache[dedupKey] = now
        
        // 清理过期的去重缓存（超过窗口时间 2 倍的记录）
        cleanupDedupCache(now)
        
        AppLogger.i(info.captureId, "AutoCapture_Dispatch", 
            "分发成功，merchant=${info.merchant}, amount=${info.amount}, dedupKey=$dedupKey, source=${info.source}")
        onDispatch(info)
    }

    /**
     * 清理过期的去重缓存
     */
    private fun cleanupDedupCache(now: Long) {
        val iterator = dedupCache.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value > CROSS_SOURCE_DEDUP_WINDOW_MS * 2) {
                iterator.remove()
            }
        }
    }
}
