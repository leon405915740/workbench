package com.accounting.app.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import com.accounting.app.ui.model.UiState
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsScreen_displaysGroupTitles() {
        composeTestRule.setContent {
            SettingsScreen(
                uiState = UiState(),
                onSaveApiKey = {},
                onManageMappings = {},
                onManageCategories = {},
                onExportCsv = {},
                onExportLog = {},
                onClearLog = {}
            )
        }
        composeTestRule.onNodeWithText("AI 配置").assertIsDisplayed()
        composeTestRule.onNodeWithText("数据管理").assertIsDisplayed()
        composeTestRule.onNodeWithText("日志管理").assertIsDisplayed()
        composeTestRule.onNodeWithText("关于").assertIsDisplayed()
    }
}
