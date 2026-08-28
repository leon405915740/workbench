package com.aigrowth.os.feature.habit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aigrowth.os.core.database.workbench.entity.Habit
import com.aigrowth.os.ui.common.*
import com.aigrowth.os.util.WorkbenchImageStore
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitScreen(vm: HabitViewModel = hiltViewModel()) {
    val habits by vm.habits.collectAsState()
    val activeHabits by vm.activeHabits.collectAsState()
    val search by vm.search.collectAsState()
    val stats by vm.stats.collectAsState()
    val logs by vm.logs.collectAsState()

    val checkedKeys = remember(logs) { logs.mapTo(hashSetOf()) { "${it.habitId}|${it.date}" } }
    val today = todayString()
    val week = remember {
        val now = LocalDate.now()
        val monday = now.minusDays((now.dayOfWeek.value - 1).toLong())
        (0 until 7).map { monday.plusDays(it.toLong()) }
    }

    var editorTarget by remember { mutableStateOf<Habit?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Habit?>(null) }
    val context = LocalContext.current

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            WorkbenchTopBar("习惯打卡", "坚持的力量来自每天一点点")
            HabitStatsRow(stats)
            SearchField(value = search, onChange = vm::setSearch, placeholder = "搜索习惯")
            Spacer(Modifier.height(8.dp))
            Box(Modifier.weight(1f)) {
                if (habits.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.CheckCircle,
                        title = if (search.isBlank()) "还没有习惯" else "没有匹配的习惯",
                        message = if (search.isBlank()) "点右下角 + 新建第一个习惯" else "换个关键词试试"
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (activeHabits.isNotEmpty()) {
                            item(key = "weekly") {
                                WeeklyHabitTracker(
                                    activeHabits = activeHabits,
                                    week = week,
                                    today = today,
                                    checkedKeys = checkedKeys,
                                    onToggle = vm::toggleCheck
                                )
                                Spacer(Modifier.height(2.dp))
                            }
                        }
                        items(habits, key = { it.habit.id }) { ui ->
                            HabitCard(
                                ui = ui,
                                onToggleToday = { vm.toggleCheck(ui.habit.id, today) },
                                onToggleActive = { vm.setActive(ui.habit, !ui.habit.active) },
                                onTogglePinned = { vm.togglePinned(ui.habit) },
                                onEdit = { editorTarget = ui.habit; showEditor = true },
                                onDelete = { pendingDelete = ui.habit }
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { editorTarget = null; showEditor = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "新建习惯")
        }

        if (showEditor) {
            HabitEditorSheet(
                initial = editorTarget,
                onDismiss = { showEditor = false },
                onSave = { title, imageUri ->
                    val target = editorTarget
                    if (target == null) {
                        if (title.isNotBlank()) vm.add(title, imageUri)
                    } else {
                        vm.edit(target, title, imageUri)
                    }
                    showEditor = false
                }
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
    }
}

@Composable
private fun HabitStatsRow(stats: HabitStats) {
    WorkbenchCard(modifier = Modifier.padding(horizontal = 16.dp), contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("今日打卡", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${stats.doneToday}/${stats.active} 已完成",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                "活跃习惯 ${stats.active} 个",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { stats.progress },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = Color(0xFF397565),
            trackColor = Color(0xFFDDE7E2)
        )
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
    WorkbenchCard(contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            if (habit.active) {
                Checkbox(
                    checked = ui.checkedToday,
                    onCheckedChange = { onToggleToday() },
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Spacer(Modifier.width(24.dp))
            }
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        habit.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (habit.active) null else TextDecoration.LineThrough,
                        color = if (habit.active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (habit.pinned) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "已置顶",
                            tint = Color(0xFF397565),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("连续 ${ui.streak} 天", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("累计 ${ui.total} 天", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!habit.active) TagChip("已停用")
                }
                if (habit.imageUri != null) {
                    Spacer(Modifier.height(8.dp))
                    WorkbenchImage(habit.imageUri, Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(10.dp)))
                }
            }
            Spacer(Modifier.width(4.dp))
            Column {
                IconButton(onClick = onTogglePinned) {
                    Icon(
                        if (habit.pinned) Icons.Default.PushPin else Icons.Default.OutlinedFlag,
                        contentDescription = "置顶",
                        tint = if (habit.pinned) Color(0xFF397565) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onToggleActive) {
                    Icon(
                        if (habit.active) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (habit.active) "停用" else "启用",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HabitEditorSheet(
    initial: Habit?,
    onDismiss: () -> Unit,
    onSave: (title: String, imageUri: String?) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
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
                onClick = { onSave(title, attachment.resolve()) },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存")
            }
        }
    }
}