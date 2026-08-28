package com.aigrowth.os.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aigrowth.os.Screen
import com.aigrowth.os.feature.pomodoro.PomodoroCard
import com.aigrowth.os.feature.pomodoro.PomodoroViewModel
import com.aigrowth.os.feature.statustrend.StatusRecordSheet
import com.aigrowth.os.feature.statustrend.StatusTrendSection
import com.aigrowth.os.ui.common.WeeklyHabitTracker
import com.aigrowth.os.ui.common.WorkbenchCard
import com.aigrowth.os.ui.common.WorkbenchTopBar
import com.aigrowth.os.ui.common.todayString
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val quotes = listOf(
    "把今天最重要的一件事做完，其余的都会顺起来。",
    "坚持一件小事，时间会给你答案。",
    "善待自己，保持节奏，稳步向前。",
    "每一个专注的 25 分钟，都在为更好的自己投票。",
    "记录让生活有迹可循，回顾让成长有据可依。"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    val vm: HomeViewModel = hiltViewModel()
    val pomodoro: PomodoroViewModel = hiltViewModel()
    val overview by vm.overview.collectAsState()
    val focus by vm.focus.collectAsState()
    val statusEntries by vm.statusEntries.collectAsState()
    val monthExpense by vm.monthExpense.collectAsState()
    val pomodoroUi by pomodoro.ui.collectAsState()

    val today = LocalDate.now()
    val week = remember {
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        (0 until 7).map { monday.plusDays(it.toLong()) }
    }
    var showStatusSheet by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            WorkbenchTopBar("首页", "今天也要稳稳地向前")
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { GreetingHeader() }
                if (focus.isNotEmpty()) {
                    item { FocusSection(focus, onNavigate) }
                }
                item { QuickEntries(onNavigate) }
                item { OverviewRingCard(overview) }
                if (overview.activeHabits.isNotEmpty()) {
                    item {
                        WeeklyHabitTracker(
                            activeHabits = overview.activeHabits,
                            week = week,
                            today = today.toString(),
                            checkedKeys = overview.habitChecked,
                            onToggle = vm::toggleCheck
                        )
                    }
                }
                item { TodayPlanCard(overview, onNavigate) }
                item {
                    PomodoroCard(
                        ui = pomodoroUi,
                        onStartFocus = pomodoro::startFocus,
                        onResume = pomodoro::resume,
                        onPause = pomodoro::pause,
                        onReset = pomodoro::reset
                    )
                }
                item { StatusTrendSection(statusEntries) { showStatusSheet = true } }
                item { SummaryCards(overview, monthExpense, onNavigate) }
                item { Spacer(Modifier.height(8.dp)) }
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
private fun GreetingHeader() {
    val now = LocalTime.now()
    val greeting = when {
        now.hour in 5..11 -> "早上好"
        now.hour in 12..17 -> "下午好"
        else -> "晚上好"
    }
    var timeText by remember { mutableStateOf(now.format(DateTimeFormatter.ofPattern("HH:mm"))) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            timeText = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        }
    }
    val dateText = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINESE))
    }
    val quote = quotes[LocalDate.now().dayOfYear % quotes.size]

    WorkbenchCard(
        color = Color(0xFF397565),
        contentPadding = PaddingValues(20.dp)
    ) {
        Text(
            "$dateText · $timeText",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFFDCEBE5)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "$greeting，开始今天吧",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(Modifier.height(8.dp))
        Text(quote, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE6F1EC))
    }
}

@Composable
private fun FocusSection(items: List<FocusItem>, onNavigate: (String) -> Unit) {
    WorkbenchCard(contentPadding = PaddingValues(16.dp)) {
        Text("今日聚焦", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { item ->
                val route = focusRoute(item)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF0F5F1))
                        .clickable { onNavigate(route) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(color = Color(0xFFD6EAE2), shape = CircleShape) {
                        Icon(
                            Icons.Default.Flag,
                            contentDescription = null,
                            tint = Color(0xFF397565),
                            modifier = Modifier.padding(8.dp).size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${item.module} · ${item.subtitle}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF9FB0A9))
                }
            }
        }
    }
}

@Composable
private fun QuickEntries(onNavigate: (String) -> Unit) {
    val entries = listOf(
        QuickEntry("记运动", Icons.Default.DirectionsRun, Screen.Exercise.route),
        QuickEntry("记打卡", Icons.Default.CheckCircle, Screen.Habits.route),
        QuickEntry("记一笔", Icons.Default.AccountBalanceWallet, "${Screen.Record.route}?openAi=true"),
        QuickEntry("记想法", Icons.Default.EditNote, Screen.Essay.route)
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        entries.forEach { entry ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onNavigate(entry.route) }
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(color = Color(0xFFD6EAE2), shape = CircleShape) {
                    Icon(
                        entry.icon,
                        contentDescription = entry.label,
                        tint = Color(0xFF397565),
                        modifier = Modifier.padding(10.dp).size(22.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(entry.label, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun OverviewRingCard(overview: OverviewUi) {
    val accent = Color(0xFF397565)
    val overallPercent = (overview.overall * 100).toInt()

    WorkbenchCard(contentPadding = PaddingValues(20.dp)) {
        Text("今日概览", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { overview.overall },
                    modifier = Modifier.size(108.dp),
                    strokeWidth = 10.dp,
                    color = accent,
                    trackColor = Color(0xFFDDE7E2)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$overallPercent%",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E2A26)
                    )
                    Text("整体完成", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniProgress("计划", overview.planProgress, "${overview.planDone}/${overview.planTotal}", accent)
                MiniProgress("习惯", overview.habitProgress, "${overview.habitDoneToday}/${overview.habitActive}", accent)
                MiniProgress("阅读", overview.readingProgress, "${overview.readingCompleted}/${overview.readingTotal}", accent)
                MiniProgress("运动", overview.exerciseProgress, "${overview.exerciseCompleted}/${overview.exerciseTotal}", accent)
            }
        }
    }
}

@Composable
private fun MiniProgress(label: String, progress: Float, fraction: String, color: Color) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(fraction, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = color,
            trackColor = Color(0xFFDDE7E2)
        )
    }
}

@Composable
private fun TodayPlanCard(overview: OverviewUi, onNavigate: (String) -> Unit) {
    WorkbenchCard(
        modifier = Modifier.clickable { onNavigate(Screen.Plan.route) },
        contentPadding = PaddingValues(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("今日计划", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Text(
                    "完成 ${overview.planDone}/${overview.planTotal}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF9FB0A9))
        }
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { overview.planProgress },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = Color(0xFF397565),
            trackColor = Color(0xFFDDE7E2)
        )
        if (overview.keyTodos.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                overview.keyTodos.forEach { todo ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(6.dp).background(Color(0xFF397565), CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            todo.title,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCards(overview: OverviewUi, monthExpense: Double?, onNavigate: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryItem(
            "本月支出",
            monthExpense?.let { "¥${formatAmount(it)}" } ?: "—",
            Icons.Default.AccountBalanceWallet,
            Modifier.weight(1f),
            onClick = { onNavigate(Screen.Record.route) }
        )
        SummaryItem(
            "阅读",
            "${overview.readingCompleted}/${overview.readingTotal}",
            Icons.Default.MenuBook,
            Modifier.weight(1f),
            onClick = { onNavigate(Screen.Reading.route) }
        )
        SummaryItem(
            "运动",
            "${overview.exerciseCompleted}/${overview.exerciseTotal}",
            Icons.Default.DirectionsRun,
            Modifier.weight(1f),
            onClick = { onNavigate(Screen.Exercise.route) }
        )
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF397565), modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(10.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private data class QuickEntry(val label: String, val icon: ImageVector, val route: String)

private fun focusRoute(item: FocusItem): String = when (item.key.substringBefore(':')) {
    "plan" -> Screen.Plan.route
    "habit" -> Screen.Habits.route
    "reading" -> Screen.Reading.route
    "exercise" -> Screen.Exercise.route
    "essay" -> Screen.Essay.route
    "clipping" -> Screen.Clipping.route
    else -> Screen.Home.route
}

private fun formatAmount(value: Double): String {
    return if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.US, "%.2f", value)
}