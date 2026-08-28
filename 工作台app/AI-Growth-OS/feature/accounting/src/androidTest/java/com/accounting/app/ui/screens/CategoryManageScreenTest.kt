package com.accounting.app.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import com.accounting.app.data.local.entity.CategoryEntity
import org.junit.Rule
import org.junit.Test

class CategoryManageScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun addCategoryDialog_dropdown_expandsOnClick() {
        val rootCategories = listOf(
            CategoryEntity(
                id = 1, type = "expense", name = "餐饮", parentId = null,
                sortOrder = 0, isSystem = true, createdAt = 0, updatedAt = 0
            ),
            CategoryEntity(
                id = 2, type = "expense", name = "交通", parentId = null,
                sortOrder = 0, isSystem = true, createdAt = 0, updatedAt = 0
            )
        )

        composeTestRule.setContent {
            CategoryManageScreen(
                rootCategories = rootCategories,
                subcategories = emptyMap(),
                onBack = {},
                onAddCategory = { _, _, _ -> },
                onUpdateCategory = {},
                onDeleteCategory = {}
            )
        }

        // 点击添加按钮打开 Dialog
        composeTestRule.onNodeWithContentDescription("添加").performClick()

        // 点击下拉框区域
        composeTestRule.onNodeWithText("上级分类（选填）").performClick()

        // 验证下拉菜单项显示
        composeTestRule.onNodeWithText("作为一级分类").assertIsDisplayed()
        composeTestRule.onNodeWithText("餐饮").assertIsDisplayed()
    }
}
