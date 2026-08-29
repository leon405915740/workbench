package com.accounting.app.notification

import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.accounting.app.MainActivity
import com.accounting.app.data.local.pref.UserPreferences
import com.accounting.app.log.AppLogger
import com.accounting.app.util.AmountUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 通知栏监听：付款后自动唤起记账卡片。
 *
 * 监听微信/支付宝/云闪付等支付应用的「支付成功/付款」通知，解析金额后
 * 带记账预填唤起 [MainActivity]。通过 NotificationListenerService 在系统
 * 绑定后被动接收，不申请额外运行时权限。
 *
 * 节点埋点统一携带临时链路 ID（ntf_<sbn.key.hashCode()>_<时间戳>，整个通知全链路复用）与 node=通知监听。
 */
class QuickRecordNotificationService : NotificationListenerService() {

    private companion object {
        const val NODE = "通知监听"

        /** 支付应用包名白名单（命中任一即视为支付通知） */
        val PAYMENT_PACKAGES = setOf(
            "com.tencent.mm",                    // 微信
            "com.eg.android.AlipayGphone",       // 支付宝
            "com.alipay.android.phone.thirdparty.msp", // 支付宝
            "com.union.pay",                     // 云闪付
            "com.unionpay"                       // 银联
        )

        /** 标题/内容中含以下任一关键词视为支付相关 */
        val PAYMENT_KEYWORDS = listOf("微信支付", "支付宝", "云闪付", "银联", "支付成功", "交易成功", "扣款")

        /** 支出动作关键词：命中任一才确定为「支出」 */
        val EXPENSE_KEYWORDS = listOf("付款", "支付成功", "交易成功", "扣款", "支出", "消费")

        /** 收入/退款关键词：命中任一则跳过（避免把收入当成支出记） */
        val NON_EXPENSE_KEYWORDS = listOf("收款", "收入", "到账", "退款", "入账", "红包", "赞赏")

        /** 通知去重窗口：同一去重键在此窗口内再次回调直接忽略（不打日志、不透传业务流） */
        const val NOTIFY_DEDUP_WINDOW_MS = 2000L

        /** 去重缓存容量（LRU 淘汰上限） */
        const val NOTIFY_DEDUP_CACHE_SIZE = 64

        /** 内容指纹取标题+文本的前缀长度 */
        const val CONTENT_DIGEST_LENGTH = 200

        /** 去重缓存项：记录首次回调时间戳及是否已被本链路处理 */
        private data class DedupEntry(val timestamp: Long, var processed: Boolean)

        private val dedupLock = Any()

        /**
         * 去重缓存：LRU（accessOrder=true，访问即置顶）。
         * key = pkg | sbn.key | 内容指纹；value = [DedupEntry]。
         * 注：sbn.key 即 StatusBarNotification.getKey()，系统保证同一通知多次回调时 key 稳定唯一。
         */
        private val dedupCache = object : LinkedHashMap<String, DedupEntry>(16, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, DedupEntry>?
            ): Boolean = size > NOTIFY_DEDUP_CACHE_SIZE
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName

        // 合并标题 + 文本用于匹配
        val extra = sbn.notification.extras
        val title = extra.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extra.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString().orEmpty()
        val full = "$title $text"

        // 计算去重键（在生成 requestId 之前做入口去重前置）。
        // 去重键 = pkg | sbn.key | 内容指纹；sbn.key 即 StatusBarNotification.getKey()，
        // 系统保证同一通知多次回调时 key 稳定唯一。内容指纹=标题+文本摘要(前200字符)，若可提取金额则追加金额，
        // 仅全同才判重，避免同 key 不同金额/内容的通知被误杀（防漏单）。
        val amountForDedup = AmountUtils.extractFenFromAmountText(text)
        val contentFingerprint = if (amountForDedup != null) {
            full.take(CONTENT_DIGEST_LENGTH) + "_" + amountForDedup
        } else {
            full.take(CONTENT_DIGEST_LENGTH)
        }
        val dedupKey = "$packageName|${sbn.key}|$contentFingerprint"

        // 入口去重前置：同一去重键在去重窗口内重复回调直接忽略，不打任何日志、不透传业务流
        val now = System.currentTimeMillis()
        synchronized(dedupLock) {
            val entry = dedupCache[dedupKey]
            if (entry != null && now - entry.timestamp < NOTIFY_DEDUP_WINDOW_MS) {
                return
            }
            dedupCache[dedupKey] = DedupEntry(now, false)
        }

        // 临时链路 ID：本通知全链路复用，不再二次生成
        val requestId = "ntf_${sbn.key.hashCode()}_$now"

        // 1. 判断是否支付通知（包名 或 关键词）；「收到推送」与「非支付跳过」合并为单行
        val isPayment = PAYMENT_PACKAGES.contains(packageName) ||
            PAYMENT_KEYWORDS.any { full.contains(it) }
        if (!isPayment) {
            AppLogger.d(requestId, NODE, "忽略通知: pkg=$packageName, reason=非支付")
            return
        }

        // 2. 判断支出方向：必须是「付款/支付成功」类，避免把收款/退款当支出
        val hasExpenseAction = EXPENSE_KEYWORDS.any { full.contains(it) }
        val hasNonExpense = NON_EXPENSE_KEYWORDS.any { full.contains(it) }
        if (!hasExpenseAction || hasNonExpense) {
            AppLogger.d(requestId, NODE, "忽略通知: pkg=$packageName, reason=非支出方向, title=$title, text=$text")
            return
        }

        // 3. 提取金额
        val amountFen = AmountUtils.extractFenFromAmountText(text) ?: run {
            AppLogger.d(requestId, NODE, "忽略通知: pkg=$packageName, reason=未解析到金额, text=$text")
            return
        }

        // 去重键已含金额/内容指纹，走到此处即确认为需处理的通知，标记已处理
        synchronized(dedupLock) {
            dedupCache[dedupKey]?.processed = true
        }

        // 4. 门店开关：关闭则跳过
        scope.launch {
            try {
                val enabled = UserPreferences(applicationContext).getQuickRecordEnabled().first()
                if (!enabled) {
                    AppLogger.d(requestId, NODE, "快捷记账已关闭，跳过: amount=${amountFen}分")
                    return@launch
                }
                launchQuickRecord(requestId, amountFen, title)
            } catch (e: Exception) {
                AppLogger.e(requestId, NODE, "读取开关异常: ${e.message}", e)
            }
        }
    }

    private fun launchQuickRecord(requestId: String, amountFen: Long, title: String) {
        val carryTitle = title.ifBlank { "快捷记账" }
        // Android 10+ 后台 startActivity 限制：从通知监听服务唤起 Activity 需悬浮窗权限豁免，
        // 否则 startActivity 调用会被系统静默拦截（不抛异常但 Activity 不起来）。
        if (!Settings.canDrawOverlays(applicationContext)) {
            AppLogger.w(
                requestId, NODE,
                "悬浮窗权限未开启，跳过唤起（Android 10+ 后台启动限制）: amount=${amountFen}分, merchant=$carryTitle"
            )
            return
        }
        AppLogger.d(requestId, NODE, "唤起记账卡片: amount=${amountFen}分, merchant=$carryTitle")
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(MainActivity.EXTRA_QUICK_PAYMENT_AMOUNT, amountFen)
            putExtra(MainActivity.EXTRA_QUICK_PAYMENT_MERCHANT, carryTitle)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            AppLogger.e(requestId, NODE, "唤起记账异常: ${e.message}", e)
        }
    }

    override fun onListenerConnected() {
        AppLogger.i("", NODE, "通知监听服务已连接")
    }
}