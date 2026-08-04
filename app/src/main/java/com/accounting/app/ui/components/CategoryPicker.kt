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
import com.accounting.app.ui.components.getCategoryEmoji

/**
 * 单级分类选择器（弹窗）。
 *
 * v4.0：已删除二级分类，仅展示单级分类网格。
 * 选中态绿色背景白字，未选中浅灰背景深灰字。
 * 入参 type 决定使用 expense 还是 income 的分类列表。
 */
@Composable
fun CategoryPicker(
    type: String,
    initialCategory: String? = null,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // 分类列表
    val categories = CategoryConstants.getCategories(type)
    // 当前选中
    var selectedCategory by rememberSaveable {
        mutableStateOf(initialCategory?.takeIf { it in categories } ?: categories.firstOrNull() ?: "")
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = CardWhite
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 标题栏
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

                // 分类网格（可滚动）
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    CategoryButtonGroup(
                        items = categories,
                        selected = selectedCategory,
                        type = type,
                        onSelect = { selectedCategory = it }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 确认按钮
                Button(
                    onClick = {
                        if (selectedCategory.isNotBlank()) {
                            onConfirm(selectedCategory)
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
 */
@Composable
private fun CategoryButtonGroup(
    items: List<String>,
    selected: String?,
    type: String,
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
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) WeChatGreen else BackgroundGray)
                            .clickable { onSelect(item) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = getCategoryEmoji(item, type),
                                fontSize = 20.sp
                            )
                            Text(
                                text = item,
                                fontSize = 12.sp,
                                color = if (isSelected) CardWhite else TextPrimary,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                    }
                }
                // 不足 3 个时填充空位
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
