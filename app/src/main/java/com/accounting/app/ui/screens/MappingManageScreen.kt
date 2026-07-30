package com.accounting.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.accounting.app.ui.components.CategorySelector
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accounting.app.data.local.entity.CategoryMappingEntity
import com.accounting.app.ui.model.UiState
import com.accounting.app.log.AppLogger
import com.accounting.app.ui.theme.BackgroundGray
import com.accounting.app.ui.theme.CardWhite
import com.accounting.app.ui.theme.NavActive
import com.accounting.app.ui.theme.TextDelete
import com.accounting.app.ui.theme.TextPrimary
import com.accounting.app.ui.theme.TextSecondary
import com.accounting.app.ui.theme.WeChatGreen
import com.accounting.app.ui.theme.DividerColor

@Composable
fun MappingManageScreen(
    uiState: UiState,
    expenseRootCategories: List<Pair<String, Long>>,
    incomeRootCategories: List<Pair<String, Long>>,
    expenseSubcategories: Map<Long, List<Pair<String, Long>>>,
    incomeSubcategories: Map<Long, List<Pair<String, Long>>>,
    onLoadMappings: () -> Unit,
    onAddMapping: (keyword: String, type: String, categoryId: Long, subcategoryId: Long?) -> Unit,
    onUpdateMapping: (id: Long, keyword: String, categoryId: Long, subcategoryId: Long?) -> Unit,
    onDeleteMapping: (Long) -> Unit,
    onToggleMappingEnabled: (Long, Boolean) -> Unit,
    onPromoteMappingToManual: (Long) -> Unit,
    onSwitchTab: (String) -> Unit,
    onShowAddDialog: () -> Unit,
    onDismissAddDialog: () -> Unit,
    onCleanStaleAuto: () -> Unit,
    onBack: () -> Unit
) {
    var deleteTargetId by remember { mutableStateOf<Long?>(null) }
    var editingMapping by remember { mutableStateOf<CategoryMappingEntity?>(null) }

    LaunchedEffect(Unit) {
        onLoadMappings()
    }

    val currentTab = uiState.currentMappingTab
    val allMappings = uiState.mappings
    val manualMappings = allMappings.filter { it.source == "MANUAL" }
    val autoMappings = allMappings.filter { it.source == "AUTO" }

    Column(
        modifier = Modifier.fillMaxSize().background(BackgroundGray)
    ) {
        TopBar(
            onBack = onBack,
            onAdd = {
                editingMapping = null
                onShowAddDialog()
            }
        )

        MappingTabRow(
            currentTab = currentTab,
            onTabSelected = onSwitchTab
        )

        val displayList = if (currentTab == "MANUAL") manualMappings else autoMappings
        val countText = if (currentTab == "MANUAL") {
            "共 ${manualMappings.size} 条手动映射"
        } else {
            "共 ${autoMappings.size} 条自动记忆"
        }

        Text(
            text = countText,
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        if (displayList.isEmpty()) {
            EmptyState(
                currentTab = currentTab,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayList, key = { it.id }) { mapping ->
                    val allRootCategories = expenseRootCategories + incomeRootCategories
                    val allSubcategories = expenseSubcategories + incomeSubcategories
                    val categoryName = allRootCategories.find { it.second == mapping.categoryId }?.first ?: ""
                    val subCategoryName = mapping.subcategoryId?.let { subId ->
                        allSubcategories[mapping.categoryId]?.find { it.second == subId }?.first
                    }
                    MappingCard(
                        mapping = mapping,
                        categoryName = categoryName,
                        subCategoryName = subCategoryName,
                        isManual = currentTab == "MANUAL",
                        onEdit = {
                            editingMapping = mapping
                            onShowAddDialog()
                        },
                        onDelete = { deleteTargetId = mapping.id },
                        onToggle = { enabled -> onToggleMappingEnabled(mapping.id, enabled) },
                        onPromote = { onPromoteMappingToManual(mapping.id) }
                    )
                }
            }
        }

        if (currentTab == "AUTO") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardWhite)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                TextButton(
                    onClick = onCleanStaleAuto,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("立即清理无效自动记忆", color = NavActive, fontSize = 14.sp)
                }
            }
        }
    }

    if (uiState.showAddMappingDialog) {
        AddMappingDialog(
            expenseRootCategories = expenseRootCategories,
            incomeRootCategories = incomeRootCategories,
            expenseSubcategories = expenseSubcategories,
            incomeSubcategories = incomeSubcategories,
            editingMapping = editingMapping,
            onConfirm = { keyword, type, catId, subId ->
                if (editingMapping != null) {
                    onUpdateMapping(editingMapping!!.id, keyword, catId, subId)
                } else {
                    onAddMapping(keyword, type, catId, subId)
                }
                editingMapping = null
                onDismissAddDialog()
            },
            onDismiss = {
                editingMapping = null
                onDismissAddDialog()
            }
        )
    }

    deleteTargetId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteTargetId = null },
            title = { Text("删除映射", fontWeight = FontWeight.SemiBold, color = TextPrimary) },
            text = { Text("确定删除该映射规则吗？删除后将不再自动匹配。") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteMapping(id)
                    deleteTargetId = null
                }) { Text("确认删除", color = TextDelete) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetId = null }) { Text("取消", color = TextSecondary) }
            }
        )
    }
}

@Composable
private fun TopBar(
    onBack: () -> Unit,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardWhite)
            .heightIn(min = 52.dp)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = "返回", tint = TextPrimary)
        }
        Text(
            "分类映射管理",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onAdd) {
            Icon(Icons.Outlined.Add, contentDescription = "新增", tint = WeChatGreen)
        }
    }
}

@Composable
private fun MappingTabRow(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardWhite)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MappingTabButton(
            "手动映射",
            currentTab == "MANUAL",
            { onTabSelected("MANUAL") },
            Modifier.weight(1f)
        )
        MappingTabButton(
            "自动学习",
            currentTab == "AUTO",
            { onTabSelected("AUTO") },
            Modifier.weight(1f)
        )
    }
}

@Composable
private fun MappingTabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) NavActive else BackgroundGray)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 14.sp,
            color = if (isSelected) CardWhite else TextPrimary,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun EmptyState(
    currentTab: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = if (currentTab == "MANUAL") "📋" else "🤖", fontSize = 36.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (currentTab == "MANUAL") "暂无手动映射" else "暂无自动记忆",
                fontSize = 14.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (currentTab == "MANUAL") "点击右上角 + 添加手动映射规则" else "记账后修改分类即可自动学习",
                fontSize = 12.sp,
                color = TextSecondary.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun MappingCard(
    mapping: CategoryMappingEntity,
    categoryName: String,
    subCategoryName: String?,
    isManual: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onPromote: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mapping.keyword,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (mapping.enabled) TextPrimary else TextSecondary.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "→ $categoryName${subCategoryName?.let { "-$it" } ?: ""}",
                        fontSize = 13.sp,
                        color = WeChatGreen
                    )
                }
                if (isManual) {
                    Switch(
                        checked = mapping.enabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = WeChatGreen,
                            checkedTrackColor = WeChatGreen.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = DividerColor
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isManual) {
                    Text(
                        text = "命中 ${mapping.hitCount} 次",
                        fontSize = 12.sp,
                        color = TextSecondary.copy(alpha = 0.6f)
                    )
                } else {
                    Text(
                        text = "命中 ${mapping.hitCount} 次",
                        fontSize = 12.sp,
                        color = TextSecondary.copy(alpha = 0.6f)
                    )
                }
                Row {
                    if (isManual) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = "编辑",
                                tint = NavActive,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        TextButton(onClick = onPromote) {
                            Text("固定", color = NavActive, fontSize = 12.sp)
                        }
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "删除",
                            tint = TextDelete,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddMappingDialog(
    expenseRootCategories: List<Pair<String, Long>>,
    incomeRootCategories: List<Pair<String, Long>>,
    expenseSubcategories: Map<Long, List<Pair<String, Long>>>,
    incomeSubcategories: Map<Long, List<Pair<String, Long>>>,
    editingMapping: CategoryMappingEntity?,
    onConfirm: (keyword: String, type: String, categoryId: Long, subcategoryId: Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var keyword by remember { mutableStateOf(editingMapping?.keyword ?: "") }
    var type by remember { mutableStateOf(editingMapping?.type ?: "expense") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(editingMapping?.categoryId) }
    var selectedSubcategoryId by remember { mutableStateOf<Long?>(editingMapping?.subcategoryId) }
    val currentRootCategories = if (type == "expense") expenseRootCategories else incomeRootCategories

    val currentSubcategoriesByRoot = if (type == "expense") {
        expenseSubcategories
    } else {
        incomeSubcategories
    }

    val currentSubcategories = selectedCategoryId?.let {
        if (type == "expense") expenseSubcategories[it] else incomeSubcategories[it]
    } ?: emptyList()

    LaunchedEffect(Unit) {
        AppLogger.d("", "映射管理",
            if (editingMapping != null) "编辑映射，关键词：${editingMapping.keyword}" else "打开新增映射弹窗")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (editingMapping != null) "编辑映射" else "新增映射",
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text("关键词（如：麦当劳）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = WeChatGreen,
                        unfocusedIndicatorColor = DividerColor
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TypeTabButton("支出", type == "expense", Modifier.weight(1f)) {
                        type = "expense"
                        selectedCategoryId = null
                        selectedSubcategoryId = null
                    }
                    TypeTabButton("收入", type == "income", Modifier.weight(1f)) {
                        type = "income"
                        selectedCategoryId = null
                        selectedSubcategoryId = null
                    }
                }
                CategorySelector(
                    type = type,
                    rootCategories = currentRootCategories,
                    subcategories = currentSubcategoriesByRoot,
                    selectedCategoryId = selectedCategoryId,
                    selectedSubcategoryId = selectedSubcategoryId,
                    onCategorySelected = { id ->
                        selectedCategoryId = id
                        selectedSubcategoryId = null
                    },
                    onSubcategorySelected = { id ->
                        selectedSubcategoryId = id
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedCategoryId?.let { catId ->
                        val subName = selectedSubcategoryId?.let { sid ->
                            currentSubcategories.find { it.second == sid }?.first
                        }
                        AppLogger.d("", "映射管理",
                            "${if (editingMapping != null) "保存" else "添加"}映射，" +
                            "关键词：$keyword，类型：$type，" +
                            "分类：${currentRootCategories.find { it.second == catId }?.first}" +
                            "${subName?.let { "-$it" } ?: ""}")
                        onConfirm(keyword, type, catId, selectedSubcategoryId)
                    }
                },
                enabled = keyword.isNotBlank() && selectedCategoryId != null
            ) {
                Text(
                    if (editingMapping != null) "保存" else "添加",
                    color = WeChatGreen
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun TypeTabButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) WeChatGreen else BackgroundGray)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 14.sp,
            color = if (selected) CardWhite else TextPrimary,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
    }
}
