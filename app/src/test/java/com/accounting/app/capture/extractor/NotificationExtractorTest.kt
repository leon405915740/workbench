package com.accounting.app.capture.extractor

import com.accounting.app.capture.model.CaptureSource
import org.junit.Test

class NotificationExtractorTest {

    private val extractor = NotificationExtractor()

    @Test
    fun `extract wechat payment notification with symbol`() {
        val title = "微信支付"
        val text = "付款成功 ¥26.80 商户：喜茶"
        val result = extractor.extract(title, text, "com.tencent.mm", "test_001")

        assert(result != null) { "应成功解析" }
        assert(result!!.amount == 2680L) { "金额应为2680分，实际=${result.amount}" }
        assert(result.merchant == "喜茶") { "商户应为喜茶，实际=${result.merchant}" }
        assert(result.source == CaptureSource.NOTIFICATION) { "来源应为NOTIFICATION" }
    }

    @Test
    fun `extract alipay payment notification`() {
        val title = "支付宝"
        val text = "支付成功 金额26.80元 收款方：星巴克"
        val result = extractor.extract(title, text, "com.eg.android.AlipayGphone", "test_002")

        assert(result != null) { "应成功解析" }
        assert(result!!.amount == 2680L) { "金额应为2680分，实际=${result.amount}" }
        assert(result.merchant == "星巴克") { "商户应为星巴克，实际=${result.merchant}" }
    }

    @Test
    fun `extract bank notification with amount keyword`() {
        val title = "招商银行"
        val text = "交易通知 消费金额：¥128.50"
        val result = extractor.extract(title, text, "cmb.pb", "test_003")

        assert(result != null) { "应成功解析" }
        assert(result!!.amount == 12850L) { "金额应为12850分，实际=${result.amount}" }
        // 银行通知无商户时使用包名映射
        assert(result.merchant == "招商银行") { "商户应为招商银行，实际=${result.merchant}" }
    }

    @Test
    fun `extract notification without amount should return null`() {
        val title = "微信"
        val text = "您有一条新消息"
        val result = extractor.extract(title, text, "com.tencent.mm", "test_004")

        assert(result == null) { "无金额时应返回null" }
    }

    @Test
    fun `extract notification with invalid amount should return null`() {
        val title = "微信支付"
        val text = "付款成功 ¥9999999.00" // 超过10000元上限
        val result = extractor.extract(title, text, "com.tencent.mm", "test_005")

        assert(result == null) { "金额超过上限时应返回null" }
    }

    @Test
    fun `extract notification with tiny amount should return null`() {
        val title = "微信支付"
        val text = "付款成功 ¥0.001" // 低于0.01元下限
        val result = extractor.extract(title, text, "com.tencent.mm", "test_006")

        assert(result == null) { "金额低于下限时应返回null" }
    }

    @Test
    fun `extract notification with merchant keyword variant`() {
        val title = "支付宝"
        val text = "支付成功 向肯德基支付26.80元"
        val result = extractor.extract(title, text, "com.eg.android.AlipayGphone", "test_007")

        assert(result != null) { "应成功解析" }
        assert(result!!.merchant == "肯德基") { "商户应为肯德基，实际=${result.merchant}" }
    }

    @Test
    fun `extract notification with yuan unit`() {
        val title = "建设银行"
        val text = "交易提醒 消费100元"
        val result = extractor.extract(title, text, "com.chinamworld.main", "test_008")

        assert(result != null) { "应成功解析" }
        assert(result!!.amount == 10000L) { "金额应为10000分，实际=${result.amount}" }
    }

    @Test
    fun `extract notification with decimal amount`() {
        val title = "微信支付"
        val text = "付款成功 ¥12.5"
        val result = extractor.extract(title, text, "com.tencent.mm", "test_009")

        assert(result != null) { "应成功解析" }
        assert(result!!.amount == 1250L) { "金额应为1250分，实际=${result.amount}" }
    }
}