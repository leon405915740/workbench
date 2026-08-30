package com.aigrowth.os.feature.habit

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aigrowth.os.core.database.workbench.entity.ExerciseCategoryEnum
import com.aigrowth.os.core.database.workbench.entity.Habit
import com.aigrowth.os.core.database.workbench.entity.HabitLog
import com.aigrowth.os.ui.common.*
import com.aigrowth.os.ui.theme.*
import com.aigrowth.os.util.WorkbenchImageStore
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitScreen(date: String? = null, vm: HabitViewModel = hiltViewModel()) {
    val habits by vm.habits.collectAsState()
    val activeHabits by vm.activeHabits.collectAsState()
    val stats by vm.stats.collectAsState()
    val logs by vm.logs.collectAsState()
    val recovery by vm.recovery.collectAsState()
    val editRequest by vm.editRequest.collectAsState()
    val toast by vm.toast.collectAsState()

    val checkedKeys = remember(logs) { logs.mapTo(hashSetOf()) { "${it.habitId}|${it.date}" } }
    val today = todayString()
    val week = remember {
        val now = LocalDate.now()
        val monday = now.minusDays((now.dayOfWeek.value - 1).toLong())
        (0 until 7).map { monday.plusDays(it.toLong()) }
    }
    val allHabits = remember(habits) { habits.map { it.habit } }
    val currentStreak = remember(habits) { habits.maxOfOrNull { it.streak } ?: 0 }
    val bestStreak = remember(logs) { bestStreakOf(logs) }
    val monthCount = remember(logs) { thisMonthCount(logs) }
    val rate = remember(logs, stats.active) { monthRate(logs, stats.active) }

    var editorTarget by remember { mutableStateOf<Habit?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Habit?>(null) }
    val context = LocalContext.current

    LaunchedEffect(date) {
        if (!date.isNullOrBlank()) vm.openMakeup(date)
    }
    LaunchedEffect(toast) {
        toast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            vm.consumeToast()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            WorkbenchTopBar(
                title = "习惯打卡",
                subtitle = "坚持每一天的小习惯",
                icon = Icons.Default.CheckCircle,
                iconTint = ModuleGreen,
                action = {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ModuleGreen)
                            .clickable { editorTarget = null; showEditor = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "新建习惯", tint = OnAccentGreen, modifier = Modifier.size(20.dp))
                    }
                }
            )
            HabitStatsRow(stats, currentStreak)
            Spacer(Modifier.height(6.dp))
            Box(Modifier.weight(1f)) {
                if (habits.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.CheckCircle,
                        title = "还没有习惯",
                        message = "点右上角 + 新建第一个习惯"
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (activeHabits.isNotEmpty()) {
                            item(key = "weekly") {
                                WeeklyHabitTracker(
                                    activeHabits = activeHabits,
                                    week = week,
                                    today = today,
                                    checkedKeys = checkedKeys,
                                    onToggle = vm::onCheckClick
                                )
                                Spacer(Modifier.height(2.dp))
                            }
                        }
                        items(habits, key = { it.habit.id }) { ui ->
                            HabitCard(
                                ui = ui,
                                onToggleToday = { vm.onCheckClick(ui.habit.id, today) },
                                onToggleActive = { vm.setActive(ui.habit, !ui.habit.active) },
                                onTogglePinned = { vm.togglePinned(ui.habit) },
                                onEdit = { editorTarget = ui.habit; showEditor = true },
                                onDelete = { pendingDelete = ui.habit }
                            )
                        }
                        item(key = "mini") {
                            HabitStatsMiniCards(currentStreak, bestStreak, monthCount, rate)
                        }
                    }
                }
            }
        }

        if (showEditor) {
            HabitEditorSheet(
                initial = editorTarget,
                onDismiss = { showEditor = false },
                onSave = { title, imageUri, category ->
                    val target = editorTarget
                    if (target == null) {
                        if (title.isNotBlank()) vm.add(title, imageUri, category)
                    } else {
                        vm.edit(target, title, imageUri, category)
                    }
                    showEditor = false
                }
            )
        }

        editRequest?.let { request ->
            val editingExists = request.habitId?.let { id ->
                logs.any { it.habitId == id && it.date == request.date }
            } ?: false
            LogEditorSheet(
                habits = allHabits,
                request = request,
                onSave = vm::saveLog,
                onDelete = if (editingExists && request.habitId != null) {
                    { vm.deleteLog(request.habitId, request.date) }
                } else null,
                onDismiss = vm::dismissEdit
            )
        }

        pendingDelete?.let { target ->
            ConfirmDeleteDialog(
                message = "确定删除「${target.title}」吗？该习惯的全部打卡历史也会一并删除。",
                onConfirm = {
                    WorkbenchImageStore.delete(context, target.imageUri)
                    vm.delete(target)
                    pendingDelete = null
                },
                onDismiss = { pendingDelete = null }
            )
        }

        if (recovery.isNotEmpty()) {
            RecoveryDialog(
                recovery = recovery,
                habits = allHabits,
                onRecoverAll = vm::dismissRecovery,
                onAbandonAll = vm::abandonAllRecovery,
                onAbandonOne = vm::abandonRecovery,
                onDismiss = vm::dismissRecovery
            )
        }
    }
}

@Composable
private fun HabitStatsRow(stats: HabitStats, currentStreak: Int) {
    WorkbenchCard(modifier = Modifier.padding(horizontal = 16.dp), contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 10.dp.toPx()
                    val inset = stroke / 2
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    val topLeft = Offset(inset, inset)
                    drawArc(color = PaperNested, startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                    drawArc(color = ModuleGreen, startAngle = -90f, sweepAngle = 360f * stats.progress, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${stats.doneToday}/${stats.active}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("已完成", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                Text("今日进度", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                val remaining = stats.active - stats.doneToday
                Text(
                    if (remaining > 0) "还差 $remaining 个习惯，保持节奏，今天也是稳定向前的一天。" else "全部完成，今天也是稳稳向前的一天。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(shape = RoundedCornerShape(999.dp), color = AccentGreenSoft) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = ModuleGreen, modifier = Modifier.size(12.dp))
                            Text("$currentStreak 天", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = ModuleGreen)
                        }
                    }
                    Text("当前连续", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun HabitStatsMiniCards(currentStreak: Int, bestStreak: Int, monthCount: Int, rate: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HabitStatMiniCard("当前连续", Icons.Default.LocalFireDepartment, "$currentStreak", "天", ModuleOchre, Modifier.weight(1f))
            HabitStatMiniCard("最佳连续", Icons.Default.EmojiEvents, "$bestStreak", "天", ModuleGreen, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HabitStatMiniCard("本月完成", Icons.Default.CheckBox, "$monthCount", "次", ModuleBlue, Modifier.weight(1f))
            HabitStatMiniCard("完成率", Icons.Default.Percent, "$rate%", "平均", ModuleClay, Modifier.weight(1f))
        }
    }
}

@Composable
private fun HabitStatMiniCard(label: String, icon: ImageVector, value: String, unit: String, color: Color, modifier: Modifier = Modifier) {
    WorkbenchCard(modifier = modifier, contentPadding = PaddingValues(vertical = 14.dp)) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = color)
            Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HabitCard(
    ui: HabitUi,
    onToggleToday: () -> Unit,
    onToggleActive: () -> Unit,
    onTogglePinned: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val habit = ui.habit
    val color = habitCategoryColor(habit.category)
    WorkbenchCard(contentPadding = PaddingValues(12.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(habitIcon(habit.category), contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            habit.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            textDecoration = if (habit.active) null else TextDecoration.LineThrough,
                            color = if (habit.active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (habit.pinnedAt != null) {
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.PushPin, contentDescription = "已置顶", tint = ModuleGreen, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("连续 ${ui.streak} 天", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("累计 ${ui.total} 天", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!habit.active) TagChip("已停用")
                    }
                }
                Spacer(Modifier.width(8.dp))
                RoundCheckButton(checked = ui.checkedToday, enabled = habit.active, color = color, onClick = onToggleToday)
            }
            if (habit.imageUri != null) {
                Spacer(Modifier.height(10.dp))
                WorkbenchImage(habit.imageUri, Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(10.dp)))
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                PinAction(
                    pinned = habit.pinnedAt != null,
                    tint = if (habit.pinnedAt != null) ModuleGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onTogglePinned
                )
                MiniAction(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
                MiniAction(onClick = onToggleActive) {
                    Icon(
                        if (habit.active) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (habit.active) "停用" else "启用",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                MiniAction(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = DangerInk, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun RoundCheckButton(checked: Boolean, enabled: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .then(
                if (checked) Modifier.background(color)
                else Modifier.border(2.dp, if (enabled) color.copy(alpha = 0.55f) else PaperBorderStrong, CircleShape)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = if (checked) "已完成" else "打卡",
            tint = if (checked) OnAccentGreen else if (enabled) color.copy(alpha = 0.55f) else InkTertiary,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun MiniAction(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

private fun habitIcon(category: ExerciseCategoryEnum?): ImageVector = when (category) {
    ExerciseCategoryEnum.CARDIO -> Icons.Default.DirectionsRun
    ExerciseCategoryEnum.UPPER, ExerciseCategoryEnum.LOWER, ExerciseCategoryEnum.CORE, ExerciseCategoryEnum.FUNCTIONAL -> Icons.Default.FitnessCenter
    ExerciseCategoryEnum.OTHER, null -> Icons.Default.CheckCircle
}

private fun bestStreakOf(logs: List<HabitLog>): Int {
    return logs.groupBy { it.habitId }.values.maxOfOrNull { ds ->
        val dates = ds.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.distinct().sorted()
        var best = 0
        var cur = 0
        var prev: LocalDate? = null
        for (d in dates) {
            cur = if (prev != null && d == prev.plusDays(1)) cur + 1 else 1
            if (cur > best) best = cur
            prev = d
        }
        best
    } ?: 0
}

private fun thisMonthCount(logs: List<HabitLog>): Int {
    val ym = YearMonth.now()
    return logs.count { runCatching { LocalDate.parse(it.date) }.getOrNull()?.let { YearMonth.from(it) == ym } == true }
}

private fun monthRate(logs: List<HabitLog>, active: Int): Int {
    if (active <= 0) return 0
    val ym = YearMonth.now()
    val done = logs.count { runCatching { LocalDate.parse(it.date) }.getOrNull()?.let { YearMonth.from(it) == ym } == true }
    val expected = active * LocalDate.now().dayOfMonth
    return if (expected == 0) 0 else (done * 100 / expected).coerceIn(0, 100)
}

@Composable
private fun RecoveryDialog(
    recovery: List<RecoverableTimer>,
    habits: List<Habit>,
    onRecoverAll: () -> Unit,
    onAbandonAll: () -> Unit,
    onAbandonOne: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("恢复运动计时") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("检测到以下进行中的运动计时，可选择恢复或放弃（放弃不保存时长）。")
                recovery.forEach { t ->
                    val title = habits.firstOrNull { it.id == t.habitId }?.title ?: "运动"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                formatExerciseElapsed(t.elapsedSeconds),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { onAbandonOne(t.habitId) }) { Text("放弃") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onRecoverAll) { Text("全部恢复") } },
        dismissButton = { TextButton(onClick = onAbandonAll) { Text("全部放弃") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun HabitEditorSheet(
    initial: Habit?,
    onDismiss: () -> Unit,
    onSave: (title: String, imageUri: String?, category: ExerciseCategoryEnum?) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var category by remember(initial?.category) { mutableStateOf(initial?.category) }
    val attachment = rememberImageAttachment(initial?.imageUri)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(if (initial == null) "新建习惯" else "编辑习惯", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("习惯名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            WorkbenchImagePicker(
                current = attachment.draft,
                onPick = attachment::onPick,
                onRemove = attachment::onRemove
            )
            Button(
                onClick = { onSave(title, attachment.resolve(), category) },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogEditorSheet(
    habits: List<Habit>,
    request: LogEditRequest,
    onSave: (habitId: String, date: String, durationMinutes: Int?, note: String?, category: ExerciseCategoryEnum?) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    var selectedHabitId by remember(request.habitId) { mutableStateOf(request.habitId ?: habits.firstOrNull()?.id) }
    val selectedHabit = habits.firstOrNull { it.id == selectedHabitId }
    var date by remember(request.date) { mutableStateOf(request.date) }
    var duration by remember(request.durationMinutes) { mutableStateOf(request.durationMinutes?.toString() ?: "") }
    var note by remember(request.note) { mutableStateOf(request.note ?: "") }
    var habitMenuExpanded by remember { mutableStateOf(false) }

    val durationMinutes = duration.toIntOrNull()
    val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull()
    val dateValid = parsedDate != null && !parsedDate.isAfter(LocalDate.now())
    val saveEnabled = selectedHabitId != null && dateValid && (durationMinutes == null || durationMinutes >= 0)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(if (request.habitId == null) "补录打卡" else "打卡记录", style = MaterialTheme.typography.titleLarge)
            if (request.habitId == null) {
                Box {
                    OutlinedButton(onClick = { habitMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            selectedHabit?.title ?: "选择习惯",
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = habitMenuExpanded, onDismissRequest = { habitMenuExpanded = false }) {
                        habits.forEach { h ->
                            DropdownMenuItem(
                                text = { Text(h.title) },
                                onClick = { selectedHabitId = h.id; habitMenuExpanded = false }
                            )
                        }
                    }
                }
                if (habits.isEmpty()) {
                    Text("暂无习惯可补录，请先新建习惯", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                }
            }
            OutlinedTextField(
                value = duration,
                onValueChange = { duration = it.filter { c -> c.isDigit() } },
                label = { Text("时长（分钟，可留空）") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("日期（yyyy-MM-dd）") },
                singleLine = true,
                isError = !dateValid,
                supportingText = { if (!dateValid) Text("日期无效或为未来日期") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { date = todayString() }) { Text("今天") }
                TextButton(onClick = { date = LocalDate.now().minusDays(1).toString() }) { Text("昨天") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) {
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("删除记录")
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        selectedHabitId?.let {
                            onSave(it, date, durationMinutes, note.ifBlank { null }, null)
                        }
                    },
                    enabled = saveEnabled
                ) {
                    Text("保存")
                }
            }
        }
    }
}
