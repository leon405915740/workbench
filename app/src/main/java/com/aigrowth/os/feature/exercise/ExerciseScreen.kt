package com.aigrowth.os.feature.exercise

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aigrowth.os.core.database.workbench.entity.ExerciseItem
import com.aigrowth.os.ui.common.*
import com.aigrowth.os.util.WorkbenchImageStore
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseScreen(vm: ExerciseViewModel = hiltViewModel()) {
    val items by vm.items.collectAsState()
    val search by vm.search.collectAsState()
    val stats by vm.stats.collectAsState()
    var editorTarget by remember { mutableStateOf<ExerciseItem?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<ExerciseItem?>(null) }
    val context = LocalContext.current

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            WorkbenchTopBar("运动", "动起来，保持好状态")
            ExerciseStatsRow(stats)
            SearchField(value = search, onChange = vm::setSearch, placeholder = "搜索运动项目或备注")
            Spacer(Modifier.height(8.dp))
            Box(Modifier.weight(1f)) {
                if (items.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.DirectionsRun,
                        title = if (search.isBlank()) "还没有运动记录" else "没有匹配的结果",
                        message = if (search.isBlank()) "点右下角 + 添加运动项目" else "换个关键词试试"
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(items, key = { it.id }) { item ->
                            ExerciseCard(
                                item = item,
                                onIncrement = { vm.increment(item, 5f) },
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
            Icon(Icons.Default.Add, contentDescription = "新增运动")
        }

        if (showEditor) {
            ExerciseEditorSheet(
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
private fun ExerciseStatsRow(stats: ExerciseStats) {
    WorkbenchCard(modifier = Modifier.padding(horizontal = 16.dp), contentPadding = PaddingValues(16.dp)) {
        Column {
            Text("运动概览", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "${stats.total} 个项目 · ${stats.completed} 个已达标 · 累计 ${formatProgress(stats.currentSum)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ExerciseCard(
    item: ExerciseItem,
    onIncrement: () -> Unit,
    onTogglePinned: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = if (item.target <= 0f) 0f else (item.current / item.target).coerceIn(0f, 1f)
    WorkbenchCard(contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f, fill = false))
                    if (item.pinned) Icon(Icons.Default.PushPin, null, tint = Color(0xFF397565), modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = Color(0xFF397565),
                    trackColor = Color(0xFFDDE7E2)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "${formatProgress(item.current)} / ${formatProgress(item.target)} ${item.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.note.isNotBlank()) {
                    Text(item.note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                Text(formatDate(item.date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (item.imageUri != null) {
                    Spacer(Modifier.height(8.dp))
                    WorkbenchImage(item.imageUri, Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(10.dp)))
                }
            }
            Spacer(Modifier.width(4.dp))
            Column {
                IconButton(onClick = onIncrement) {
                    Icon(Icons.Default.Add, contentDescription = "增加进度", tint = Color(0xFF397565), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onTogglePinned) {
                    Icon(Icons.Default.PushPin, contentDescription = "置顶", tint = if (item.pinned) Color(0xFF397565) else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
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
private fun ExerciseEditorSheet(
    initial: ExerciseItem?,
    onDismiss: () -> Unit,
    onSave: (title: String, current: Float, target: Float, unit: String, note: String, date: String, imageUri: String?) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var currentText by remember { mutableStateOf(initial?.let { formatProgress(it.current) } ?: "") }
    var targetText by remember { mutableStateOf(initial?.let { formatProgress(it.target) } ?: "") }
    var unit by remember { mutableStateOf(initial?.unit ?: "分钟") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var date by remember { mutableStateOf(initial?.date ?: LocalDate.now().toString()) }
    val attachment = rememberImageAttachment(initial?.imageUri)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(if (initial == null) "新增运动" else "编辑运动", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("项目名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
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
            OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("单位（分钟/次/距离）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
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