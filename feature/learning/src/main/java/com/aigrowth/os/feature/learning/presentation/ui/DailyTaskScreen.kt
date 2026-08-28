package com.aigrowth.os.feature.learning.presentation.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aigrowth.os.core.database.entity.DailyTask
import com.aigrowth.os.core.database.entity.TaskStatus
import com.aigrowth.os.core.database.entity.TaskType
import com.aigrowth.os.core.design.GradientButton
import com.aigrowth.os.core.design.GradientSummaryCard
import com.aigrowth.os.core.design.Morandi
import com.aigrowth.os.core.design.MorandiCard
import com.aigrowth.os.core.design.MorandiEmptyState
import com.aigrowth.os.feature.learning.presentation.DailyTaskViewModel

/**
 * 每日任务页面——莫兰迪雾蓝紫设计
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyTaskScreen(
    levelId: String,
    levelTitle: String,
    onNavigateBack: () -> Unit,
    onTaskClick: (String) -> Unit,
    viewModel: DailyTaskViewModel = hiltViewModel()
) {
    val tasks by viewModel.tasks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(levelId) {
        viewModel.loadTodayTasks()
    }

    // 手动添加任务弹窗
    var showAddDialog by remember { mutableStateOf(false) }

    // 计算统计数据
    val completedTasks = tasks.filter { it.status == TaskStatus.COMPLETED }
    val pendingTasks = tasks.filter { it.status == TaskStatus.PENDING }
    val totalMinutes = completedTasks.sumOf { it.estimatedTime }

    Scaffold(
        containerColor = Morandi.BackgroundGray,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "今日任务",
                        fontWeight = FontWeight.Bold,
                        color = Morandi.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = Morandi.BrandPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        when {
            isLoading || isGenerating -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = Morandi.BrandPrimary)
                        Text(
                            text = if (isGenerating) "AI正在生成今日任务..." else "加载中...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Morandi.TextSecondary
                        )
                    }
                }
            }
            tasks.isEmpty() -> {
                // 没有任务，显示生成按钮
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        MorandiEmptyState(
                            icon = Icons.Default.Task,
                            title = "今天还没有学习任务",
                            subtitle = "点击下方按钮让AI为你生成"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        GradientButton(
                            text = "生成今日任务",
                            onClick = {
                                viewModel.generateDailyTasks(levelId, levelTitle)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, Morandi.BorderDefault, RoundedCornerShape(14.dp))
                                .clickable { showAddDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "手动添加任务",
                                style = MaterialTheme.typography.titleMedium,
                                color = Morandi.TextSecondary
                            )
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 统计卡片（成长绿渐变）
                    item {
                        GradientSummaryCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = Morandi.IncomeGradient,
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatItem(
                                    icon = Icons.Default.CheckCircle,
                                    label = "已完成",
                                    value = completedTasks.size.toString()
                                )
                                StatItem(
                                    icon = Icons.Default.Timer,
                                    label = "学习时长",
                                    value = "${totalMinutes}分钟"
                                )
                                StatItem(
                                    icon = Icons.Default.Flag,
                                    label = "待完成",
                                    value = pendingTasks.size.toString()
                                )
                            }
                        }
                    }

                    // 生成 / 手动添加按钮
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GradientButton(
                                text = "生成更多任务",
                                onClick = {
                                    viewModel.generateDailyTasks(levelId, levelTitle)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.dp, Morandi.BorderDefault, RoundedCornerShape(14.dp))
                                    .clickable { showAddDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "手动添加",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Morandi.TextSecondary
                                )
                            }
                        }
                    }

                    // 待完成任务
                    if (pendingTasks.isNotEmpty()) {
                        item {
                            Text(
                                text = "待完成",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Morandi.BrandPrimary
                            )
                        }

                        items(pendingTasks, key = { it.id }) { task ->
                            TaskCard(
                                task = task,
                                onClick = { onTaskClick(task.id) },
                                onComplete = { viewModel.completeTask(task.id) },
                                onSkip = { viewModel.skipTask(task.id) }
                            )
                        }
                    }

                    // 已完成任务
                    if (completedTasks.isNotEmpty()) {
                        item {
                            Text(
                                text = "已完成",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Morandi.NavInactive
                            )
                        }

                        items(completedTasks, key = { it.id }) { task ->
                            TaskCard(
                                task = task,
                                onClick = { onTaskClick(task.id) },
                                onComplete = {},
                                onSkip = {},
                                isCompleted = true
                            )
                        }
                    }
                }
            }
        }

        // 错误提示
        error?.let {
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("关闭", color = Morandi.BrandPrimary)
                    }
                }
            ) {
                Text(it)
            }
        }

        // 手动添加任务弹窗
        if (showAddDialog) {
            ManualAddTaskDialog(
                onConfirm = { title, desc, minutes, type ->
                    viewModel.addManualTask(title, desc, minutes, type)
                    showAddDialog = false
                },
                onDismiss = { showAddDialog = false }
            )
        }
    }
}

/**
 * 手动添加任务弹窗（本地闭环，不依赖AI）
 */
@Composable
private fun ManualAddTaskDialog(
    onConfirm: (String, String, Int, TaskType) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("30") }
    var taskType by remember { mutableStateOf(TaskType.LEARNING) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(title.trim(), description.trim(), minutes.toIntOrNull() ?: 30, taskType)
                },
                enabled = title.isNotBlank()
            ) {
                Text(
                    text = "添加",
                    color = if (title.isNotBlank()) Morandi.BrandPrimary else Morandi.TextSecondary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Morandi.TextSecondary)
            }
        },
        title = {
            Text("手动添加任务", fontWeight = FontWeight.Bold, color = Morandi.TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("任务标题") },
                    placeholder = { Text("例如：阅读30页书") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Morandi.BrandPrimary,
                        unfocusedBorderColor = Morandi.BorderDefault,
                        focusedLabelColor = Morandi.BrandPrimary,
                        cursorColor = Morandi.BrandPrimary
                    )
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("任务描述（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Morandi.BrandPrimary,
                        unfocusedBorderColor = Morandi.BorderDefault,
                        focusedLabelColor = Morandi.BrandPrimary,
                        cursorColor = Morandi.BrandPrimary
                    )
                )
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { input -> minutes = input.filter { c -> c.isDigit() } },
                    label = { Text("预计时长（分钟）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Morandi.BrandPrimary,
                        unfocusedBorderColor = Morandi.BorderDefault,
                        focusedLabelColor = Morandi.BrandPrimary,
                        cursorColor = Morandi.BrandPrimary
                    )
                )
                Text(
                    text = "任务类型",
                    style = MaterialTheme.typography.labelMedium,
                    color = Morandi.TextSecondary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TaskType.values().forEach { type ->
                        FilterChip(
                            selected = type == taskType,
                            onClick = { taskType = type },
                            label = {
                                Text(
                                    text = getTaskTypeLabel(type),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
            }
        },
        containerColor = Morandi.CardWhite
    )
}

/**
 * 统计项（渐变卡上的白字统计）
 */
@Composable
fun StatItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

/**
 * 任务卡片
 */
@Composable
fun TaskCard(
    task: DailyTask,
    onClick: () -> Unit,
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    isCompleted: Boolean = false
) {
    MorandiCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        background = if (isCompleted) Morandi.BubbleAi else Morandi.CardWhite
    ) {
        Column {
            // 任务类型标签
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = getTaskTypeColor(task.taskType)
            ) {
                Text(
                    text = getTaskTypeLabel(task.taskType),
                    style = MaterialTheme.typography.labelSmall,
                    color = Morandi.TextPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Morandi.TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = task.description,
                style = MaterialTheme.typography.bodySmall,
                color = Morandi.TextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Morandi.NavInactive
                )
                Text(
                    text = "${task.estimatedTime}分钟",
                    style = MaterialTheme.typography.labelSmall,
                    color = Morandi.NavInactive
                )
            }

            // 操作按钮（仅待完成任务显示）
            if (!isCompleted) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GradientButton(
                        text = "完成",
                        onClick = onComplete,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, Morandi.BorderDefault, RoundedCornerShape(14.dp))
                            .clickable(onClick = onSkip),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "跳过",
                            style = MaterialTheme.typography.titleMedium,
                            color = Morandi.TextSecondary
                        )
                    }
                }
            }

            // 评分（已完成任务显示）
            if (isCompleted && task.score != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Morandi.BrandPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "评分: ${task.score}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Morandi.BrandPrimary
                    )
                }
            }
        }
    }
}

/**
 * 获取任务类型标签
 */
fun getTaskTypeLabel(type: TaskType): String {
    return when (type) {
        TaskType.LEARNING -> "学习"
        TaskType.PRACTICE -> "练习"
        TaskType.TEST -> "测试"
        TaskType.FEYNMAN -> "费曼"
        TaskType.REVIEW -> "复盘"
    }
}

/**
 * 获取任务类型颜色
 */
@Composable
fun getTaskTypeColor(type: TaskType): Color {
    return when (type) {
        TaskType.LEARNING -> MaterialTheme.colorScheme.primaryContainer
        TaskType.PRACTICE -> MaterialTheme.colorScheme.secondaryContainer
        TaskType.TEST -> MaterialTheme.colorScheme.tertiaryContainer
        TaskType.FEYNMAN -> MaterialTheme.colorScheme.errorContainer
        TaskType.REVIEW -> MaterialTheme.colorScheme.surfaceVariant
    }
}
