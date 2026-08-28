package com.aigrowth.os.feature.learning.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aigrowth.os.core.database.entity.Goal
import com.aigrowth.os.core.database.entity.GoalStatus
import com.aigrowth.os.core.design.Morandi
import com.aigrowth.os.core.design.MorandiCard
import com.aigrowth.os.core.design.MorandiEmptyState
import com.aigrowth.os.feature.learning.presentation.GoalListViewModel

/**
 * 目标列表页面——莫兰迪雾蓝紫设计
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalListScreen(
    onGoalClick: (String) -> Unit,
    onAddGoalClick: () -> Unit,
    onMemoryClick: () -> Unit,
    onOpenDrawer: () -> Unit = {},
    viewModel: GoalListViewModel = hiltViewModel()
) {
    val goals by viewModel.goals.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        containerColor = Morandi.BackgroundGray,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "学习目标",
                        fontWeight = FontWeight.Bold,
                        color = Morandi.TextPrimary
                    )
                },
                actions = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "菜单",
                            tint = Morandi.BrandPrimary
                        )
                    }
                    IconButton(onClick = onMemoryClick) {
                        Icon(
                            Icons.Default.Psychology,
                            contentDescription = "AI记忆",
                            tint = Morandi.BrandPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddGoalClick,
                containerColor = Morandi.BrandPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加目标")
            }
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Morandi.BrandPrimary)
                }
            }
            goals.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    MorandiEmptyState(
                        icon = Icons.Default.Flag,
                        title = "还没有学习目标",
                        subtitle = "点击右下角按钮添加你的第一个目标"
                    )
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
                    items(goals, key = { it.id }) { goal ->
                        GoalCard(
                            goal = goal,
                            onClick = { onGoalClick(goal.id) },
                            onDelete = { viewModel.deleteGoal(goal) },
                            onComplete = {
                                viewModel.updateGoalStatus(goal, GoalStatus.COMPLETED)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 目标卡片
 */
@Composable
fun GoalCard(
    goal: Goal,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onComplete: () -> Unit
) {
    MorandiCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = goal.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Morandi.TextPrimary
                )
                if (goal.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = goal.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Morandi.TextSecondary
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = Morandi.TextDelete
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MiniChip(
                text = "完成",
                container = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = onComplete
            )

            if (goal.learningPathId != null) {
                MiniChip(
                    text = "已生成学习路线",
                    container = MaterialTheme.colorScheme.secondaryContainer,
                    content = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = {}
                )
            }
        }
    }
}

/**
 * 小型圆角标签
 */
@Composable
private fun MiniChip(
    text: String,
    container: Color,
    content: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = container,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = content,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
