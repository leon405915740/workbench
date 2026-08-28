package com.accounting.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryEmojiUtilsTest {

    @Test
    fun getCategoryEmoji_knownExpenseCategory_returnsCorrectEmoji() {
        assertEquals("🍜", getCategoryEmoji("餐饮", "expense"))
    }

    @Test
    fun getCategoryEmoji_knownIncomeCategory_returnsCorrectEmoji() {
        assertEquals("💰", getCategoryEmoji("工资", "income"))
    }

    @Test
    fun getCategoryEmoji_unknownCategory_returnsDefaultBox() {
        assertEquals("📦", getCategoryEmoji("未知分类", "expense"))
    }

    @Test
    fun getSubcategoryEmoji_knownSubcategory_returnsOwnEmoji() {
        assertEquals("🍜", getSubcategoryEmoji("餐饮", "expense", null))
    }

    @Test
    fun getSubcategoryEmoji_unknownSubcategoryWithParent_returnsParentEmoji() {
        assertEquals("🍜", getSubcategoryEmoji("外卖", "expense", "餐饮"))
    }

    @Test
    fun getSubcategoryEmoji_unknownSubcategoryWithoutParent_returnsDefaultBox() {
        assertEquals("📦", getSubcategoryEmoji("未知子分类", "expense", null))
    }

    @Test
    fun getSubcategoryEmoji_unknownSubcategoryWithUnknownParent_returnsDefaultBox() {
        assertEquals("📦", getSubcategoryEmoji("未知子分类", "expense", "未知父分类"))
    }
}
