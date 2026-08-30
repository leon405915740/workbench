package com.aigrowth.os.feature.reading

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aigrowth.os.core.database.workbench.entity.ReadingItem
import com.aigrowth.os.ui.common.*
import com.aigrowth.os.ui.theme.*
import com.aigrowth.os.util.WorkbenchImageStore
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingScreen(vm: ReadingViewModel = hiltViewModel()) {
    val items by vm.items.collectAsState()
    val archived by vm.archived.collectAsState()
    val search by vm.search.collectAsState()
    val stats by vm.stats.collectAsState()
    val readingToday by vm.readingToday.collectAsState()
    var editorTarget by remember { mutableStateOf<ReadingItem?>(null) }
    var archivedExpanded by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<ReadingItem?>(null) }
    val context = LocalContext.current

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            WorkbenchTopBar(
                title = "阅读",
                subtitle = "每天读一点，积少成多",
                icon = Icons.Default.AutoStories,
                iconTint = ModuleBlue
            )
            ReadingHeader(stats = stats, todayAmount = readingToday, search = search, onChange = vm::setSearch)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.weight(1f)) {
                if (items.isEmpty() && archived.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.MenuBook,
                        title = if (search.isBlank()) "还没有阅读记录" else "没有匹配的结果",
                        message = if (search.isBlank()) "点右下角 + 添加在读书籍" else "换个关键词试试"
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items, key = { "reading|${it.id}" }) { item ->
                            ReadingCard(
                                item = item,
                                onIncrement = { vm.increment(item, 10f) },
                                onTogglePinned = { vm.togglePinned(item) },
                                onEdit = { editorTarget = item; showEditor = true },
                                onDelete = { pendingDelete = item }
                            )
                        }
                        if (archived.isNotEmpty()) {
                            item(key = "archived_header") {
                                ReadingArchivedHeader(
                                    count = archived.size,
                                    expanded = archivedExpanded,
                                    onToggle = { archivedExpanded = !archivedExpanded }
                                )
                            }
                            if (archivedExpanded) {
                                items(archived, key = { "arch|${it.id}" }) { item ->
                                    ReadingArchivedCard(item = item)
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { editorTarget = null; showEditor = true },
            modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "新增阅读")
        }

        if (showEditor) {
            ReadingEditorSheet(
                initial = editorTarget,
                onDismiss = { showEditor = false },
                onSave = { title, current, target, unit, note, date, imageUri ->
                    val t = editorTarget
                    if (t == null) vm.add(title, current, target, unit, note, date, imageUri)
                    else vm.update(t.copy(title = title, current = current, target = target, unit = unit, note = note, date = date, imageUri = imageUri))
                    showEditor = false
                }
            )
        }

        pendingDelete?.let { target ->
            ConfirmDeleteDialog(
                message = "确定删除「${target.title}」吗？此操作无法撤销。",
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
private fun ReadingHeader(stats: ReadingStats, todayAmount: Float, search: String, onChange: (String) -> Unit) {
    WorkbenchCard(
        modifier = Modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 14.dp, horizontal = 0.dp)
    ) {
        SearchField(value = search, onChange = onChange, placeholder = "搜索书名或备注")
        Spacer(Modifier.height(12.dp))
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text("阅读概览", style = MaterialTheme.typography.labelMedium, color = InkSecondary)
            Text(
                "${stats.total} 本在读 · ${stats.completed} 本已完成 · 累计 ${formatProgress(stats.currentSum)} · 今日阅读 ${formatProgress(todayAmount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ReadingArchivedHeader(count: Int, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 4.dp, vertical = 10.dp)
    ) {
        Text(
            "已完成归档",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = InkText
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "($count)",
            style = MaterialTheme.typography.labelSmall,
            color = InkSecondary
        )
        Spacer(Modifier.weight(1f))
        Icon(
            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (expanded) "收起归档" else "展开归档",
            tint = InkTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ReadingArchivedCard(item: ReadingItem) {
    WorkbenchCard(contentPadding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ModuleIconBadge(icon = Icons.Default.AutoStories, tint = ModuleBlue)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "已完成 · ${formatProgress(item.current)} ${item.unit}",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSecondary
                )
            }
            Icon(Icons.Default.CheckCircle, contentDescription = "已完成", tint = ModuleBlue, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ReadingCard(
    item: ReadingItem,
    onIncrement: () -> Unit,
    onTogglePinned: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = if (item.target <= 0f) 0f else (item.current / item.target).coerceIn(0f, 1f)
    val completed = item.target > 0f && item.current >= item.target
    WorkbenchCard(contentPadding = PaddingValues(12.dp)) {
        Column {
            Row(verticalAlignment = Alignment.Top) {
                ModuleIconBadge(icon = Icons.Default.AutoStories, tint = ModuleBlue)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = ModuleBlue,
                        trackColor = AccentGreenSoft
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${formatProgress(item.current)} / ${formatProgress(item.target)} ${item.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                    if (item.note.isNotBlank()) {
                        Text(item.note, style = MaterialTheme.typography.labelSmall, color = InkSecondary, maxLines = 1)
                    }
                    Text(formatDate(item.date), style = MaterialTheme.typography.labelSmall, color = InkSecondary)
                    if (item.imageUri != null) {
                        Spacer(Modifier.height(8.dp))
                        WorkbenchImage(item.imageUri, Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(10.dp)))
                    }
                }
                Spacer(Modifier.width(6.dp))
                ToggleCircle(selected = completed, color = ModuleBlue, onClick = onIncrement, unselectedIcon = Icons.Default.Add)
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                PinAction(
                    pinned = item.pinned,
                    tint = if (item.pinned) ModuleBlue else InkSecondary,
                    onClick = onTogglePinned
                )
                MiniAction(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑", tint = InkSecondary, modifier = Modifier.size(16.dp))
                }
                MiniAction(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = DangerInk, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ModuleIconBadge(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier.size(44.dp).background(tint.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun ToggleCircle(selected: Boolean, color: Color, onClick: () -> Unit, unselectedIcon: ImageVector? = null) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .then(
                if (selected) Modifier.background(color)
                else Modifier.border(2.dp, color, CircleShape)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = "已读完", tint = Color.White, modifier = Modifier.size(20.dp))
        } else {
            unselectedIcon?.let { Icon(it, contentDescription = null, tint = color, modifier = Modifier.size(16.dp)) }
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadingEditorSheet(
    initial: ReadingItem?,
    onDismiss: () -> Unit,
    onSave: (title: String, current: Float, target: Float, unit: String, note: String, date: String, imageUri: String?) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var currentText by remember { mutableStateOf(initial?.let { formatProgress(it.current) } ?: "") }
    var targetText by remember { mutableStateOf(initial?.let { formatProgress(it.target) } ?: "") }
    var unit by remember { mutableStateOf(initial?.unit ?: "页") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var date by remember { mutableStateOf(initial?.date ?: LocalDate.now().toString()) }
    val attachment = rememberImageAttachment(initial?.imageUri)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(if (initial == null) "新增阅读" else "编辑阅读", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("书名/标题") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = currentText, onValueChange = { currentText = it }, label = { Text("当前值") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = targetText, onValueChange = { targetText = it }, label = { Text("目标值") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("单位（页/章/自定义）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("日期 (yyyy-MM-dd)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth())
            WorkbenchImagePicker(
                current = attachment.draft,
                onPick = attachment::onPick,
                onRemove = attachment::onRemove
            )
            Button(
                onClick = {
                    onSave(
                        title,
                        currentText.toFloatOrNull() ?: 0f,
                        targetText.toFloatOrNull() ?: 0f,
                        unit,
                        note,
                        date.ifBlank { LocalDate.now().toString() },
                        attachment.resolve()
                    )
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("保存") }
        }
    }
}
