package com.aigrowth.os.feature.learning.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aigrowth.os.core.database.entity.AIMemory
import com.aigrowth.os.core.database.entity.MemoryType
import com.aigrowth.os.feature.learning.presentation.MemoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: MemoryViewModel = hiltViewModel()
) {
    val memories by viewModel.memories.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val isExtracting by viewModel.isExtracting.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AI记忆中心",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加记忆")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 统计卡片
            MemoryStatsCard(memories = memories, viewModel = viewModel)

            // 类型筛选器
            TypeFilterChips(
                selectedType = selectedType,
                onTypeSelected = { viewModel.filterByType(it) },
                viewModel = viewModel
            )

            if (memories.isEmpty()) {
                EmptyMemoryState(isExtracting = isExtracting)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(memories, key = { it.id }) { memory ->
                        MemoryCard(
                            memory = memory,
                            viewModel = viewModel,
                            onDelete = { viewModel.deleteMemory(memory) }
                        )
                    }
                }
            }
        }

        // 添加记忆对话框
        if (showAddDialog) {
            AddMemoryDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.dismissAddDialog() }
            )
        }

        // 错误提示
        error?.let { errorMsg ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("关闭")
                    }
                }
            ) {
                Text(errorMsg)
            }
        }
    }
}

@Composable
private fun MemoryStatsCard(
    memories: List<AIMemory>,
    viewModel: MemoryViewModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "记忆统计",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MemoryType.values().forEach { type ->
                    val count = memories.count { it.memoryType == type }
                    val label = viewModel.getMemoryTypeLabel(type)
                    val color = viewModel.getMemoryTypeColor(type)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeFilterChips(
    selectedType: MemoryType?,
    onTypeSelected: (MemoryType?) -> Unit,
    viewModel: MemoryViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedType == null,
            onClick = { onTypeSelected(null) },
            label = { Text("全部") }
        )
        MemoryType.values().forEach { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeSelected(type) },
                label = { Text(viewModel.getMemoryTypeLabel(type)) }
            )
        }
    }
}

@Composable
private fun MemoryCard(
    memory: AIMemory,
    viewModel: MemoryViewModel,
    onDelete: () -> Unit
) {
    val color = viewModel.getMemoryTypeColor(memory.memoryType)
    val label = viewModel.getMemoryTypeLabel(memory.memoryType)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 类型指示器
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = color.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = color,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 重要性指示器
                    Row {
                        repeat(5) { index ->
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (index < memory.importance)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = memory.content,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun EmptyMemoryState(isExtracting: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            if (isExtracting) {
                CircularProgressIndicator()
                Text(
                    text = "AI正在分析对话提取记忆...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Icon(
                    Icons.Default.Psychology,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
                Text(
                    text = "还没有AI记忆",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "与AI导师互动后，AI会自动提取你的学习特点\n你也可以手动添加记忆",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun AddMemoryDialog(
    viewModel: MemoryViewModel,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf(MemoryType.PREFERENCE) }
    var content by remember { mutableStateOf("") }
    var importance by remember { mutableStateOf(3) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加记忆") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // 类型选择
                Text(
                    text = "记忆类型",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MemoryType.values().forEach { type ->
                        val label = viewModel.getMemoryTypeLabel(type)
                        val selected = selectedType == type
                        FilterChip(
                            selected = selected,
                            onClick = { selectedType = type },
                            label = { Text(label) }
                        )
                    }
                }

                // 内容输入
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("记忆内容") },
                    placeholder = { Text("例如：晚上学习效率最高") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                // 重要性选择
                Text(
                    text = "重要性: $importance",
                    style = MaterialTheme.typography.labelMedium
                )
                Slider(
                    value = importance.toFloat(),
                    onValueChange = { importance = it.toInt() },
                    valueRange = 1f..5f,
                    steps = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (content.isNotBlank()) {
                        viewModel.addMemory(selectedType, content.trim(), importance)
                    }
                },
                enabled = content.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
