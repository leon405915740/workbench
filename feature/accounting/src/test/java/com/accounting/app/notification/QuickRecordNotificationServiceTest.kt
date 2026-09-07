package com.accounting.app.notification

import com.accounting.app.util.AmountUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickRecordNotificationServiceTest {

    @Test
    fun `uses complete BigText when Text is truncated`() {
        val shortText = "您在支付宝-湖南佳宜企业管理有限公司有一笔7.12人民币的消"
        val bigText = "您在支付宝-湖南佳宜企业管理有限公司有一笔7.12人民币的消费"

        val content = mergeNotificationContent(
            title = "交易提醒",
            text = shortText,
            bigText = bigText,
            textLines = arrayOf(bigText)
        )

        assertEquals("交易提醒 $shortText $bigText", content)
        assertTrue(hasExpenseDirection(content))
        assertEquals(712L, AmountUtils.extractFenFromAmountText(content))
    }

    @Test
    fun `rejects refunds income and generic transaction notices`() {
        assertFalse(hasExpenseDirection("交易提醒 原消费7.12元已退款到账"))
        assertFalse(hasExpenseDirection("交易提醒 工资1000元已入账"))
        assertFalse(hasExpenseDirection("交易提醒 您有一笔7.12人民币的交易"))
    }

    @Test
    fun `extracts amount with thousands separator comma`() {
        // 长沙银行通知：1,829.97 不应被截断为 829.97
        val content = "长沙银行 您尾号5100的银联卡活期账户9月7日10:58财付通付款存入1,829.97元，余额1,851.21元"
        assertEquals(182997L, AmountUtils.extractFenFromAmountText(content))
    }

    @Test
    fun `extracts merchant from notification body`() {
        assertEquals(
            "财付通",
            extractMerchant("您尾号5100的银联卡活期账户9月7日10:58财付通付款存入1,829.97元")
        )
        assertEquals(
            "星巴克",
            extractMerchant("您尾号1234的卡于09-07 10:58在星巴克消费100.00元")
        )
        assertEquals(
            "张三",
            extractMerchant("向张三支付50.00元")
        )
        // 提取不到时返回 null，由调用方回退到通知 title
        assertEquals(null, extractMerchant("交易提醒 您有一笔7.12人民币的交易"))
    }
}
