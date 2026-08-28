package com.accounting.app.notification

import android.content.Intent
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
 * 节点埋点统一携带 requestId（每次通知事件一条）与 node=通知监听。
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

        /** 最近一次已处理的通知 key，避免同一通知重复唤起 */
        @Volatile
        var lastProcessedKey: String = ""
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val requestId = AppLogger.generateRequestId()
        val packageName = sbn.packageName
        AppLogger.d(requestId, NODE, "收到通知推送: pkg=$packageName, key=${sbn.key}")

        // 合并标题 + 文本用于匹配
        val extra = sbn.notification.extras
        val title = extra.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extra.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString().orEmpty()
        val full = "$title $text"

        // 1. 判断是否支付通知（包名 或 关键词）
        val isPayment = PAYMENT_PACKAGES.contains(packageName) ||
            PAYMENT_KEYWORDS.any { full.contains(it) }
        if (!isPayment) {
            AppLogger.d(requestId, NODE, "非支付通知，跳过: pkg=$packageName")
            return
        }

        // 2. 判断支出方向：必须是「付款/支付成功」类，避免把收款/退款当支出
        val hasExpenseAction = EXPENSE_KEYWORDS.any { full.contains(it) }
        val hasNonExpense = NON_EXPENSE_KEYWORDS.any { full.contains(it) }
        if (!hasExpenseAction || hasNonExpense) {
            AppLogger.d(requestId, NODE, "非支出方向，跳过: title=$title, text=$text")
            return
        }

        // 3. 提取金额
        val amountFen = AmountUtils.extractFenFromAmountText(text) ?: run {
            AppLogger.d(requestId, NODE, "未解析到金额，跳过: text=$text")
            return
        }

        // 4. 去重：同一通知事件只处理一次
        val dedupKey = packageName + "|" + sbn.key
        if (dedupKey == lastProcessedKey) {
            AppLogger.d(requestId, NODE, "重复通知，跳过: $dedupKey, amount=${amountFen}分")
            return
        }
        lastProcessedKey = dedupKey

        // 5. 门店开关：关闭则跳过
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