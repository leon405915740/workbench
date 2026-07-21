package com.accounting.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accounting.app.data.local.entity.CategoryEntity
import com.accounting.app.ui.components.getCategoryEmoji
import com.accounting.app.ui.components.getSubcategoryEmoji
import com.accounting.app.ui.theme.BackgroundGray
import com.accounting.app.ui.theme.CardWhite
import com.accounting.app.ui.theme.NavActive
import com.accounting.app.ui.theme.TextDelete
import com.accounting.app.ui.theme.TextPrimary
import com.accounting.app.ui.theme.TextSecondary
import com.accounting.app.ui.theme.WeChatGreen
import com.accounting.app.ui.theme.WeChatGreenLight
import com.accounting.app.log.AppLogger

@Composable
fun CategoryManageScreen(
    rootCategories: List<CategoryEntity>,
    subcategories: Map<Long, List<CategoryEntity>>,
    onBack: () -> Unit,
    onAddCategory: (type: String, name: String, parentId: Long?) -> Unit,
    onUpdateCategory: (category: CategoryEntity) -> Unit,
    onDeleteCategory: (id: Long) -> Unit
) {
    var currentTab by remember { mutableStateOf("expense") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var expandedCategories by remember { mutableStateOf(setOf<Long>()) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var deletingCategory by remember { mutableStateOf<CategoryEntity?>(null) }

    val filteredRootCategories = rootCategories.filter { it.type == currentTab }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundGray)) {
        Row(modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).background(CardWhite).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(imageVector = Icons.Outlined.ArrowBack, contentDescription = "返回", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "分类管理", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { showAddDialog = true }, modifier = Modifier.size(40.dp)) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = "添加", tint = WeChatGreen)
            }
        }

        Row(modifier = Modifier.fillMaxWidth().background(CardWhite).padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = "支出", fontSize = 16.sp, color = if (currentTab == "expense") WeChatGreen else TextSecondary, fontWeight = if (currentTab == "expense") FontWeight.Medium else FontWeight.Normal, modifier = Modifier.clickable { currentTab = "expense"; expandedCategories = emptySet() })
            Text(text = "收入", fontSize = 16.sp, color = if (currentTab == "income") WeChatGreen else TextSecondary, fontWeight = if (currentTab == "income") FontWeight.Medium else FontWeight.Normal, modifier = Modifier.clickable { currentTab = "income"; expandedCategories = emptySet() })
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 12.dp)) {
            itemsIndexed(filteredRootCategories) { _, root ->
                CategoryCard(
                    category = root,
                    isExpanded = expandedCategories.contains(root.id),
                    onToggleExpand = {
                        expandedCategories = if (expandedCategories.contains(root.id)) {
                            expandedCategories - root.id
                        } else {
                            expandedCategories + root.id
                        }
                    },
                    onEdit = { editingCategory = root; showEditDialog = true },
                    onDelete = { deletingCategory = root; showDeleteDialog = true }
                )

                AnimatedVisibility(
                    visible = expandedCategories.contains(root.id),
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    val subs = subcategories[root.id] ?: emptyList()
                    Column(modifier = Modifier.padding(start = 32.dp)) {
                        subs.forEach { sub ->
                            SubCategoryCard(
                                category = sub,
                                parentName = root.name,
                                onEdit = { editingCategory = sub; showEditDialog = true },
                                onDelete = { deletingCategory = sub; showDeleteDialog = true }
                            )
                        }
                        if (subs.isEmpty()) {
                            Text(text = "暂无二级分类", fontSize = 14.sp, color = TextSecondary, modifier = Modifier.padding(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showAddDialog) {
        AddCategoryDialog(
            rootCategories = filteredRootCategories,
            type = currentTab,
            onConfirm = { name, parentId ->
                AppLogger.i("", "CategoryManage", "添加分类: name=$name, type=$currentTab, parentId=$parentId")
                onAddCategory(currentTab, name, parentId)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    editingCategory?.let { category ->
        EditCategoryDialog(
            category = category,
            onConfirm = { newName ->
                AppLogger.i("", "CategoryManage", "修改分类: id=${category.id}, 新名称=$newName")
                onUpdateCategory(category.copy(name = newName, updatedAt = System.currentTimeMillis()))
                showEditDialog = false
                editingCategory = null
            },
            onDismiss = {
                showEditDialog = false
                editingCategory = null
            }
        )
    }

    deletingCategory?.let { category ->
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                deletingCategory = null
            },
            title = { Text("删除分类", fontWeight = FontWeight.SemiBold, color = TextPrimary) },
            text = { Text("确定删除「${category.name}」分类吗？相关记账记录可能受影响。") },
            confirmButton = {
                TextButton(onClick = {
                    AppLogger.i("", "CategoryManage", "删除分类: id=${category.id}, name=${category.name}")
                    onDeleteCategory(category.id)
                    showDeleteDialog = false
                    deletingCategory = null
                }) { Text("删除", color = TextDelete) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    deletingCategory = null
                }) { Text("取消", color = TextSecondary) }
            }
        )
    }
}

@Composable
private fun CategoryCard(
    category: CategoryEntity,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = getCategoryEmoji(category.name, category.type), fontSize = 24.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = category.name, fontSize = 16.sp, color = TextPrimary, modifier = Modifier.weight(1f))
            Row(modifier = Modifier.width(100.dp), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Outlined.Edit, contentDescription = "编辑", tint = TextSecondary)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Outlined.Delete, contentDescription = "删除", tint = TextDelete)
                }
                Icon(imageVector = Icons.Outlined.KeyboardArrowDown, contentDescription = if (isExpanded) "收起" else "展开", tint = TextSecondary, modifier = Modifier.size(20.dp).rotate(if (isExpanded) 180f else 0f).clickable(onClick = onToggleExpand))
            }
        }
    }
}

@Composable
private fun SubCategoryCard(
    category: CategoryEntity,
    parentName: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundGray),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = getSubcategoryEmoji(category.name, category.type, parentName), fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = category.name, fontSize = 15.sp, color = TextPrimary, modifier = Modifier.weight(1f))
            Row(modifier = Modifier.width(80.dp), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onEdit, modifier = Modifier.size(20.dp)) {
                    Icon(imageVector = Icons.Outlined.Edit, contentDescription = "编辑", tint = TextSecondary)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(20.dp)) {
                    Icon(imageVector = Icons.Outlined.Delete, contentDescription = "删除", tint = TextDelete)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddCategoryDialog(
    rootCategories: List<CategoryEntity>,
    @Suppress("UNUSED_PARAMETER") type: String,
    onConfirm: (name: String, parentId: Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedParentId by remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加分类", fontWeight = FontWeight.SemiBold, color = TextPrimary) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("分类名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = WeChatGreen,
                        unfocusedIndicatorColor = Color(0xFFE5E5E5)
                    )
                )

                Text(
                    text = "上级分类（选填）",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "作为一级分类",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rootCategories.forEach { cat ->
                                val isSelected = selectedParentId == cat.id
                                Column(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) WeChatGreenLight else BackgroundGray)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.dp,
                                            color = if (isSelected) WeChatGreen else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            selectedParentId = if (isSelected) null else cat.id
                                        }
                                        .padding(top = 10.dp, bottom = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = getCategoryEmoji(cat.name, cat.type),
                                        fontSize = 24.sp
                                    )
                                    Text(
                                        text = cat.name,
                                        fontSize = 12.sp,
                                        color = if (isSelected) WeChatGreen else TextPrimary,
                                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(name, selectedParentId)
                },
                enabled = name.isNotBlank()
            ) { Text("添加", color = WeChatGreen) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
        }
    )
}

@Composable
private fun EditCategoryDialog(
    category: CategoryEntity,
    onConfirm: (newName: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(category.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑分类", fontWeight = FontWeight.SemiBold, color = TextPrimary) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("分类名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = WeChatGreen,
                    unfocusedIndicatorColor = Color(0xFFE5E5E5)
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank() && name != category.name
            ) { Text("保存", color = WeChatGreen) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
        }
    )
}