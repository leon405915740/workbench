package com.aigrowth.os.feature.plan

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aigrowth.os.core.database.workbench.entity.PlanItem
import com.aigrowth.os.ui.common.*
import com.aigrowth.os.util.WorkbenchImageStore
import java.time.LocalDate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(vm: PlanViewModel = hiltViewModel()) {
    val items by vm.items.collectAsState()
    val search by vm.search.collectAsState()
    val stats by vm.stats.collectAsState()
    var editorTarget by remember { mutableStateOf<PlanItem?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<PlanItem?>(null) }
    val context = LocalContext.current

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            WorkbenchTopBar("今日计划", "安排优先级，聚焦最重要的事")
            PlanStatsRow(stats)
            SearchField(value = search, onChange = vm::setSearch, placeholder = "搜索计划或备注")
            Spacer(Modifier.height(8.dp))
            Box(Modifier.weight(1f)) {
                if (items.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.Checklist,
                        title = if (search.isBlank()) "还没有计划" else "没有匹配的计划",
                        message = if (search.isBlank()) "点右下角 + 添加今日第一条计划" else "换个关键词试试"
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(items, key = { it.id }) { item ->
                            PlanCard(
                                item = item,
                                onToggleDone = { vm.toggleDone(item) },
                                onTogglePinned = { vm.togglePinned(item) },
                                onEdit = { editorTarget = item; showEditor = true },
                                onDelete = { pendingDelete = item }
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
            Icon(Icons.Default.Add, contentDescription = "新增计划")
        }

        if (showEditor) {
            PlanEditorSheet(
                initial = editorTarget,
                onDismiss = { showEditor = false },
                onSave = { title, priority, note, date, imageUri ->
                    val target = editorTarget
                    if (target == null) {
                        if (title.isNotBlank()) vm.add(title, priority, note, date, imageUri)
                    } else {
                        vm.update(target.copy(title = title, priority = priority, note = note, planDate = date, imageUri = imageUri))
                    }
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
private fun PlanStatsRow(stats: PlanStats) {
    WorkbenchCard(modifier = Modifier.padding(horizontal = 16.dp), contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("今日完成进度", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${stats.done}/${stats.total} 已完成",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.width(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PriorityBadge("P0 ${stats.p0}")
                PriorityBadge("P1 ${stats.p1}")
                PriorityBadge("P2 ${stats.p2}")
            }
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
private fun PlanCard(
    item: PlanItem,
    onToggleDone: () -> Unit,
    onTogglePinned: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    WorkbenchCard(contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Checkbox(
                checked = item.done,
                onCheckedChange = { onToggleDone() },
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (item.done) TextDecoration.LineThrough else null,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.pinned) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "已置顶",
                            tint = Color(0xFF397565),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (item.note.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(item.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    PriorityBadge(item.priority)
                    Text(formatDate(item.planDate), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (item.imageUri != null) {
                    Spacer(Modifier.height(8.dp))
                    WorkbenchImage(item.imageUri, Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(10.dp)))
                }
            }
            Spacer(Modifier.width(4.dp))
            Column {
                IconButton(onClick = onTogglePinned) {
                    Icon(
                        if (item.pinned) Icons.Default.PushPin else Icons.Default.OutlinedFlag,
                        contentDescription = "置顶",
                        tint = if (item.pinned) Color(0xFF397565) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑", modifier = Modifier.size(18.dp))
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
private fun PlanEditorSheet(
    initial: PlanItem?,
    onDismiss: () -> Unit,
    onSave: (title: String, priority: String, note: String, date: String, imageUri: String?) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var priority by remember { mutableStateOf(initial?.priority ?: "P1") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var date by remember { mutableStateOf(initial?.planDate ?: LocalDate.now().toString()) }
    val attachment = rememberImageAttachment(initial?.imageUri)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(if (initial == null) "新增计划" else "编辑计划", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text("优先级", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("P0", "P1", "P2").forEach { p ->
                    FilterChip(
                        selected = priority == p,
                        onClick = { priority = p },
                        label = { Text(p) }
                    )
                }
            }
            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("计划日期 (yyyy-MM-dd)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注") },
                modifier = Modifier.fillMaxWidth()
            )
            WorkbenchImagePicker(
                current = attachment.draft,
                onPick = attachment::onPick,
                onRemove = attachment::onRemove
            )
            Button(
                onClick = { onSave(title, priority, note, date.ifBlank { LocalDate.now().toString() }, attachment.resolve()) },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存")
            }
        }
    }
}