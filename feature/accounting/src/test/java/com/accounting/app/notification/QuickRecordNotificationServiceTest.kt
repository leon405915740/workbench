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
}
