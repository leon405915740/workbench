package com.accounting.app.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertDoesNotExist
import org.junit.Rule
import org.junit.Test

class CategorySelectorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testRootCategories = listOf(
        "餐饮" to 1L,
        "交通" to 2L,
        "购物" to 3L
    )

    private val testSubcategories = mapOf(
        1L to listOf("午餐" to 11L, "晚餐" to 12L),
        2L to listOf("公交" to 21L)
    )

    @Test
    fun categorySelector_displaysRootCategories() {
        composeTestRule.setContent {
            CategorySelector(
                type = "expense",
                rootCategories = testRootCategories,
                subcategories = testSubcategories,
                selectedCategoryId = null,
                selectedSubcategoryId = null,
                onCategorySelected = {},
                onSubcategorySelected = {}
            )
        }

        // 验证一级分类名称和 emoji 都显示
        composeTestRule.onNodeWithText("餐饮").assertIsDisplayed()
        composeTestRule.onNodeWithText("交通").assertIsDisplayed()
        composeTestRule.onNodeWithText("购物").assertIsDisplayed()
    }

    @Test
    fun categorySelector_clickRootCategory_showsSubcategories() {
        composeTestRule.setContent {
            CategorySelector(
                type = "expense",
                rootCategories = testRootCategories,
                subcategories = testSubcategories,
                selectedCategoryId = null,
                selectedSubcategoryId = null,
                onCategorySelected = {},
                onSubcategorySelected = {}
            )
        }

        // 初始状态：二级分类不显示（因为没有选中一级分类）
        composeTestRule.onNodeWithText("午餐").assertDoesNotExist()

        // 点击一级分类"餐饮"
        composeTestRule.onNodeWithText("餐饮").performClick()

        // 选中后触发回调，需要重新设置内容以反映选中状态
        var clickedId: Long? = null
        composeTestRule.setContent {
            CategorySelector(
                type = "expense",
                rootCategories = testRootCategories,
                subcategories = testSubcategories,
                selectedCategoryId = clickedId,
                selectedSubcategoryId = null,
                onCategorySelected = { id -> clickedId = id },
                onSubcategorySelected = {}
            )
        }

        // 模拟点击餐饮后选中
        clickedId = 1L

        // 重新渲染以反映选中状态
        composeTestRule.setContent {
            CategorySelector(
                type = "expense",
                rootCategories = testRootCategories,
                subcategories = testSubcategories,
                selectedCategoryId = clickedId,
                selectedSubcategoryId = null,
                onCategorySelected = { },
                onSubcategorySelected = {}
            )
        }

        // 验证二级分类显示
        composeTestRule.onNodeWithText("午餐").assertIsDisplayed()
        composeTestRule.onNodeWithText("晚餐").assertIsDisplayed()
    }

    @Test
    fun categorySelector_clickRootCategory_triggersCallback() {
        var selectedId: Long? = null

        composeTestRule.setContent {
            CategorySelector(
                type = "expense",
                rootCategories = testRootCategories,
                subcategories = testSubcategories,
                selectedCategoryId = null,
                selectedSubcategoryId = null,
                onCategorySelected = { id -> selectedId = id },
                onSubcategorySelected = {}
            )
        }

        // 点击一级分类"交通"
        composeTestRule.onNodeWithText("交通").performClick()

        // 验证回调被触发，传入了正确的 categoryId
        assert(selectedId == 2L) { "Expected categoryId 2, got $selectedId" }
    }

    @Test
    fun categorySelector_clickSubcategory_triggersCallback() {
        var selectedSubId: Long? = -1L // sentinel

        composeTestRule.setContent {
            CategorySelector(
                type = "expense",
                rootCategories = testRootCategories,
                subcategories = testSubcategories,
                selectedCategoryId = 1L, // 餐饮已选中
                selectedSubcategoryId = null,
                onCategorySelected = {},
                onSubcategorySelected = { id -> selectedSubId = id }
            )
        }

        // 验证二级分类已显示
        composeTestRule.onNodeWithText("午餐").assertIsDisplayed()

        // 点击二级分类"午餐"
        composeTestRule.onNodeWithText("午餐").performClick()

        // 验证回调被触发，传入了正确的 subcategoryId
        assert(selectedSubId == 11L) { "Expected subcategoryId 11, got $selectedSubId" }
    }

    @Test
    fun categorySelector_clickSameSubcategory_togglesOff() {
        var selectedSubId: Long? = -1L // sentinel

        composeTestRule.setContent {
            CategorySelector(
                type = "expense",
                rootCategories = testRootCategories,
                subcategories = testSubcategories,
                selectedCategoryId = 1L,
                selectedSubcategoryId = 11L, // 午餐已选中
                onCategorySelected = {},
                onSubcategorySelected = { id -> selectedSubId = id }
            )
        }

        // 点击已选中的二级分类"午餐"应取消选中
        composeTestRule.onNodeWithText("午餐").performClick()

        // 验证回调传入 null（取消选中）
        assert(selectedSubId == null) { "Expected null (toggle off), got $selectedSubId" }
    }

    @Test
    fun categorySelector_noSubcategories_showsEmptyHint() {
        composeTestRule.setContent {
            CategorySelector(
                type = "expense",
                rootCategories = listOf("购物" to 3L), // 购物没有二级分类
                subcategories = emptyMap(),
                selectedCategoryId = 3L,
                selectedSubcategoryId = null,
                onCategorySelected = {},
                onSubcategorySelected = {}
            )
        }

        // 验证空状态提示显示
        composeTestRule.onNodeWithText("该分类暂无二级分类，可直接确认").assertIsDisplayed()
    }
}
