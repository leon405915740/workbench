package com.accounting.app.capture

import com.accounting.app.MainActivity
import com.accounting.app.capture.model.PaymentInfo
import com.accounting.app.log.AppLogger
import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class PaymentAccessibilityService : AccessibilityService() {

    private val targetPackages = setOf(
        "com.tencent.mm",
        "com.eg.android.AlipayGphone"
    )

    private val deduplicator = WindowChangeDeduplicator()
    private val handler = Handler(Looper.getMainLooper())

    @Volatile private var detector: PaymentDetector? = null
    @Volatile private var nodeExtractor: com.accounting.app.capture.extractor.NodeExtractor? = null
    @Volatile private var dispatcher: com.accounting.app.capture.dispatcher.CaptureDispatcher? = null

    private var pendingRunnable: Runnable? = null
    private var currentCaptureId: String? = null

    @Volatile private var lockedWindowId: Int = -1
    @Volatile private var lockedPackageName: String? = null
    @Volatile private var lockReleaseTime: Long = 0L

    private val INITIAL_DELAY_MS = 300L
    private val MAX_RETRY_COUNT = 4
    private val RETRY_DELAYS = longArrayOf(100L, 200L, 400L, 800L)
    private val LOCK_DURATION_ON_SUCCESS_MS = 10_000L
    private val LOCK_DURATION_ON_FAILURE_MS = 5_000L

    override fun onServiceConnected() {
        super.onServiceConnected()
        val startupRequestId = ""
        AppLogger.i(startupRequestId, "AutoCapture_Service", "服务已连接，targetPackages=${targetPackages.joinToString()}")
        startForegroundService()
    }

    private fun startForegroundService() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notification = CaptureNotificationManager.createForegroundNotification(this)
                startForeground(CaptureNotificationManager.getForegroundNotificationId(), notification)
            }
            AppLogger.i("", "AutoCapture_Service", "前台服务已启动")
        } catch (e: Exception) {
            AppLogger.e("", "AutoCapture_Service", "启动前台服务失败: ${e.message}", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (pkg !in targetPackages) return

        val windowId = event.windowId

        if (isWindowLocked(windowId, pkg)) {
            AppLogger.d("", "AutoCapture_Service", "丢弃事件，原因：windowId=$windowId 正在处理中")
            return
        }

        if (!deduplicator.shouldProcess(windowId, pkg)) {
            return
        }

        val captureId = generateCaptureId()
        AppLogger.d(captureId, "AutoCapture_Service", "收到事件 pkg=$pkg, windowId=$windowId, eventType=${event.eventType}")

        lockWindow(windowId, pkg)

        currentCaptureId = captureId
        cancelPendingTask()

        pendingRunnable = Runnable {
            tryProcessPaymentPage(pkg, captureId, 0)
        }
        handler.postDelayed(pendingRunnable!!, INITIAL_DELAY_MS)
        AppLogger.d(captureId, "AutoCapture_Service", "延迟 ${INITIAL_DELAY_MS}ms 后处理")
    }

    private fun isWindowLocked(windowId: Int, packageName: String): Boolean {
        val now = System.currentTimeMillis()
        return lockedWindowId == windowId &&
                lockedPackageName == packageName &&
                now < lockReleaseTime
    }

    private fun lockWindow(windowId: Int, packageName: String) {
        lockedWindowId = windowId
        lockedPackageName = packageName
        lockReleaseTime = Long.MAX_VALUE
        AppLogger.d("", "AutoCapture_Service", "锁定窗口: windowId=$windowId, pkg=$packageName")
    }

    private fun unlockWindow(success: Boolean) {
        val duration = if (success) LOCK_DURATION_ON_SUCCESS_MS else LOCK_DURATION_ON_FAILURE_MS
        lockReleaseTime = System.currentTimeMillis() + duration
        AppLogger.d("", "AutoCapture_Service", "释放窗口锁: windowId=$lockedWindowId, 成功=$success, 锁定${duration}ms")
    }

    private fun forceUnlockWindow() {
        lockReleaseTime = 0L
        AppLogger.d("", "AutoCapture_Service", "强制释放窗口锁: windowId=$lockedWindowId")
    }

    private fun tryProcessPaymentPage(pkg: String, captureId: String, retryCount: Int) {
        val rootNode = try {
            rootInActiveWindow
        } catch (e: Exception) {
            AppLogger.e(captureId, "AutoCapture_Service", "获取根节点失败: ${e.message}", e)
            unlockWindow(false)
            return
        }

        if (rootNode == null) {
            if (retryCount < MAX_RETRY_COUNT) {
                scheduleRetry(pkg, captureId, retryCount)
            } else {
                unlockWindow(false)
            }
            return
        }

        val result = handlePotentialPaymentPage(rootNode, pkg, captureId)

        if (result) {
            unlockWindow(true)
        } else if (retryCount < MAX_RETRY_COUNT) {
            scheduleRetry(pkg, captureId, retryCount)
        } else {
            unlockWindow(false)
        }
    }

    private fun scheduleRetry(pkg: String, captureId: String, retryCount: Int) {
        val delay = RETRY_DELAYS[retryCount]
        pendingRunnable = Runnable {
            AppLogger.d(captureId, "AutoCapture_Service", "重试第${retryCount + 1}次，延迟${delay}ms")
            tryProcessPaymentPage(pkg, captureId, retryCount + 1)
        }
        handler.postDelayed(pendingRunnable!!, delay)
    }

    private fun cancelPendingTask() {
        pendingRunnable?.let {
            handler.removeCallbacks(it)
            pendingRunnable = null
        }
    }

    override fun onDestroy() {
        cancelPendingTask()
        forceUnlockWindow()
        super.onDestroy()
        AppLogger.w("", "AutoCapture_Service", "服务销毁")
    }

    override fun onInterrupt() {
        cancelPendingTask()
        forceUnlockWindow()
        AppLogger.w("", "AutoCapture_Service", "服务被系统中断")
    }

    private fun handlePotentialPaymentPage(rootNode: AccessibilityNodeInfo, pkg: String, captureId: String): Boolean {
        val detector = this.detector ?: PaymentDetector().also { this.detector = it }
        val extractor = this.nodeExtractor
            ?: com.accounting.app.capture.extractor.NodeExtractor().also { this.nodeExtractor = it }

        if (!detector.isPaymentSuccessPage(rootNode, pkg, captureId)) {
            return false
        }

        val info = extractor.extract(rootNode, pkg, captureId) ?: return false

        val dispatcher = this.dispatcher ?: createDispatcher().also { this.dispatcher = it }
        dispatcher.dispatch(info)
        return true
    }

    private fun createDispatcher(): com.accounting.app.capture.dispatcher.CaptureDispatcher {
        return com.accounting.app.capture.dispatcher.CaptureDispatcher { info ->
            launchMainActivity(info)
        }
    }

    /**
     * 检测到支付成功后，把记账 App 拉到前台并携带 PaymentInfo。
     *
     * MainActivity launchMode=singleTask，已在运行则走 onNewIntent，
     * 未运行则 onCreate。两条路径都从 Intent extra 取 PaymentInfo 弹记账确认窗。
     */
    private fun launchMainActivity(info: PaymentInfo) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(MainActivity.EXTRA_PAYMENT_INFO, info)
        }
        startActivity(intent)
        AppLogger.i(
            info.captureId, "AutoCapture_Launch",
            "跳转记账前台：merchant=${info.merchant}, amount=${info.amount}"
        )
    }

    private fun generateCaptureId(): String {
        return "cap_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    companion object {
        fun isAccessibilityServiceEnabled(androidContext: android.content.Context, serviceClass: Class<*>): Boolean {
            val serviceName = serviceClass.name
            val expectedComponentName = android.content.ComponentName(androidContext, serviceClass).flattenToString()

            val enabled = android.provider.Settings.Secure.getString(
                androidContext.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val splitter = android.text.TextUtils.SimpleStringSplitter(':')
            splitter.setString(enabled)
            while (splitter.hasNext()) {
                val componentName = android.content.ComponentName.unflattenFromString(splitter.next())
                if (componentName != null && componentName.flattenToString() == expectedComponentName) {
                    return true
                }
            }
            return false
        }
    }
}
