package com.aigrowth.os.feature.learning.presentation.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aigrowth.os.core.aiengine.GrowthReviewResponse
import com.aigrowth.os.core.database.entity.GrowthRecord
import com.aigrowth.os.core.design.Morandi
import com.aigrowth.os.core.design.MorandiCard
import com.aigrowth.os.feature.learning.presentation.GrowthViewModel
import com.aigrowth.os.feature.learning.presentation.TaskCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrowthScreen(
    onEvaluationClick: (String) -> Unit,
    onKnowledgeCardClick: (String) -> Unit,
    onOpenDrawer: () -> Unit = {},
    initialCategory: com.aigrowth.os.feature.learning.presentation.TaskCategory = TaskCategory.ALL,
    viewModel: GrowthViewModel = hiltViewModel()
) {
    val filteredRecords by viewModel.filteredRecords.collectAsState()
    val selectedRecord by viewModel.selectedRecord.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedRecordId by viewModel.selectedRecordId.collectAsState()
    val reviewResponse by viewModel.reviewResponse.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val error by viewModel.error.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        if (initialCategory != TaskCategory.ALL) {
            viewModel.filterByCategory(initialCategory)
        }
    }

    LaunchedEffect(filteredRecords) {
        if (filteredRecords.isNotEmpty()) {
            val currentId = selectedRecordId
            val isInList = currentId != null && filteredRecords.any { it.id == currentId }
            if (!isInList) {
                viewModel.selectRecord(filteredRecords.first().id)
            }
        }
    }

    LaunchedEffect(error) {
        error?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "成长数据中心",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "菜单")
                    }
                    IconButton(onClick = { viewModel.recordTodayGrowth() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新今日数据")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TaskListPanel(
                modifier = Modifier.width(140.dp),
                records = filteredRecords,
                selectedRecordId = selectedRecordId,
                selectedCategory = selectedCategory,
                onCategorySelected = { viewModel.filterByCategory(it) },
                onRecordClick = { viewModel.selectRecord(it) }
            )
            RecordDetailPanel(
                modifier = Modifier.weight(1f),
                record = selectedRecord,
                records = filteredRecords,
                reviewResponse = reviewResponse,
                isGenerating = isGenerating,
                onGenerate = { viewModel.generateGrowthReview(7) },
                onEvaluationClick = onEvaluationClick,
                onKnowledgeCardClick = onKnowledgeCardClick
            )
        }
    }
}

@Composable
private fun TaskListPanel(
    modifier: Modifier = Modifier,
    records: List<GrowthRecord>,
    selectedRecordId: String?,
    selectedCategory: TaskCategory,
    onCategorySelected: (TaskCategory) -> Unit,
    onRecordClick: (String) -> Unit
) {
    Column(modifier = modifier.fillMaxHeight()) {
        CategoryFilterBar(
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected
        )

        if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Insights,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "暂无记录",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onCategorySelected(TaskCategory.ALL) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "去添加目标",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(records, key = { it.id }) { record ->
                    CompactRecordItem(
                        record = record,
                        isSelected = record.id == selectedRecordId,
                        onClick = { onRecordClick(record.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactRecordItem(
    record: GrowthRecord,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val barColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }
    val dateStr = remember(record.date) {
        SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(record.date))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(40.dp)
                .background(barColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = dateStr,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${record.learningMinutes}m",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${record.tasksCompleted}个",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${record.masteryScore}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RecordDetailPanel(
    modifier: Modifier = Modifier,
    record: GrowthRecord?,
    records: List<GrowthRecord>,
    reviewResponse: GrowthReviewResponse?,
    isGenerating: Boolean,
    onGenerate: () -> Unit,
    onEvaluationClick: (String) -> Unit,
    onKnowledgeCardClick: (String) -> Unit
) {
    if (record == null) {
        Box(
            modifier = modifier.fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Insights,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (records.isEmpty()) "该分类暂无记录" else "选择左侧记录查看详情",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    AnimatedContent(
        targetState = record,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        modifier = modifier.fillMaxHeight(),
        label = "recordDetail"
    ) { targetRecord ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 头部：日期 + 掌握度标签
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val fullDate = remember(targetRecord.date) {
                    SimpleDateFormat("yyyy-MM-dd EEEE", Locale.getDefault())
                        .format(Date(targetRecord.date))
                }
                Text(
                    text = fullDate,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = when {
                        targetRecord.masteryScore >= 80 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        targetRecord.masteryScore >= 50 -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    }
                ) {
                    Text(
                        text = "掌握度 ${targetRecord.masteryScore}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // 成长指标卡片行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Timer,
                    value = "${targetRecord.learningMinutes}m",
                    label = "学习时长"
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CheckCircle,
                    value = "${targetRecord.tasksCompleted}",
                    label = "完成任务"
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.School,
                    value = "${targetRecord.knowledgeCardsCreated}",
                    label = "知识卡片"
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Star,
                    value = "${targetRecord.masteryScore}%",
                    label = "掌握度"
                )
            }

            // AI总结区域
            MorandiCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
                Column {
                    Text(
                        text = "AI总结",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val summary = targetRecord.aiSummary
                    if (summary.isNullOrBlank()) {
                        Text(
                            text = "暂无AI总结",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // 成长趋势迷你图表
            MiniTrendChart(records = records)

            // AI复盘区域
            GrowthReviewSection(
                reviewResponse = reviewResponse,
                isGenerating = isGenerating,
                onGenerate = onGenerate
            )

            // 底部快捷操作栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onEvaluationClick(targetRecord.id) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Assessment,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "开始考核",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                OutlinedButton(
                    onClick = { onKnowledgeCardClick(targetRecord.id) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "查看卡片",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                OutlinedButton(
                    onClick = onGenerate,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "AI复盘",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilterBar(
    selectedCategory: TaskCategory,
    onCategorySelected: (TaskCategory) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TaskCategory.values().forEach { category ->
            FilterChip(
                selected = category == selectedCategory,
                onClick = { onCategorySelected(category) },
                label = {
                    Text(
                        text = category.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String
) {
    MorandiCard(
        modifier = modifier,
        radius = 12.dp,
        contentPadding = PaddingValues(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Morandi.BrandPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Morandi.TextPrimary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Morandi.TextSecondary
            )
        }
    }
}

@Composable
private fun MiniTrendChart(records: List<GrowthRecord>) {
    val chartRecords = remember(records) {
        records.takeLast(7)
    }
    val chartColor = MaterialTheme.colorScheme.primary

    MorandiCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Column {
            Text(
                text = "成长趋势（近7天）",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (chartRecords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无趋势数据",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val maxValue = remember(chartRecords) {
                    (chartRecords.maxOfOrNull { it.learningMinutes + it.tasksCompleted * 10 } ?: 1)
                        .coerceAtLeast(1)
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    val width = size.width
                    val height = size.height

                    if (chartRecords.size < 2) {
                        val x = width / 2f
                        val y = height / 2f
                        drawCircle(
                            color = chartColor,
                            radius = 6f,
                            center = Offset(x, y)
                        )
                        return@Canvas
                    }

                    val stepX = width / (chartRecords.size - 1).coerceAtLeast(1)

                    val path = Path()
                    chartRecords.forEachIndexed { index, record ->
                        val yValue = record.learningMinutes + record.tasksCompleted * 10
                        val x = index * stepX
                        val y = height - (yValue.toFloat() / maxValue) * height * 0.85f - 10f
                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = chartColor,
                        style = Stroke(width = 3f)
                    )

                    chartRecords.forEachIndexed { index, record ->
                        val yValue = record.learningMinutes + record.tasksCompleted * 10
                        val x = index * stepX
                        val y = height - (yValue.toFloat() / maxValue) * height * 0.85f - 10f
                        drawCircle(
                            color = chartColor,
                            radius = 6f,
                            center = Offset(x, y)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    chartRecords.forEach { record ->
                        Text(
                            text = SimpleDateFormat("MM-dd", Locale.getDefault())
                                .format(Date(record.date)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GrowthReviewSection(
    reviewResponse: GrowthReviewResponse?,
    isGenerating: Boolean,
    onGenerate: () -> Unit
) {
    MorandiCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI成长复盘",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (!isGenerating && reviewResponse == null) {
                    TextButton(onClick = onGenerate) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("生成")
                    }
                }
            }

            when {
                isGenerating -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "AI正在生成复盘报告...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                reviewResponse != null -> {
                    Spacer(modifier = Modifier.height(16.dp))

                    // 整体评价
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = reviewResponse.overallRating,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 亮点
                    if (reviewResponse.keyHighlights.isNotEmpty()) {
                        ReviewSection(title = "亮点", items = reviewResponse.keyHighlights, color = MaterialTheme.colorScheme.primary)
                    }

                    // 改进点
                    if (reviewResponse.areasForImprovement.isNotEmpty()) {
                        ReviewSection(title = "改进点", items = reviewResponse.areasForImprovement, color = MaterialTheme.colorScheme.error)
                    }

                    // 下周建议
                    if (reviewResponse.nextWeekRecommendations.isNotEmpty()) {
                        ReviewSection(title = "下周建议", items = reviewResponse.nextWeekRecommendations, color = MaterialTheme.colorScheme.secondary)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 鼓励的话
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = reviewResponse.encouragement,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
                else -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "点击右上角「生成」让AI为你复盘最近7天的学习情况",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewSection(
    title: String,
    items: List<String>,
    color: Color
) {
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = color
    )
    items.forEach { item ->
        Row(
            modifier = Modifier.padding(vertical = 2.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "• ",
                color = color
            )
            Text(
                text = item,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
