package com.accounting.app.capture

import org.junit.Test

class WindowChangeDeduplicatorTest {

    @Test
    fun `shouldProcess same windowId and packageName within 500ms returns false`() {
        val deduplicator = WindowChangeDeduplicator()
        // 第一次应该放行
        assert(deduplicator.shouldProcess(windowId = 123, packageName = "com.tencent.mm"))
        // 500ms 内重复应拦截
        Thread.sleep(100)
        val result = deduplicator.shouldProcess(windowId = 123, packageName = "com.tencent.mm")
        assert(!result) { "500ms 内重复窗口应返回 false" }
    }

    @Test
    fun `shouldProcess same windowId and packageName after 500ms returns true`() {
        val deduplicator = WindowChangeDeduplicator()
        deduplicator.shouldProcess(windowId = 123, packageName = "com.tencent.mm")
        // 等待超过 500ms
        Thread.sleep(600)
        val result = deduplicator.shouldProcess(windowId = 123, packageName = "com.tencent.mm")
        assert(result) { "500ms 后应放行" }
    }

    @Test
    fun `shouldProcess different windowId returns true`() {
        val deduplicator = WindowChangeDeduplicator()
        deduplicator.shouldProcess(windowId = 123, packageName = "com.tencent.mm")
        val result = deduplicator.shouldProcess(windowId = 456, packageName = "com.tencent.mm")
        assert(result) { "不同 windowId 应放行" }
    }

    @Test
    fun `shouldProcess different packageName returns true`() {
        val deduplicator = WindowChangeDeduplicator()
        deduplicator.shouldProcess(windowId = 123, packageName = "com.tencent.mm")
        val result = deduplicator.shouldProcess(windowId = 123, packageName = "com.eg.android.AlipayGphone")
        assert(result) { "不同 packageName 应放行" }
    }
}
