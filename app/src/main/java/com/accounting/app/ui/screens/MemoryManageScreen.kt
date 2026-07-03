package com.accounting.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accounting.app.data.local.entity.CategoryMemoryEntity
import com.accounting.app.ui.model.MemoryGroup
import com.accounting.app.ui.model.SubGroup
import com.accounting.app.ui.model.UiState
import com.accounting.app.ui.theme.BackgroundGray
import com.accounting.app.ui.theme.CardWhite
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
fun MemoryManageScreen(
    uiState: UiState,
    onLoadMemories: (String) -> Unit,
    onAddMemory: (triggerWord: String, type: String, category: String, subcategory: String?) -> Unit,
    onDeleteMemory: (Long) -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit,
    onSearch: (String) -> Unit = {},
    onToggleExpand: (String) -> Unit = {}
) {
    var currentTab by remember { mutableStateOf("expense") }
    var showClearDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var deleteTargetId by remember { mutableStateOf<Long?>(null) }
    var searchText by remember { mutableStateOf("") }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(currentTab) {
        onLoadMemories(currentTab)
    }

    val groups = uiState.memoryGroups
    val expandedCats = uiState.expandedCategories

    Column(
        modifier = Modifier.fillMaxSize().background(BackgroundGray)
    ) {
        // 顶部标题栏
        Row(
            modifier = Modifier.fillMaxWidth().background(CardWhite).heightIn(min = 52.dp).padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "返回", tint = TextPrimary)
            }
            Text("分类记忆管理", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { showClearDialog = true }) {
                Text("恢复默认", fontSize = 14.sp, color = NavActive)
            }
            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Outlined.Add, contentDescription = "新增", tint = WeChatGreen)
            }
        }

        // Tab 栏
        MemoryTabRow(
            currentTab = currentTab,
            onTabSelected = {
                currentTab = it
                searchText = ""
            }
        )

        // 词条总数
        Text(
            text = "共 ${uiState.totalMemoryCount} 条记忆规则",
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        // 搜索框
        OutlinedTextField(
            value = searchText,
            onValueChange = { q ->
                searchText = q
                searchJob?.cancel()
                searchJob = MainScope().launch {
                    delay(200)
                    onSearch(q)
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            placeholder = { Text("搜索关键词/商家", color = TextSecondary, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp)) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = CardWhite,
                unfocusedContainerColor = CardWhite,
                focusedIndicatorColor = WeChatGreen,
                unfocusedIndicatorColor = Color(0xFFE5E5E5)
            )
        )

        // 列表 / 空状态
        if (groups.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (searchText.isNotBlank()) "未找到匹配的记忆规则"
                    else "暂无记忆规则，记账后修改分类即可自动学习",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(groups.size) { gIdx ->
                    val group = groups[gIdx]
                    val isExpanded = group.categoryName in expandedCats
                    val totalItems = group.subGroups.sumOf { it.items.size }

                    // 一级分组标题
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
                            text = "${group.categoryName}($totalItems)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Outlined.KeyboardArrowDown,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp).rotate(if (isExpanded) 0f else -90f)
                        )
                    }

                    // 二级分组 + 词条（可折叠动画）
                    AnimatedVisibility(visible = isExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                        Column {
                            for (subGroup in group.subGroups) {
                                // 二级分组标题
                                if (subGroup.subName != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 6.dp, bottom = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier.width(3.dp).height(16.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(NavActive)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = subGroup.subName,
                                            fontSize = 13.sp,
                                            color = TextSecondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                // 词条行
                                for (item in subGroup.items) {
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
                                        val isSeed = item.source == "seed"
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isSeed) Color(0xFFE8E8E8) else NavActive.copy(alpha = 0.12f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (isSeed) "系统预置" else "自定义",
                                                fontSize = 11.sp,
                                                color = if (isSeed) TextSecondary else NavActive
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(onClick = { deleteTargetId = item.id }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Outlined.Delete, contentDescription = "删除", tint = TextDelete, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
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

    // 新增记忆弹窗
    if (showAddDialog) {
        AddMemoryDialog(
            onConfirm = { word, type, cat, sub ->
                onAddMemory(word, type, cat, sub)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    // 删除确认弹窗
    deleteTargetId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteTargetId = null },
            title = { Text("删除记忆规则", fontWeight = FontWeight.SemiBold, color = TextPrimary) },
            text = { Text("确定删除该记忆规则吗？删除后将不再自动匹配。") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteMemory(id)
                    deleteTargetId = null
                }) { Text("确认删除", color = TextDelete) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetId = null }) { Text("取消", color = TextSecondary) }
            }
        )
    }

    // 恢复默认确认弹窗
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("恢复默认记忆", fontWeight = FontWeight.SemiBold, color = TextPrimary) },
            text = { Text("仅重置系统预置词条，你的自定义记忆不会丢失。是否继续？") },
            confirmButton = {
                TextButton(onClick = {
                    onClearAll()
                    showClearDialog = false
                }) { Text("确认恢复", color = NavActive) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消", color = TextSecondary) }
            }
        )
    }
}

@Composable
private fun MemoryTabRow(currentTab: String, onTabSelected: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(CardWhite).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MemoryTabButton("支出", currentTab == "expense", { onTabSelected("expense") }, Modifier.weight(1f))
        MemoryTabButton("收入", currentTab == "income", { onTabSelected("income") }, Modifier.weight(1f))
    }
}

@Composable
private fun MemoryTabButton(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) NavActive else BackgroundGray)
            .clickable(onClick = onClick).padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 14.sp, color = if (isSelected) CardWhite else TextPrimary,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal)
    }
}

@Composable
private fun AddMemoryDialog(
    onConfirm: (triggerWord: String, type: String, category: String, subcategory: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var triggerWord by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("expense") }
    var category by remember { mutableStateOf("") }
    var subcategory by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增记忆", fontWeight = FontWeight.SemiBold, color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = triggerWord, onValueChange = { triggerWord = it },
                    label = { Text("触发词（如：麦当劳）") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(focusedIndicatorColor = WeChatGreen, unfocusedIndicatorColor = Color(0xFFE5E5E5)))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TypeTabButton("支出", type == "expense", Modifier.weight(1f)) { type = "expense" }
                    TypeTabButton("收入", type == "income", Modifier.weight(1f)) { type = "income" }
                }
                OutlinedTextField(value = category, onValueChange = { category = it },
                    label = { Text("一级分类（如：餐饮）") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(focusedIndicatorColor = WeChatGreen, unfocusedIndicatorColor = Color(0xFFE5E5E5)))
                OutlinedTextField(value = subcategory, onValueChange = { subcategory = it },
                    label = { Text("二级分类（选填）") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(focusedIndicatorColor = WeChatGreen, unfocusedIndicatorColor = Color(0xFFE5E5E5)))
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(triggerWord, type, category, subcategory.takeIf { it.isNotBlank() }) },
                enabled = triggerWord.isNotBlank() && category.isNotBlank()) { Text("添加", color = WeChatGreen) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) } }
    )
}

@Composable
private fun TypeTabButton(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier.heightIn(min = 36.dp).clip(RoundedCornerShape(6.dp))
            .background(if (selected) WeChatGreen else BackgroundGray)
            .clickable(onClick = onClick).padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 14.sp, color = if (selected) CardWhite else TextPrimary,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal)
    }
}
