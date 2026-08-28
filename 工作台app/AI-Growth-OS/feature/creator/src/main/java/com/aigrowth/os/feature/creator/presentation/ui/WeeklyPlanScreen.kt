package com.aigrowth.os.feature.creator.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aigrowth.os.core.aiengine.DailyPlan
import com.aigrowth.os.feature.creator.presentation.CreatorViewModel

/**
 * 7天学习计划页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyPlanScreen(
    onNavigateBack: () -> Unit,
    viewModel: CreatorViewModel = hiltViewModel()
) {
    var goal by remember { mutableStateOf("") }
    var selectedDays by remember { mutableIntStateOf(7) }

    val weeklyPlanResponse by viewModel.weeklyPlanResponse.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val error by viewModel.error.collectAsState()

    val dayOptions = listOf(3, 5, 7, 14)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "7天学习计划",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 输入区域
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = goal,
                        onValueChange = { goal = it },
                        label = { Text("学习目标") },
                        placeholder = { Text("例如：7天掌握Git基础、3天学会SQL查询...") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Default.Flag, contentDescription = null)
                        }
                    )

                    Text(
                        text = "计划天数",
                        style = MaterialTheme.typography.labelMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        dayOptions.forEach { days ->
                            FilterChip(
                                selected = selectedDays == days,
                                onClick = { selectedDays = days },
                                label = { Text("${days}天") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (goal.isNotBlank()) {
                                viewModel.generateWeeklyPlan(goal, selectedDays)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = goal.isNotBlank() && !isGenerating
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (isGenerating) "生成中..." else "AI生成计划")
                    }
                }
            }

            // 错误提示
            error?.let { errorMsg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMsg,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // 周计划结果
            weeklyPlanResponse?.let { response ->
                // 计划摘要
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = response.goal,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = response.planSummary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // 每日计划
                response.dailyPlans.forEach { dailyPlan ->
                    DailyPlanItem(dailyPlan = dailyPlan)
                }

                // 学习建议
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "学习建议",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        response.tips.forEachIndexed { index, tip ->
                            Text(
                                text = "${index + 1}. $tip",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (index < response.tips.size - 1) {
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }

            // 提示
            if (weeklyPlanResponse == null && error == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "输入学习目标，AI将为你生成详细的每日学习计划",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyPlanItem(dailyPlan: DailyPlan) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "Day ${dailyPlan.day}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dailyPlan.theme,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${dailyPlan.estimatedMinutes}分钟",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "学习任务：",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            dailyPlan.tasks.forEachIndexed { index, task ->
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = "${index + 1}. ",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = task,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (dailyPlan.resources.isNotEmpty()) {
                Text(
                    text = "推荐资源：",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                dailyPlan.resources.forEach { resource ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = resource,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
