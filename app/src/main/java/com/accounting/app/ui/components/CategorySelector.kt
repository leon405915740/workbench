package com.accounting.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.accounting.app.ui.theme.WeChatGreen

/** ID 型分类选择器，供分类映射管理页使用。 */
@Composable
fun CategorySelector(
    type: String,
    rootCategories: List<Pair<String, Long>>,
    subcategories: Map<Long, List<Pair<String, Long>>>,
    selectedCategoryId: Long?,
    selectedSubcategoryId: Long?,
    onCategorySelected: (Long) -> Unit,
    onSubcategorySelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(if (type == "income") "收入分类" else "支出分类")
        rootCategories.forEach { (name, id) ->
            Text(
                text = if (id == selectedCategoryId) "✓ $name" else name,
                color = if (id == selectedCategoryId) WeChatGreen else Color.Unspecified,
                modifier = Modifier.fillMaxWidth().clickable { onCategorySelected(id) }.padding(vertical = 8.dp)
            )
        }
        selectedCategoryId?.let { categoryId ->
            subcategories[categoryId].orEmpty().forEach { (name, id) ->
                Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp)) {
                    Text(
                        text = if (id == selectedSubcategoryId) "✓ $name" else name,
                        color = if (id == selectedSubcategoryId) WeChatGreen else Color.Unspecified,
                        modifier = Modifier.clickable { onSubcategorySelected(id) }.padding(vertical = 6.dp)
                    )
                }
            }
        }
    }
}
