package com.accounting.app.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertDoesNotExist
import com.accounting.app.ui.model.UiState
import org.junit.Rule
import org.junit.Test

class MappingManageScreenFilterTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun addMappingDialog_expenseType_showsOnlyExpenseCategories() {
        val expenseRoots = listOf("餐饮" to 1L, "交通" to 2L)
        val incomeRoots = listOf("工资" to 3L, "奖金" to 4L)
        val uiState = UiState(
            showAddMappingDialog = true,
            mappings = emptyList(),
            isLoading = false,
            error = null,
            toast = null,
            expenseRootCategories = expenseRoots,
            incomeRootCategories = incomeRoots,
            expenseSubcategories = emptyMap(),
            incomeSubcategories = emptyMap()
        )

        composeTestRule.setContent {
            MappingManageScreen(
                uiState = uiState,
                expenseRootCategories = expenseRoots,
                incomeRootCategories = incomeRoots,
                expenseSubcategories = emptyMap(),
                incomeSubcategories = emptyMap(),
                onLoadMappings = {},
                onAddMapping = { _, _, _, _ -> },
                onUpdateMapping = { _, _, _, _ -> },
                onDeleteMapping = {},
                onToggleMappingEnabled = { _, _ -> },
                onPromoteMappingToManual = {},
                onSwitchTab = {},
                onShowAddDialog = {},
                onDismissAddDialog = {},
                onCleanStaleAuto = {},
                onBack = {}
            )
        }

        // 点击一级分类下拉框
        composeTestRule.onNodeWithText("选择一级分类").performClick()

        // 验证支出分类显示
        composeTestRule.onNodeWithText("餐饮").assertIsDisplayed()
        composeTestRule.onNodeWithText("交通").assertIsDisplayed()

        // 验证收入分类不存在
        composeTestRule.onNodeWithText("工资").assertDoesNotExist()
        composeTestRule.onNodeWithText("奖金").assertDoesNotExist()
    }

    @Test
    fun addMappingDialog_incomeType_showsOnlyIncomeCategories() {
        val expenseRoots = listOf("餐饮" to 1L, "交通" to 2L)
        val incomeRoots = listOf("工资" to 3L, "奖金" to 4L)
        val uiState = UiState(
            showAddMappingDialog = true,
            mappings = emptyList(),
            isLoading = false,
            error = null,
            toast = null,
            expenseRootCategories = expenseRoots,
            incomeRootCategories = incomeRoots,
            expenseSubcategories = emptyMap(),
            incomeSubcategories = emptyMap()
        )

        composeTestRule.setContent {
            MappingManageScreen(
                uiState = uiState,
                expenseRootCategories = expenseRoots,
                incomeRootCategories = incomeRoots,
                expenseSubcategories = emptyMap(),
                incomeSubcategories = emptyMap(),
                onLoadMappings = {},
                onAddMapping = { _, _, _, _ -> },
                onUpdateMapping = { _, _, _, _ -> },
                onDeleteMapping = {},
                onToggleMappingEnabled = { _, _ -> },
                onPromoteMappingToManual = {},
                onSwitchTab = {},
                onShowAddDialog = {},
                onDismissAddDialog = {},
                onCleanStaleAuto = {},
                onBack = {}
            )
        }

        // 切换到收入类型
        composeTestRule.onNodeWithText("收入").performClick()

        // 点击一级分类下拉框
        composeTestRule.onNodeWithText("选择一级分类").performClick()

        // 验证收入分类显示
        composeTestRule.onNodeWithText("工资").assertIsDisplayed()
        composeTestRule.onNodeWithText("奖金").assertIsDisplayed()

        // 验证支出分类不存在
        composeTestRule.onNodeWithText("餐饮").assertDoesNotExist()
        composeTestRule.onNodeWithText("交通").assertDoesNotExist()
    }
}
