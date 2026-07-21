package com.accounting.app.capture.extractor

import com.accounting.app.capture.model.PaymentInfo
import com.accounting.app.capture.model.CaptureSource
import org.junit.Test

class NodeExtractorTest {

    @Test
    fun `extract wechat full fields returns complete PaymentInfo`() {
        val extractor = NodeExtractor()
        val rawText = "喜茶\n付款成功\n¥26.80\n2026-07-14 10:23"
        val info = extractor.extractFromText(rawText, "com.tencent.mm", "test_cap_001")

        assert(info != null) { "应返回 PaymentInfo" }
        assert(info?.merchant == "喜茶") { "商户应为喜茶，实际=${info?.merchant}" }
        assert(info?.amount == 2680L) { "金额应为2680分，实际=${info?.amount}" }
        assert(info?.confidence ?: 0f >= 0.8f) { "置信度应≥0.8，实际=${info?.confidence}" }
    }

    @Test
    fun `extract alipay full fields returns complete PaymentInfo`() {
        val extractor = NodeExtractor()
        val rawText = "星巴克\n支付成功\n￥35.50\n2026-07-14 14:30"
        val info = extractor.extractFromText(rawText, "com.eg.android.AlipayGphone", "test_cap_002")

        assert(info != null) { "应返回 PaymentInfo" }
        assert(info?.merchant == "星巴克") { "商户应为星巴克，实际=${info?.merchant}" }
        assert(info?.amount == 3550L) { "金额应为3550分，实际=${info?.amount}" }
    }

    @Test
    fun `extract missing merchant returns partial success with missingFields`() {
        val extractor = NodeExtractor()
        val rawText = "付款成功\n¥26.80"
        val info = extractor.extractFromText(rawText, "com.tencent.mm", "test_cap_003")

        assert(info != null) { "应返回 PaymentInfo（部分成功）" }
        assert(info?.amount == 2680L) { "金额应正确提取" }
    }

    @Test
    fun `validate amount zero returns false`() {
        val extractor = NodeExtractor()
        val info = PaymentInfo(
            source = CaptureSource.ACCESSIBILITY,
            merchant = "测试",
            amount = 0L,
            payTime = System.currentTimeMillis(),
            paymentType = "WECHAT",
            confidence = 1.0f,
            rawText = "",
            captureId = "test_cap_004"
        )
        assert(!extractor.validate(info)) { "金额为0应校验失败" }
    }

    @Test
    fun `validate amount too large returns false`() {
        val extractor = NodeExtractor()
        val info = PaymentInfo(
            source = CaptureSource.ACCESSIBILITY,
            merchant = "测试",
            amount = 10_000_01L,
            payTime = System.currentTimeMillis(),
            paymentType = "WECHAT",
            confidence = 1.0f,
            rawText = "",
            captureId = "test_cap_005"
        )
        assert(!extractor.validate(info)) { "金额>10000应校验失败" }
    }

    @Test
    fun `validate merchant is keyword returns false`() {
        val extractor = NodeExtractor()
        val info = PaymentInfo(
            source = CaptureSource.ACCESSIBILITY,
            merchant = "付款成功",
            amount = 2680L,
            payTime = System.currentTimeMillis(),
            paymentType = "WECHAT",
            confidence = 1.0f,
            rawText = "",
            captureId = "test_cap_006"
        )
        assert(!extractor.validate(info)) { "商户为成功关键词应校验失败" }
    }

    @Test
    fun `validate time too old returns false`() {
        val extractor = NodeExtractor()
        val info = PaymentInfo(
            source = CaptureSource.ACCESSIBILITY,
            merchant = "测试",
            amount = 2680L,
            payTime = System.currentTimeMillis() - 700_000L,
            paymentType = "WECHAT",
            confidence = 1.0f,
            rawText = "",
            captureId = "test_cap_007"
        )
        assert(!extractor.validate(info)) { "时间超过10分钟应校验失败" }
    }

    @Test
    fun `validate valid info returns true`() {
        val extractor = NodeExtractor()
        val info = PaymentInfo(
            source = CaptureSource.ACCESSIBILITY,
            merchant = "喜茶",
            amount = 2680L,
            payTime = System.currentTimeMillis(),
            paymentType = "WECHAT",
            confidence = 1.0f,
            rawText = "",
            captureId = "test_cap_008"
        )
        assert(extractor.validate(info)) { "合法信息应校验通过" }
    }

    @Test
    fun `extract empty rawText returns null`() {
        val extractor = NodeExtractor()
        val info = extractor.extractFromText("", "com.tencent.mm", "test_cap_009")
        assert(info == null) { "空文本应返回null" }
    }

    @Test
    fun `extract only success keyword without amount returns null`() {
        val extractor = NodeExtractor()
        val rawText = "付款成功"
        val info = extractor.extractFromText(rawText, "com.tencent.mm", "test_cap_010")
        assert(info == null) { "仅有成功关键词无金额应返回null" }
    }

    @Test
    fun `extract multiple amounts picks closest to success`() {
        val extractor = NodeExtractor()
        val rawText = "余额 100.00\n付款成功\n¥26.80"
        val info = extractor.extractFromText(rawText, "com.tencent.mm", "test_cap_011")
        assert(info?.amount == 2680L) { "应取最接近'成功'的金额2680，实际=${info?.amount}" }
    }
}
