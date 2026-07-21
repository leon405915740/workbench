package com.accounting.app.capture.dispatcher

import com.accounting.app.capture.model.PaymentInfo
import com.accounting.app.capture.model.CaptureSource
import org.junit.Test

class CaptureDispatcherTest {

    @Test
    fun `dispatch same dedupKey within 60 seconds should be deduplicated`() {
        var dispatchCount = 0
        val dispatcher = CaptureDispatcher { dispatchCount++ }

        val info1 = createPaymentInfo("喜茶", 2680L, "test_cap_001", CaptureSource.ACCESSIBILITY)
        dispatcher.dispatch(info1)

        val info2 = createPaymentInfo("喜茶", 2680L, "test_cap_002", CaptureSource.ACCESSIBILITY)
        dispatcher.dispatch(info2)

        assert(dispatchCount == 1) { "60秒内相同dedupKey应只分发1次，实际=$dispatchCount" }
    }

    @Test
    fun `dispatch same dedupKey after 60 seconds should be allowed`() {
        var dispatchCount = 0
        val dispatcher = CaptureDispatcher { dispatchCount++ }

        val info1 = createPaymentInfo("喜茶", 2680L, "test_cap_003", CaptureSource.ACCESSIBILITY)
        dispatcher.dispatch(info1)

        Thread.sleep(61000)

        val info2 = createPaymentInfo("喜茶", 2680L, "test_cap_004", CaptureSource.ACCESSIBILITY)
        dispatcher.dispatch(info2)

        assert(dispatchCount == 2) { "60秒后相同dedupKey应允许分发，实际=$dispatchCount" }
    }

    @Test
    fun `dispatch different merchant should not be deduplicated`() {
        var dispatchCount = 0
        val dispatcher = CaptureDispatcher { dispatchCount++ }

        val info1 = createPaymentInfo("喜茶", 2680L, "test_cap_005", CaptureSource.ACCESSIBILITY)
        dispatcher.dispatch(info1)

        val info2 = createPaymentInfo("星巴克", 2680L, "test_cap_006", CaptureSource.ACCESSIBILITY)
        dispatcher.dispatch(info2)

        assert(dispatchCount == 2) { "不同商户不应被去重，实际=$dispatchCount" }
    }

    @Test
    fun `dispatch different amount should not be deduplicated`() {
        var dispatchCount = 0
        val dispatcher = CaptureDispatcher { dispatchCount++ }

        val info1 = createPaymentInfo("喜茶", 2680L, "test_cap_007", CaptureSource.ACCESSIBILITY)
        dispatcher.dispatch(info1)

        val info2 = createPaymentInfo("喜茶", 3550L, "test_cap_008", CaptureSource.ACCESSIBILITY)
        dispatcher.dispatch(info2)

        assert(dispatchCount == 2) { "不同金额不应被去重，实际=$dispatchCount" }
    }

    @Test
    fun `cross source deduplication - accessibility first then notification`() {
        var dispatchCount = 0
        val dispatcher = CaptureDispatcher { dispatchCount++ }

        // 无障碍先触发
        val info1 = createPaymentInfo("喜茶", 2680L, "test_cap_009", CaptureSource.ACCESSIBILITY)
        dispatcher.dispatch(info1)

        // 通知后触发（相同商户+金额，应该被去重）
        val info2 = createPaymentInfo("喜茶", 2680L, "test_cap_010", CaptureSource.NOTIFICATION)
        dispatcher.dispatch(info2)

        assert(dispatchCount == 1) { "跨来源去重：无障碍先触发后，通知应被去重，实际=$dispatchCount" }
    }

    @Test
    fun `cross source deduplication - notification first then accessibility`() {
        var dispatchCount = 0
        val dispatcher = CaptureDispatcher { dispatchCount++ }

        // 通知先触发
        val info1 = createPaymentInfo("喜茶", 2680L, "test_cap_011", CaptureSource.NOTIFICATION)
        dispatcher.dispatch(info1)

        // 无障碍后触发（相同商户+金额，应该被去重）
        val info2 = createPaymentInfo("喜茶", 2680L, "test_cap_012", CaptureSource.ACCESSIBILITY)
        dispatcher.dispatch(info2)

        assert(dispatchCount == 1) { "跨来源去重：通知先触发后，无障碍应被去重，实际=$dispatchCount" }
    }

    private fun createPaymentInfo(merchant: String, amount: Long, captureId: String, source: CaptureSource): PaymentInfo {
        return PaymentInfo(
            source = source,
            merchant = merchant,
            amount = amount,
            payTime = System.currentTimeMillis(),
            paymentType = "WECHAT",
            confidence = 1.0f,
            rawText = "",
            captureId = captureId
        )
    }
}