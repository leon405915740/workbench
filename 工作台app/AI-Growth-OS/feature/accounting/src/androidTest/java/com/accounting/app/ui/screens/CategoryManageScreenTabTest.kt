package com.accounting.app.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.onAllNodesWithContentDescription
import com.accounting.app.data.local.entity.CategoryEntity
import org.junit.Rule
import org.junit.Test

class CategoryManageScreenTabTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun tabSwitch_clearsExpandedCategories() {
        val rootCategories = listOf(
            CategoryEntity(
                id = 1, type = "expense", name = "餐饮", parentId = null,
                sortOrder = 0, isSystem = true, createdAt = 0, updatedAt = 0
            ),
            CategoryEntity(
                id = 2, type = "income", name = "工资", parentId = null,
                sortOrder = 0, isSystem = true, createdAt = 0, updatedAt = 0
            )
        )
        val subcategories = mapOf(
            1L to listOf(
                CategoryEntity(
                    id = 3, type = "expense", name = "午餐", parentId = 1,
                    sortOrder = 0, isSystem = false, createdAt = 0, updatedAt = 0
                )
            )
        )

        composeTestRule.setContent {
            CategoryManageScreen(
                rootCategories = rootCategories,
                subcategories = subcategories,
                onBack = {},
                onAddCategory = { _, _, _ -> },
                onUpdateCategory = {},
                onDeleteCategory = {}
            )
        }

        // 展开第一个分类（餐饮）
        composeTestRule.onAllNodesWithContentDescription("展开")[0].performClick()

        // 验证子分类"午餐"显示
        composeTestRule.onNodeWithText("午餐").assertIsDisplayed()

        // 切换到"收入"标签
        composeTestRule.onNodeWithText("收入").performClick()

        // 验证子分类"午餐"不再存在（因为 expandedCategories 被清空）
        composeTestRule.onNodeWithText("午餐").assertDoesNotExist()
    }
}
