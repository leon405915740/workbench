package com.accounting.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.accounting.app.ui.theme.BackgroundGray
import com.accounting.app.ui.theme.CardWhite
import com.accounting.app.ui.theme.TextPrimary
import com.accounting.app.ui.theme.TextSecondary
import com.accounting.app.ui.theme.WeChatGreen
import com.accounting.app.util.CategoryConstants

/**
 * 两级分类选择器（弹窗）。
 *
 * - 一级分类按钮组：选中态绿色背景白字，未选中浅灰背景深灰字
 * - 二级分类联动：点击已选中的二级分类可取消选中（返回 null）
 * - 底部绿色全宽确认按钮
 *
 * 入参 type 决定使用 expense 还是 income 的分类列表。
 */
@Composable
fun CategoryPicker(
    type: String,
    initialCategory: String? = null,
    initialSubcategory: String? = null,
    onConfirm: (String, String?) -> Unit,
    onDismiss: () -> Unit
) {
    // 一级分类列表
    val categories = CategoryConstants.getCategories(type)
    // 当前选中的一级分类，初始为 initialCategory（若不在列表则取第一项）
    var selectedCategory by rememberSaveable {
        mutableStateOf(initialCategory?.takeIf { it in categories } ?: categories.firstOrNull() ?: "")
    }
    // 二级分类列表（联动）
    val subcategories = remember(selectedCategory) {
        CategoryConstants.getSubcategories(selectedCategory)
    }
    // 当前选中的二级分类，初始为 initialSubcategory（需属于当前一级分类下）
    var selectedSubcategory by rememberSaveable {
        mutableStateOf(
            if (initialCategory == selectedCategory && initialSubcategory in subcategories) {
                initialSubcategory
            } else null
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = CardWhite
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 标题栏：选择分类 + 关闭按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "选择分类",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "关闭",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 内容区可滚动，限制最大高度避免超出屏幕
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "一级分类",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CategoryButtonGroup(
                        items = categories,
                        selected = selectedCategory,
                        onSelect = {
                            if (it != selectedCategory) {
                                selectedCategory = it
                                // 一级分类变化时清空二级分类
                                selectedSubcategory = null
                            }
                        }
                    )

                    if (subcategories.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "二级分类（可选）",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CategoryButtonGroup(
                            items = subcategories,
                            selected = selectedSubcategory,
                            onSelect = { item ->
                                // 再次点击当前已选项 = 取消选中
                                selectedSubcategory = if (item == selectedSubcategory) null else item
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 底部确认按钮（绿色全宽）
                Button(
                    onClick = {
                        if (selectedCategory.isNotBlank()) {
                            onConfirm(selectedCategory, selectedSubcategory)
                        }
                    },
                    enabled = selectedCategory.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WeChatGreen,
                        disabledContainerColor = WeChatGreen.copy(alpha = 0.4f)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp)
                ) {
                    Text(
                        text = "确认",
                        color = CardWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * 分类按钮组：每行 3 个按钮，自动换行。
 * 选中态绿色背景白字，未选中浅灰背景深灰字。
 */
@Composable
private fun CategoryButtonGroup(
    items: List<String>,
    selected: String?,
    onSelect: (String) -> Unit
) {
    val rows = items.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { item ->
                    val isSelected = item == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) WeChatGreen else BackgroundGray)
                            .clickable { onSelect(item) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item,
                            fontSize = 13.sp,
                            color = if (isSelected) CardWhite else TextPrimary,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
                // 不足 3 个时填充空位以保持每行宽度一致
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
