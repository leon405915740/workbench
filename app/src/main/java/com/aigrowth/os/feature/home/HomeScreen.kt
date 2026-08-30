package com.aigrowth.os.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aigrowth.os.Screen
import com.aigrowth.os.core.database.workbench.entity.StatusTrendEntry
import com.aigrowth.os.feature.pomodoro.PomodoroPhase
import com.aigrowth.os.feature.pomodoro.PomodoroUi
import com.aigrowth.os.feature.pomodoro.PomodoroViewModel
import com.aigrowth.os.feature.statustrend.StatusRecordSheet
import com.aigrowth.os.ui.common.WorkbenchCard
import com.aigrowth.os.ui.common.shadowCard
import com.aigrowth.os.ui.common.todayString
import com.aigrowth.os.ui.theme.AccentGreen
import com.aigrowth.os.ui.theme.DrawerBg
import com.aigrowth.os.ui.theme.DrawerBgTop
import com.aigrowth.os.ui.theme.DrawerText
import com.aigrowth.os.ui.theme.InkSecondary
import com.aigrowth.os.ui.theme.InkTertiary
import com.aigrowth.os.ui.theme.InkText
import com.aigrowth.os.ui.theme.ModuleBlue
import com.aigrowth.os.ui.theme.ModuleClay
import com.aigrowth.os.ui.theme.ModuleGreen
import com.aigrowth.os.ui.theme.ModuleOchre
import com.aigrowth.os.ui.theme.ModuleOlive
import com.aigrowth.os.ui.theme.PaperBg
import com.aigrowth.os.ui.theme.PaperBorder
import com.aigrowth.os.ui.theme.PaperCard
import com.aigrowth.os.ui.theme.PaperNested
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    val vm: HomeViewModel = hiltViewModel()
    val pomodoro: PomodoroViewModel = hiltViewModel()
    val overview by vm.overview.collectAsState()
    val statusEntries by vm.statusEntries.collectAsState()
    val monthExpense by vm.monthExpense.collectAsState()

    var showStatusSheet by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(PaperBg)
            .statusBarsPadding()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { GreetingHeader() }
            item { OverviewRingCard(overview) }
            item { QuickEntries(onNavigate) }
            item {
                PomodoroCard(
                    uiFlow = pomodoro.ui,
                    onStartFocus = remember(pomodoro) { pomodoro::startFocus },
                    onResume = remember(pomodoro) { pomodoro::resume },
                    onPause = remember(pomodoro) { pomodoro::pause },
                    onReset = remember(pomodoro) { pomodoro::reset }
                )
            }
            item { StatusTrendLine(statusEntries) { showStatusSheet = true } }
            item { SummaryCards(overview, monthExpense, onNavigate) }
            item { Spacer(Modifier.height(8.dp)) }
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
        now.hour in 5..11 -> "早安"
        now.hour in 12..17 -> "午安"
        else -> "晚安"
    }
    val dateText = remember {
        LocalDate.now().format(DateTimeFormatterPatterns.date)
    }

    WorkbenchCard(contentPadding = PaddingValues(20.dp)) {
        Box(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 24.dp, y = (-28).dp)
                    .size(132.dp)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(AccentGreen.copy(alpha = 0.18f), Color.Transparent),
                                center = center
                            ),
                            radius = size.minDimension / 2f,
                            center = center
                        )
                    }
            )
            Column(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WbSunny, contentDescription = null, tint = ModuleOchre, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        greeting,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = InkSecondary,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "生活手记",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = InkText
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "记录每一天的进步",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary
                        )
                    }
                    DatePill(dateText)
                }
            }
        }
    }
}

@Composable
private fun DatePill(dateText: String) {
    val shape = RoundedCornerShape(50)
    Surface(
        shape = shape,
        color = PaperCard,
        border = BorderStroke(1.dp, PaperBorder),
        modifier = Modifier.shadowCard(shape)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Icon(Icons.Default.DateRange, contentDescription = null, tint = InkSecondary, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(dateText, style = MaterialTheme.typography.labelSmall, color = InkSecondary)
        }
    }
}

@Composable
private fun QuickEntries(onNavigate: (String) -> Unit) {
    val entries = listOf(
        QuickEntry("记运动", Icons.Default.DirectionsRun, ModuleOchre, Screen.Exercise.route),
        QuickEntry("记打卡", Icons.Default.CheckCircle, ModuleGreen, Screen.Habits.route),
        QuickEntry("记一笔", Icons.Default.AccountBalanceWallet, ModuleClay, "${Screen.Record.route}?openAi=true"),
        QuickEntry("记想法", Icons.Default.EditNote, ModuleOlive, Screen.Essay.route)
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { entry ->
                    QuickEntryItem(entry, Modifier.weight(1f)) { onNavigate(entry.route) }
                }
            }
        }
    }
}

@Composable
private fun QuickEntryItem(entry: QuickEntry, modifier: Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val active = hovered || pressed
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(PaperCard)
            .border(1.dp, PaperBorder, shape)
            .shadowCard(shape)
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(if (active) entry.color else entry.color.copy(alpha = 0.10f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                entry.icon,
                contentDescription = entry.label,
                tint = if (active) Color.White else entry.color,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(entry.label, style = MaterialTheme.typography.labelMedium, color = InkText)
    }
}

@Composable
private fun OverviewRingCard(overview: OverviewUi) {
    val overallPercent = (overview.overall * 100).toInt()
    WorkbenchCard(contentPadding = PaddingValues(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CardTitleBar("今日概览", ModuleGreen)
            Spacer(Modifier.weight(1f))
            Text("完成度 $overallPercent%", style = MaterialTheme.typography.labelMedium, color = InkSecondary)
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RingProgress(overview.overall, "$overallPercent%", "今日")
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniProgress("待办", overview.planProgress, "${overview.planDone}/${overview.planTotal}", ModuleGreen, Icons.Default.Checklist)
                MiniProgress("习惯", overview.habitProgress, "${overview.habitDoneToday}/${overview.habitActive}", ModuleBlue, Icons.Default.EventAvailable)
                MiniProgress("运动", overview.exerciseProgress, formatExerciseMinutes(overview.exerciseCompleted), ModuleOchre, Icons.Default.DirectionsRun)
                MiniProgress("阅读", overview.readingProgress, "${overview.readingCompleted}/${overview.readingTotal}", ModuleOlive, Icons.Default.MenuBook)
            }
        }
    }
}

@Composable
private fun RingProgress(progress: Float, centerTop: String, centerBottom: String) {
    Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(120.dp),
            strokeWidth = 8.dp,
            color = AccentGreen,
            trackColor = PaperNested
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerTop, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = InkText)
            Text(centerBottom, style = MaterialTheme.typography.labelSmall, color = InkSecondary)
        }
    }
}

@Composable
private fun MiniProgress(label: String, progress: Float, fraction: String, color: Color, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(color.copy(alpha = 0.10f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = InkSecondary)
                Text(fraction, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = color)
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(5.dp),
                color = color,
                trackColor = PaperNested
            )
        }
    }
}

@Composable
private fun PomodoroCard(
    uiFlow: StateFlow<PomodoroUi>,
    onStartFocus: () -> Unit,
    onResume: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit
) {
    val ui by uiFlow.collectAsState()
    val shape = RoundedCornerShape(18.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .shadowCard(shape)
            .clip(shape)
            .background(Brush.linearGradient(listOf(DrawerBgTop, DrawerBg)))
    ) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 20.dp, y = (-20).dp)
                .size(108.dp)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(AccentGreen.copy(alpha = 0.30f), Color.Transparent),
                            center = center
                        ),
                        radius = size.minDimension / 2f,
                        center = center
                    )
                }
        )
        Column(
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "POMODORO",
                style = MaterialTheme.typography.labelSmall,
                color = DrawerText.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(2.dp))
            Text("专注时刻", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = DrawerText)
            Spacer(Modifier.height(14.dp))
            PomodoroRing(progress = ui.progress, remainText = formatClock(ui.remainSeconds))
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                when {
                    ui.running -> DarkPrimaryButton("暂停", onPause)
                    ui.remainSeconds < ui.totalSeconds -> DarkPrimaryButton("继续", onResume)
                    else -> DarkPrimaryButton(if (ui.phase == PomodoroPhase.BREAK) "开始休息" else "开始专注", onStartFocus)
                }
                DarkOutlineButton("重置", onReset)
            }
        }
    }
}

@Composable
private fun PomodoroRing(progress: Float, remainText: String) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(108.dp)) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.matchParentSize(),
            strokeWidth = 8.dp,
            color = DrawerText,
            trackColor = DrawerText.copy(alpha = 0.15f)
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(remainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = DrawerText)
            Text("min : sec", style = MaterialTheme.typography.labelSmall, color = DrawerText.copy(alpha = 0.55f))
        }
    }
}

@Composable
private fun DarkPrimaryButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = DrawerText, contentColor = DrawerBg),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DarkOutlineButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = DrawerText),
        border = BorderStroke(1.dp, DrawerText.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StatusTrendLine(entries: List<StatusTrendEntry>, onRecord: () -> Unit) {
    val today = LocalDate.now()
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val byDate = remember(entries) { entries.associateBy { it.date } }
    val recorded = entries.size

    WorkbenchCard(contentPadding = PaddingValues(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CardTitleBar("状态趋势", ModuleClay)
            Spacer(Modifier.weight(1f))
            Text("近 7 天", style = MaterialTheme.typography.labelMedium, color = InkSecondary)
            Spacer(Modifier.width(6.dp))
            TextButton(onClick = onRecord) { Text("记录今天", color = ModuleClay) }
        }
        Spacer(Modifier.height(8.dp))
        if (recorded == 0) {
            Text(
                "记录每天的状态，一周后就能看到自己的趋势",
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary
            )
        } else {
            val filled = remember(days, byDate) {
                var carry = 0
                days.map { day -> (byDate[day.toString()]?.score ?: carry).also { carry = it } }
            }
            TrendLineChart(filled)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("周一", "周三", "周五", "周日").forEach { label ->
                    Text(label, style = MaterialTheme.typography.labelSmall, color = InkTertiary)
                }
            }
        }
    }
}

private val TrendColor = ModuleClay

@Composable
private fun TrendLineChart(scores: List<Int>) {
    Canvas(Modifier.fillMaxWidth().height(96.dp)) {
        val w = size.width
        val h = size.height
        val stepX = if (scores.size > 1) w / (scores.size - 1).toFloat() else 0f
        val points = scores.mapIndexed { i, s ->
            Offset(stepX * i, h - (s / 100f) * h)
        }
        val area = Path().apply {
            moveTo(points.first().x, h)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, h)
            close()
        }
        drawPath(area, brush = Brush.verticalGradient(listOf(TrendColor.copy(alpha = 0.30f), Color.Transparent)))
        val line = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(line, color = TrendColor, style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(TrendColor, radius = 5f, center = points.last())
    }
}

@Composable
private fun SummaryCards(overview: OverviewUi, monthExpense: Double?, onNavigate: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryItem(
            "本月支出",
            monthExpense?.let { "¥${formatAmount(it)}" } ?: "—",
            Icons.Default.AccountBalanceWallet,
            ModuleClay,
            Modifier.weight(1f),
            "本月累计"
        ) { onNavigate(Screen.Record.route) }
        SummaryItem(
            "本月阅读",
            "${overview.readingCompleted}/${overview.readingTotal}",
            Icons.Default.MenuBook,
            ModuleBlue,
            Modifier.weight(1f),
            "本月完成"
        ) { onNavigate(Screen.Reading.route) }
        SummaryItem(
            "本月运动",
            formatExerciseMinutes(overview.exerciseCompleted),
            Icons.Default.DirectionsRun,
            ModuleOchre,
            Modifier.weight(1f),
            "目标 ${formatExerciseMinutes(overview.exerciseTotal)}"
        ) { onNavigate(Screen.Exercise.route) }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier,
    meta: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(PaperCard)
            .border(1.dp, PaperBorder, shape)
            .shadowCard(shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(5.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = InkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = InkText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(meta, style = MaterialTheme.typography.labelSmall, color = InkTertiary, maxLines = 1)
    }
}

@Composable
private fun CardTitleBar(title: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 4.dp, height = 14.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = InkText)
    }
}

private object DateTimeFormatterPatterns {
    val date = java.time.format.DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINESE)
}

private data class QuickEntry(val label: String, val icon: ImageVector, val color: Color, val route: String)

private fun formatAmount(value: Double): String {
    return if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.US, "%.2f", value)
}

private fun formatExerciseMinutes(minutes: Int): String {
    return if (minutes < 60) "${minutes}m" else "${minutes / 60}h ${minutes % 60}m"
}

private fun formatClock(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
