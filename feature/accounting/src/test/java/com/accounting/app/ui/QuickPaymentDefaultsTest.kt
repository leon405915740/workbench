package com.accounting.app.ui

import com.accounting.app.util.CategoryConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 快捷记账默认分类与弹窗行为相关的最小 JVM 回归测试。
 *
 * ponytail：MainViewModel 依赖 AppRepository（Room/DataStore/Context），
 * 纯 JVM 单测不注入 mock，避免额外引入 mockk/coroutines-test 依赖。
 * 这里用"常量断言 + 源码文本检查"两层覆盖：
 *  - 常量层：DEFAULT_QUICK_PAYMENT_CATEGORY 必须稳定为"餐饮美食"
 *  - 代码静态层：openPaymentQuickEntry 内必须把 category 指向 DEFAULT_QUICK_PAYMENT_CATEGORY，
 *    同时 openManualEntry 必须保持 category=""，防止把"非快捷入口"也默认美食化。
 */
class QuickPaymentDefaultsTest {

    @Test
    fun `DEFAULT_QUICK_PAYMENT_CATEGORY equals 餐饮美食`() {
        assertEquals("餐饮美食", CategoryConstants.DEFAULT_QUICK_PAYMENT_CATEGORY)
    }

    @Test
    fun `openPaymentQuickEntry assigns DEFAULT_QUICK_PAYMENT_CATEGORY as category`() {
        val vmSource = readMainViewModelSource()
        // openPaymentQuickEntry 代码块内必须显式写 category = CategoryConstants.DEFAULT_QUICK_PAYMENT_CATEGORY
        val marker = "category = CategoryConstants.DEFAULT_QUICK_PAYMENT_CATEGORY"
        assertTrue(
            "openPaymentQuickEntry 未把 category 设置为 DEFAULT_QUICK_PAYMENT_CATEGORY，" +
                "请检查 MainViewModel.openPaymentQuickEntry。源码片段未找到：$marker",
            vmSource.contains(marker)
        )
    }

    @Test
    fun `openManualEntry keeps category empty`() {
        val vmSource = readMainViewModelSource()
        // openManualEntry 内部 category 必须保持空字符串
        val block = vmSource.substringAfter("fun openManualEntry").substringBefore("fun submitManualEntry")
        assertTrue(
            "openManualEntry 的 category 已不再是空字符串，请确认未意外修改非目标路径。",
            block.contains("category = \"\"")
        )
    }

    private fun readMainViewModelSource(): String {
        // 工作目录通常是 feature/accounting 或仓库根；用相对路径回退搜索 3 个候选。
        val candidates = listOf(
            "feature/accounting/src/main/java/com/accounting/app/ui/MainViewModel.kt",
            "src/main/java/com/accounting/app/ui/MainViewModel.kt",
            "../feature/accounting/src/main/java/com/accounting/app/ui/MainViewModel.kt"
        )
        for (c in candidates) {
            val f = File(c)
            if (f.isFile) return f.readText(Charsets.UTF_8)
        }
        throw AssertionError(
            "找不到 MainViewModel.kt 源文件（相对路径搜索失败）。" +
                "当前目录：${File(".").absolutePath}"
        )
    }
}
