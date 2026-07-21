package com.accounting.app.capture

import android.app.Notification
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.accounting.app.capture.dispatcher.CaptureDispatcher
import com.accounting.app.capture.extractor.NotificationExtractor
import com.accounting.app.capture.model.PaymentInfo
import com.accounting.app.log.AppLogger

/**
 * 支付通知监听服务
 *
 * 监听微信、支付宝、银行APP的支付通知，提取支付信息后分发到 CaptureDispatcher。
 * 与 PaymentAccessibilityService 共享同一去重机制，实现跨来源去重。
 */
class PaymentNotificationService : NotificationListenerService() {

    private val targetPackages = setOf(
        "com.tencent.mm",                    // 微信
        "com.eg.android.AlipayGphone",       // 支付宝
        "cmb.pb",                            // 招商银行
        "com.chinamworld.main",              // 建设银行
        "com.icbc"                           // 工商银行
    )

    // 支付相关通知关键词（用于快速过滤）
    private val paymentKeywords = listOf(
        "支付成功", "付款成功", "交易成功", "收款",
        "消费", "支出", "交易", "支付金额"
    )

    @Volatile private var extractor: NotificationExtractor? = null
    @Volatile private var dispatcher: CaptureDispatcher? = null

    // 通知级别去重：同一 notificationId 5秒内不重复处理
    private val processedNotifications = mutableMapOf<String, Long>()
    private val NOTIFICATION_DEDUP_MS = 5000L

    override fun onListenerConnected() {
        super.onListenerConnected()
        val requestId = ""
        AppLogger.i(requestId, "NotificationListener", "服务已连接，targetPackages=${targetPackages.joinToString()}")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val pkg = sbn.packageName?.toString() ?: return
        if (pkg !in targetPackages) return

        val notificationId = sbn.id.toString()
        val dedupKey = "${pkg}_$notificationId"

        // 通知级别快速去重
        val now = System.currentTimeMillis()
        val lastProcessTime = processedNotifications[dedupKey]
        if (lastProcessTime != null && now - lastProcessTime < NOTIFICATION_DEDUP_MS) {
            AppLogger.d("", "NotificationListener", "通知去重命中，dedupKey=$dedupKey")
            return
        }
        processedNotifications[dedupKey] = now

        // 清理过期的去重记录
        cleanupProcessedNotifications(now)

        val captureId = generateCaptureId()
        AppLogger.d(captureId, "NotificationListener", "收到通知 pkg=$pkg, id=$notificationId")

        // 提取通知内容
        val (title, text) = extractNotificationContent(sbn.notification)

        // 快速过滤：非支付类通知直接跳过
        val fullText = "$title $text"
        if (!paymentKeywords.any { fullText.contains(it) }) {
            AppLogger.d(captureId, "NotificationListener", "非支付类通知，跳过：$fullText")
            return
        }

        AppLogger.d(captureId, "NotificationListener", "支付类通知通过过滤，开始解析")

        // 解析通知
        val extractor = this.extractor ?: NotificationExtractor().also { this.extractor = it }
        val info = extractor.extract(title, text, pkg, captureId)

        if (info == null) {
            AppLogger.d(captureId, "NotificationListener", "通知解析失败，title=$title, text=$text")
            return
        }

        // 分发到 CaptureDispatcher（跨来源去重）
        val dispatcher = this.dispatcher ?: createDispatcher().also { this.dispatcher = it }
        dispatcher.dispatch(info)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        AppLogger.w("", "NotificationListener", "服务断开连接")
    }

    override fun onDestroy() {
        super.onDestroy()
        AppLogger.w("", "NotificationListener", "服务销毁")
    }

    /**
     * 提取通知标题和内容
     */
    private fun extractNotificationContent(notification: Notification): Pair<String?, String?> {
        val extras = notification.extras ?: return Pair(null, null)

        val title = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            extras.getCharSequence("android.title")?.toString()
        } else {
            null
        }

        val text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            extras.getCharSequence("android.text")?.toString()
        } else {
            null
        }

        return Pair(title, text)
    }

    private fun createDispatcher(): CaptureDispatcher {
        return CaptureDispatcher { info ->
            notificationCallback?.invoke(info)
        }
    }

    private fun generateCaptureId(): String {
        return "notif_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    /**
     * 清理过期的通知去重记录
     */
    private fun cleanupProcessedNotifications(now: Long) {
        val iterator = processedNotifications.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value > NOTIFICATION_DEDUP_MS * 2) {
                iterator.remove()
            }
        }
    }

    companion object {
        private var notificationCallback: ((PaymentInfo) -> Unit)? = null

        fun setNotificationCallback(callback: (PaymentInfo) -> Unit) {
            notificationCallback = callback
        }

        fun clearNotificationCallback() {
            notificationCallback = null
        }

        /**
         * 检测通知监听权限是否已开启
         */
        fun isNotificationListenerEnabled(context: android.content.Context): Boolean {
            val packageName = context.packageName
            val enabledListeners = android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            return enabledListeners.contains(packageName)
        }
    }
}