package com.accounting.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accounting.app.log.AppLogger
import com.accounting.app.ui.components.CategoryPicker
import com.accounting.app.ui.components.CategorySelector
import com.accounting.app.ui.components.getCategoryEmoji
import com.accounting.app.ui.model.MappingItemUi
import com.accounting.app.ui.model.MemoryItemUi
import com.accounting.app.ui.theme.BackgroundGray
import com.accounting.app.ui.theme.BorderDefault
import com.accounting.app.ui.theme.CardWhite
import com.accounting.app.ui.theme.DividerColor
import com.accounting.app.ui.theme.NavActive
import com.accounting.app.ui.theme.TextDelete
import com.accounting.app.ui.theme.TextPrimary
import com.accounting.app.ui.theme.TextSecondary
import com.accounting.app.ui.theme.WeChatGreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MemoryMappingManageScreen(
    memories: List<MemoryItemUi>,
    mappings: List<MappingItemUi>,
    memoryTotalCount: Int,
    memorySourceFilter: String,
    expandedCategories: Set<String>,
    expenseCategories: List<Pair<String, Long>>,
    incomeCategories: List<Pair<String, Long>>,
    onAddMemory: (triggerWord: String, type: String, category: String) -> Unit,
    onDeleteMemory: (Long) -> Unit,
    onClearAllMemories: () -> Unit,
    onRestoreDefaultMemories: () -> Unit,
    onSearchMemories: (String) -> Unit,
    onToggleExpand: (String) -> Unit,
    onSourceFilter: (String) -> Unit,
    onAddMapping: (keyword: String, type: String, categoryId: Long, subcategoryId: Long?) -> Unit,
    onDeleteMapping: (Long) -> Unit,
    onToggleMappingEnabled: (Long, Boolean) -> Unit,
    onPromoteMappingToManual: (Long) -> Unit,
    onCleanStaleAutoMappings: () -> Unit,
    onBack: () -> Unit
) {
    var currentTopTab by remember { mutableStateOf(0) }
    var currentMemoryTab by remember { mutableStateOf("expense") }
    var currentMappingTab by remember { mutableStateOf("MANUAL") }
    var showAddMemoryDialog by remember { mutableStateOf(false) }
    var showClearMemoryDialog by remember { mutableStateOf(false) }
    var deleteMemoryTargetId by remember { mutableStateOf<Long?>(null) }
    var searchText by remember { mutableStateOf("") }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var showAddMappingDialog by remember { mutableStateOf(false) }
    var deleteMappingTargetId by remember { mutableStateOf<Long?>(null) }
    var editingMapping by remember { mutableStateOf<MappingItemUi?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().background(BackgroundGray)
    ) {
        // 顶部标题栏
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
                "记忆与分类管理",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            if (currentTopTab == 0) {
                TextButton(onClick = { showClearMemoryDialog = true }) {
                    Text("恢复默认", fontSize = 14.sp, color = NavActive)
                }
                IconButton(onClick = { showAddMemoryDialog = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = "新增", tint = WeChatGreen)
                }
            } else {
                IconButton(onClick = {
                    editingMapping = null
                    showAddMappingDialog = true
                }) {
                    Icon(Icons.Outlined.Add, contentDescription = "新增", tint = WeChatGreen)
                }
            }
        }

        // 顶部 TabRow
        TabRow(
            selectedTabIndex = currentTopTab,
            containerColor = CardWhite,
            contentColor = WeChatGreen
        ) {
            Tab(
                selected = currentTopTab == 0,
                onClick = { currentTopTab = 0 },
                text = { Text("分类记忆") }
            )
            Tab(
                selected = currentTopTab == 1,
                onClick = { currentTopTab = 1 },
                text = { Text("分类映射") }
            )
        }

        when (currentTopTab) {
            0 -> {
                // ===== 分类记忆 =====
                val filteredMemories = memories.filter { it.type == currentMemoryTab }
                val groups = filteredMemories
                    .groupBy { it.category }
                    .map { (name, items) -> MemoryGroupUi(name, items) }

                MemoryTabRow(
                    currentTab = currentMemoryTab,
                    onTabSelected = {
                        currentMemoryTab = it
                        searchText = ""
                    }
                )

                SourceFilterRow(
                    currentFilter = memorySourceFilter,
                    onFilterSelected = { onSourceFilter(it) }
                )

                Text(
                    text = "共 $memoryTotalCount 条记忆规则",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )

                OutlinedTextField(
                    value = searchText,
                    onValueChange = { q ->
                        searchText = q
                        searchJob?.cancel()
                        searchJob = MainScope().launch {
                            delay(200)
                            onSearchMemories(q)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    placeholder = {
                        Text("搜索关键词/商家", color = TextSecondary, fontSize = 14.sp)
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CardWhite,
                        unfocusedContainerColor = CardWhite,
                        focusedIndicatorColor = WeChatGreen,
                        unfocusedIndicatorColor = BorderDefault
                    )
                )

                // 列表 / 空状态
                if (groups.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        val emptyText = when {
                            searchText.isNotBlank() -> "未找到匹配的记忆规则"
                            memorySourceFilter == "auto" -> "暂无自动学习记忆\n记账后修改分类或确认关键词即可自动学习"
                            memorySourceFilter == "seed" -> "暂无系统预置记忆\n点击右上角「恢复默认」可重置"
                            memorySourceFilter == "user" -> "暂无手动添加的记忆\n点击右上角 + 号可新增"
                            else -> "暂无记忆规则，记账后修改分类即可自动学习"
                        }
                        Text(
                            text = emptyText,
                            fontSize = 14.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        items(groups.size) { gIdx ->
                            val group = groups[gIdx]
                            val isExpanded = group.categoryName in expandedCategories
                            val totalItems = group.items.size

                            // 分组标题
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BackgroundGray)
                                    .clickable { onToggleExpand(group.categoryName) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${getCategoryEmoji(group.categoryName, currentMemoryTab)} ${group.categoryName}($totalItems)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    Icons.Outlined.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .rotate(if (isExpanded) 0f else -90f)
                                )
                            }

                            // 词条列表（可折叠动画）
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically(),
                                exit = shrinkVertically()
                            ) {
                                Column {
                                    for (item in group.items) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(CardWhite)
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.triggerWord,
                                                    fontSize = 15.sp,
                                                    color = TextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            // 来源标签
                                            val sourceLabel = when (item.source) {
                                                "seed" -> "系统预置"
                                                "auto" -> "自动学习"
                                                else -> "手动添加"
                                            }
                                            val sourceColor = when (item.source) {
                                                "seed" -> TextSecondary
                                                "auto" -> WeChatGreen
                                                else -> NavActive
                                            }
                                            val sourceBg = when (item.source) {
                                                "seed" -> DividerColor
                                                "auto" -> WeChatGreen.copy(alpha = 0.12f)
                                                else -> NavActive.copy(alpha = 0.12f)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(sourceBg)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = sourceLabel,
                                                    fontSize = 11.sp,
                                                    color = sourceColor
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = { deleteMemoryTargetId = item.id },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Outlined.Delete,
                                                    contentDescription = "删除",
                                                    tint = TextDelete,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                            }

                            if (gIdx < groups.size - 1) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
            1 -> {
                // ===== 分类映射 =====
                val manualMappings = mappings.filter { it.isManual }
                val autoMappings = mappings.filter { !it.isManual }
                val displayList = if (currentMappingTab == "MANUAL") manualMappings else autoMappings

                MappingTabRow(
                    currentTab = currentMappingTab,
                    onTabSelected = { currentMappingTab = it }
                )

                val countText = if (currentMappingTab == "MANUAL") {
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
                        currentTab = currentMappingTab,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayList, key = { it.id }) { mapping ->
                            MappingCard(
                                mapping = mapping,
                                isManual = currentMappingTab == "MANUAL",
                                onEdit = {
                                    editingMapping = mapping
                                    showAddMappingDialog = true
                                },
                                onDelete = { deleteMappingTargetId = mapping.id },
                                onToggle = { enabled ->
                                    onToggleMappingEnabled(mapping.id, enabled)
                                },
                                onPromote = { onPromoteMappingToManual(mapping.id) }
                            )
                        }
                    }
                }

                if (currentMappingTab == "AUTO") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardWhite)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        TextButton(
                            onClick = onCleanStaleAutoMappings,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("立即清理无效自动记忆", color = NavActive, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }

    // 新增记忆弹窗
    if (showAddMemoryDialog) {
        AddMemoryDialog(
            onConfirm = { word, type, cat ->
                onAddMemory(word, type, cat)
                showAddMemoryDialog = false
            },
            onDismiss = { showAddMemoryDialog = false }
        )
    }

    // 删除记忆确认弹窗
    deleteMemoryTargetId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteMemoryTargetId = null },
            title = {
                Text("删除记忆规则", fontWeight = FontWeight.SemiBold, color = TextPrimary)
            },
            text = { Text("确定删除该记忆规则吗？删除后将不再自动匹配。") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteMemory(id)
                    deleteMemoryTargetId = null
                }) { Text("确认删除", color = TextDelete) }
            },
            dismissButton = {
                TextButton(onClick = { deleteMemoryTargetId = null }) {
                    Text("取消", color = TextSecondary)
                }
            }
        )
    }

    // 恢复默认记忆确认弹窗
    if (showClearMemoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearMemoryDialog = false },
            title = {
                Text("恢复默认记忆", fontWeight = FontWeight.SemiBold, color = TextPrimary)
            },
            text = { Text("仅重置系统预置词条，你的自定义记忆不会丢失。是否继续？") },
            confirmButton = {
                TextButton(onClick = {
                    onRestoreDefaultMemories()
                    showClearMemoryDialog = false
                }) { Text("确认恢复", color = NavActive) }
            },
            dismissButton = {
                TextButton(onClick = { showClearMemoryDialog = false }) {
                    Text("取消", color = TextSecondary)
                }
            }
        )
    }

    // 新增/编辑映射弹窗
    if (showAddMappingDialog) {
        AddMappingDialog(
            expenseRootCategories = expenseCategories,
            incomeRootCategories = incomeCategories,
            expenseSubcategories = emptyMap(),
            incomeSubcategories = emptyMap(),
            editingMapping = editingMapping,
            onConfirm = { keyword, type, catId, subId ->
                onAddMapping(keyword, type, catId, subId)
                editingMapping = null
                showAddMappingDialog = false
            },
            onDismiss = {
                editingMapping = null
                showAddMappingDialog = false
            }
        )
    }

    // 删除映射确认弹窗
    deleteMappingTargetId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteMappingTargetId = null },
            title = { Text("删除映射", fontWeight = FontWeight.SemiBold, color = TextPrimary) },
            text = { Text("确定删除该映射规则吗？删除后将不再自动匹配。") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteMapping(id)
                    deleteMappingTargetId = null
                }) { Text("确认删除", color = TextDelete) }
            },
            dismissButton = {
                TextButton(onClick = { deleteMappingTargetId = null }) {
                    Text("取消", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SourceFilterRow(currentFilter: String, onFilterSelected: (String) -> Unit) {
    val filters = listOf(
        "" to "全部",
        "auto" to "自动学习",
        "seed" to "系统预置",
        "user" to "手动添加"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardWhite)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { (value, label) ->
            val isSelected = currentFilter == value
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) WeChatGreen else BackgroundGray)
                    .clickable { onFilterSelected(value) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = if (isSelected) CardWhite else TextSecondary,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun MemoryTabRow(currentTab: String, onTabSelected: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardWhite)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MemoryTabButton("支出", currentTab == "expense", { onTabSelected("expense") }, Modifier.weight(1f))
        MemoryTabButton("收入", currentTab == "income", { onTabSelected("income") }, Modifier.weight(1f))
    }
}

@Composable
private fun MemoryTabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = 40.dp)
            .shadow(elevation = if (isSelected) 2.dp else 0.dp, shape = RoundedCornerShape(20.dp), clip = false)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) CardWhite else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 14.sp,
            color = if (isSelected) WeChatGreen else TextSecondary,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun AddMemoryDialog(
    onConfirm: (triggerWord: String, type: String, category: String) -> Unit,
    onDismiss: () -> Unit
) {
    var triggerWord by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("expense") }
    var category by remember { mutableStateOf("") }
    var showCategoryPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增记忆", fontWeight = FontWeight.SemiBold, color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = triggerWord,
                    onValueChange = { triggerWord = it },
                    label = { Text("触发词（如：麦当劳）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = WeChatGreen,
                        unfocusedIndicatorColor = BorderDefault
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TypeTabButton("支出", type == "expense", Modifier.weight(1f)) {
                        type = "expense"; category = ""
                    }
                    TypeTabButton("收入", type == "income", Modifier.weight(1f)) {
                        type = "income"; category = ""
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(BackgroundGray)
                        .clickable { showCategoryPicker = true }
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        if (category.isNotBlank()) category else "点击选择分类",
                        fontSize = 16.sp,
                        color = if (category.isNotBlank()) TextPrimary else TextSecondary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(triggerWord, type, category) },
                enabled = triggerWord.isNotBlank() && category.isNotBlank()
            ) { Text("添加", color = WeChatGreen) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
        }
    )

    if (showCategoryPicker) {
        CategoryPicker(
            type = type,
            initialCategory = category.ifBlank { null },
            onConfirm = {
                category = it
                showCategoryPicker = false
            },
            onDismiss = { showCategoryPicker = false }
        )
    }
}

@Composable
private fun MappingTabRow(currentTab: String, onTabSelected: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardWhite)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MappingTabButton("手动映射", currentTab == "MANUAL", { onTabSelected("MANUAL") }, Modifier.weight(1f))
        MappingTabButton("自动学习", currentTab == "AUTO", { onTabSelected("AUTO") }, Modifier.weight(1f))
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
            .shadow(elevation = if (isSelected) 2.dp else 0.dp, shape = RoundedCornerShape(20.dp), clip = false)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) CardWhite else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 14.sp,
            color = if (isSelected) WeChatGreen else TextSecondary,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun EmptyState(currentTab: String, modifier: Modifier = Modifier) {
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
    mapping: MappingItemUi,
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
                        color = if (mapping.isEnabled) TextPrimary else TextSecondary.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val emoji = getCategoryEmoji(mapping.categoryName, mapping.type)
                    Text(
                        text = "→ $emoji ${mapping.categoryName}${mapping.subcategoryName?.let { "-$it" } ?: ""}",
                        fontSize = 13.sp,
                        color = WeChatGreen
                    )
                }
                if (isManual) {
                    Switch(
                        checked = mapping.isEnabled,
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
                Text(
                    text = "命中 ${mapping.hitCount} 次",
                    fontSize = 12.sp,
                    color = TextSecondary.copy(alpha = 0.6f)
                )
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
    editingMapping: MappingItemUi?,
    onConfirm: (keyword: String, type: String, categoryId: Long, subcategoryId: Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var keyword by remember { mutableStateOf(editingMapping?.keyword ?: "") }
    var type by remember { mutableStateOf(editingMapping?.type ?: "expense") }
    var selectedCategoryId by remember {
        mutableStateOf(editingMapping?.let { em ->
            (if (em.type == "expense") expenseRootCategories else incomeRootCategories)
                .find { it.first == em.categoryName }?.second
        })
    }
    var selectedSubcategoryId by remember { mutableStateOf<Long?>(null) }
    val currentRootCategories = if (type == "expense") expenseRootCategories else incomeRootCategories
    val currentSubcategoriesByRoot = if (type == "expense") expenseSubcategories else incomeSubcategories
    val currentSubcategories = selectedCategoryId?.let {
        if (type == "expense") expenseSubcategories[it] else incomeSubcategories[it]
    } ?: emptyList()

    LaunchedEffect(Unit) {
        AppLogger.d(
            "", "映射管理",
            if (editingMapping != null) "编辑映射，关键词：${editingMapping.keyword}" else "打开新增映射弹窗"
        )
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
                        unfocusedIndicatorColor = BorderDefault
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
                        AppLogger.d(
                            "", "映射管理",
                            "${if (editingMapping != null) "保存" else "添加"}映射，" +
                                "关键词：$keyword，类型：$type，" +
                                "分类：${currentRootCategories.find { it.second == catId }?.first}" +
                                "${subName?.let { "-$it" } ?: ""}"
                        )
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
            .shadow(elevation = if (selected) 2.dp else 0.dp, shape = RoundedCornerShape(16.dp), clip = false)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) CardWhite else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 14.sp,
            color = if (selected) WeChatGreen else TextSecondary,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

private data class MemoryGroupUi(val categoryName: String, val items: List<MemoryItemUi>)
