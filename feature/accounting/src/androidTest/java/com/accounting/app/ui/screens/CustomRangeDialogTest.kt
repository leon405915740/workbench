package com.accounting.app.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertDoesNotExist
import com.accounting.app.ui.model.UiState
import org.junit.Rule
import org.junit.Test

class CustomRangeDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun confirmButton_dismissesDialog() {
        var dialogVisible = true
        var confirmed = false

        val startDate = 1719792000000L // 2024-07-01 00:00:00
        val endDate = 1722470399999L   // 2024-07-31 23:59:59.999

        composeTestRule.setContent {
            if (dialogVisible) {
                CustomRangeDialog(
                    show = true,
                    currentStart = startDate,
                    currentEnd = endDate,
                    onDismiss = { dialogVisible = false },
                    onConfirm = { _, _ -> confirmed = true }
                )
            }
        }

        // 验证弹窗显示
        composeTestRule.onNodeWithText("选择时间范围").assertIsDisplayed()

        // 点击确认按钮
        composeTestRule.onNodeWithText("确认").performClick()

        // 验证 onConfirm 被调用（功能实现了）
        assert(confirmed) { "onConfirm should have been called" }

        // 验证弹窗已关闭（bug：点击确认后弹窗未消失）
        composeTestRule.onNodeWithText("选择时间范围").assertDoesNotExist()
    }
}
