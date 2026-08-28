package com.aigrowth.os.feature.insight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aigrowth.os.feature.statustrend.StatusRecordSheet
import com.aigrowth.os.feature.statustrend.StatusTrendSection
import com.aigrowth.os.ui.common.WorkbenchCard
import com.aigrowth.os.ui.common.WorkbenchTopBar
import com.aigrowth.os.ui.common.todayString
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightScreen(vm: InsightViewModel = hiltViewModel()) {
    val insight by vm.insight.collectAsState()
    val pomodoro by vm.pomodoro.collectAsState()
    val monthExpense by vm.monthExpense.collectAsState()
    val statusEntries by vm.statusEntries.collectAsState()

    var showStatusSheet by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            WorkbenchTopBar("洞察", "回顾过去，看清方向")
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    StatCard(
                        title = "计划",
                        value = "${insight.planDone}/${insight.planTotal}",
                        caption = "已完成",
                        icon = Icons.Default.Checklist,
                        progress = insight.planProgress
                    )
                }
                item {
                    StatCard(
                        title = "习惯",
                        value = "${insight.habitActive}",
                        caption = "活跃 · 累计 ${insight.habitTotalLogs} 次",
                        icon = Icons.Default.CheckCircle,
                        progress = null
                    )
                }
                item {
                    StatCard(
                        title = "连续打卡",
                        value = "${insight.avgStreak}天",
                        caption = "平均连续",
                        icon = Icons.Default.LocalFireDepartment,
                        progress = null
                    )
                }
                item {
                    StatCard(
                        title = "阅读",
                        value = "${insight.readingCompleted}/${insight.readingTotal}",
                        caption = "已完成后读",
                        icon = Icons.Default.MenuBook,
                        progress = insight.readingProgress
                    )
                }
                item {
                    StatCard(
                        title = "运动",
                        value = "${insight.exerciseCompleted}/${insight.exerciseTotal}",
                        caption = "已完成",
                        icon = Icons.Default.DirectionsRun,
                        progress = insight.exerciseProgress
                    )
                }
                item {
                    StatCard(
                        title = "本月支出",
                        value = monthExpense?.let { "¥${formatAmount(it)}" } ?: "—",
                        caption = "记账收支",
                        icon = Icons.Default.AccountBalanceWallet,
                        progress = null
                    )
                }
                item {
                    StatCard(
                        title = "番茄钟",
                        value = "${pomodoro?.focusCount ?: 0}次",
                        caption = "累计 ${pomodoro?.totalFocusMinutes ?: 0} 分钟",
                        icon = Icons.Default.Timer,
                        progress = null
                    )
                }
                item {
                    StatCard(
                        title = "状态记录",
                        value = "${statusEntries.size}",
                        caption = "累计记录天数",
                        icon = Icons.Default.Insights,
                        progress = null
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    StatusTrendSection(statusEntries) { showStatusSheet = true }
                }
            }
        }
    }

    if (showStatusSheet) {
        val todayEntry = statusEntries.firstOrNull { it.date == todayString() }
        StatusRecordSheet(
            initialScore = todayEntry?.score,
            initialNote = todayEntry?.note,
            onSave = { score, note ->
                vm.upsertStatus(score, note)
                showStatusSheet = false
            },
            onDismiss = { showStatusSheet = false }
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    caption: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    progress: Float?
) {
    WorkbenchCard(contentPadding = PaddingValues(16.dp)) {
        Icon(icon, contentDescription = null, tint = Color(0xFF397565), modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(10.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(title, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(2.dp))
        Text(
            caption,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (progress != null) {
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = Color(0xFF397565),
                trackColor = Color(0xFFDDE7E2)
            )
        }
    }
}

private fun formatAmount(value: Double): String {
    return if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.US, "%.2f", value)
}