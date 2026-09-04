package com.accounting.app.log

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLoggerTest {

    @Test
    fun `sanitizeLog masks renminbi amounts without masking unrelated numbers`() {
        assertEquals(
            "交易提醒：有一笔***的消费，订单20260903",
            AppLogger.sanitizeLog("交易提醒：有一笔7.12人民币的消费，订单20260903")
        )
        assertEquals("金额***", AppLogger.sanitizeLog("金额7.12 人民币"))
    }
}
