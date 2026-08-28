package com.aigrowth.os.feature.learning.presentation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aigrowth.os.core.design.GradientSummaryCard
import com.aigrowth.os.core.design.Morandi
import com.aigrowth.os.core.design.MorandiCard
import com.aigrowth.os.feature.learning.presentation.DashboardOverview
import com.aigrowth.os.feature.learning.presentation.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenDrawer: () -> Unit = {},
    onGoalListClick: () -> Unit,
    onTaskClick: (String) -> Unit,
    onTaskListClick: () -> Unit,
    onAccountingClick: () -> Unit = {},
    onGrowthClick: () -> Unit = {},
    onCreatorClick: () -> Unit = {},
    onFitnessClick: () -> Unit = {},
    onEnglishClick: () -> Unit = {},
    onProgrammingClick: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val overview by viewModel.overview.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        containerColor = Morandi.BackgroundGray,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "工作台",
                        fontWeight = FontWeight.Bold,
                        color = Morandi.TextPrimary
                    )
                },
                actions = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "菜单",
                            tint = Morandi.BrandPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Morandi.BrandPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    GrowthScoreGauge(overview = overview, onTaskListClick = onTaskListClick)
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CoreEntryCard(
                            icon = Icons.Default.Flag,
                            title = "待办",
                            subtitle = "管理你的学习目标",
                            onClick = onGoalListClick,
                            modifier = Modifier.weight(1f)
                        )
                        CoreEntryCard(
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            title = "记录",
                            subtitle = "查看成长数据",
                            onClick = onGrowthClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Text(
                        text = "应用",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Morandi.TextPrimary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AppEntryCard(
                            icon = Icons.Default.AccountBalanceWallet,
                            name = "记账",
                            onClick = onAccountingClick,
                            modifier = Modifier.weight(1f)
                        )
                        AppEntryCard(
                            icon = Icons.Default.FitnessCenter,
                            name = "健身",
                            onClick = onFitnessClick,
                            modifier = Modifier.weight(1f)
                        )
                        AppEntryCard(
                            icon = Icons.Default.Create,
                            name = "自媒体",
                            onClick = onCreatorClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AppEntryCard(
                            icon = Icons.Default.Translate,
                            name = "英语",
                            onClick = onEnglishClick,
                            modifier = Modifier.weight(1f)
                        )
                        AppEntryCard(
                            icon = Icons.Default.Code,
                            name = "编程",
                            onClick = onProgrammingClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GrowthScoreGauge(overview: DashboardOverview, onTaskListClick: () -> Unit) {
    val score = overview.todayGrowthScore.coerceIn(0, 100)

    GradientSummaryCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "今日成长值",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier.size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 20f
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset(
                        (size.width - diameter) / 2f,
                        (size.height - diameter) / 2f
                    )
                    val arcSize = Size(diameter, diameter)

                    drawArc(
                        color = Color.White.copy(alpha = 0.25f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    val sweepAngle = 360f * (score / 100f)
                    drawArc(
                        color = Color.White,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$score",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "分",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = formatDate(System.currentTimeMillis()),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "完成 ${overview.tasksCompleted}/${overview.tasksTotal} 个任务 · 学习 ${overview.learningMinutes} 分钟",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "查看今日任务 →",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.18f))
                    .clickable(onClick = onTaskListClick)
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun CoreEntryCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MorandiCard(
        modifier = modifier.clickable(onClick = onClick),
        radius = 16.dp,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Morandi.BrandPrimary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Morandi.TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Morandi.TextSecondary
            )
        }
    }
}

@Composable
private fun AppEntryCard(
    icon: ImageVector,
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MorandiCard(
        modifier = modifier.clickable(onClick = onClick),
        radius = 16.dp,
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Morandi.BrandPrimary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = Morandi.TextPrimary
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.CHINESE)
        .format(Date(timestamp))
}