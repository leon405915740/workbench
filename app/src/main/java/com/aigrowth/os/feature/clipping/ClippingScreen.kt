package com.aigrowth.os.feature.clipping

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aigrowth.os.core.database.workbench.entity.Clipping
import com.aigrowth.os.ui.common.*
import com.aigrowth.os.ui.theme.*
import com.aigrowth.os.util.WorkbenchImageStore
import java.time.LocalDate

private val STATUSES = listOf("收藏", "稍后读", "已读")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClippingScreen(vm: ClippingViewModel = hiltViewModel()) {
    val items by vm.items.collectAsState()
    val search by vm.search.collectAsState()
    var editorTarget by remember { mutableStateOf<Clipping?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Clipping?>(null) }
    val context = LocalContext.current

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            WorkbenchTopBar(
                title = "剪报",
                subtitle = "收藏值得留下来的内容",
                icon = Icons.Default.Newspaper,
                iconTint = ModuleClay
            )
            ClippingHeader(search = search, onChange = vm::setSearch)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.weight(1f)) {
                if (items.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.Article,
                        title = if (search.isBlank()) "还没有剪报" else "没有匹配的结果",
                        message = if (search.isBlank()) "点右下角 + 收藏第一条内容" else "换个关键词试试"
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items, key = { it.id }) { item ->
                            ClippingCard(
                                item = item,
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
            modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "新增剪报")
        }

        if (showEditor) {
            ClippingEditorSheet(
                initial = editorTarget,
                onDismiss = { showEditor = false },
                onSave = { title, content, status, source, tags, layout, date, imageUri ->
                    val t = editorTarget
                    if (t == null) vm.add(title, content, status, source, tags, layout, date, imageUri)
                    else vm.update(t.copy(title = title, content = content, status = status, source = source, tags = tags, layout = layout, date = date, imageUri = imageUri))
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
private fun ClippingHeader(search: String, onChange: (String) -> Unit) {
    WorkbenchCard(
        modifier = Modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 0.dp)
    ) {
        SearchField(value = search, onChange = onChange, placeholder = "搜索标题、正文或标签")
    }
}

@Composable
private fun ClippingCard(
    item: Clipping,
    onTogglePinned: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val tags = item.tags.split(",").filter { it.isNotBlank() }
    WorkbenchCard(contentPadding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            ModuleIconBadge(icon = Icons.Default.Newspaper, tint = ModuleClay)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                if (item.layout == "quote") {
                    Column(Modifier.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(6.dp))
                        Text(item.content, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = InkSecondary)
                    }
                } else {
                    Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    if (item.content.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(item.content, style = MaterialTheme.typography.bodySmall, color = InkSecondary, maxLines = 3)
                    }
                }
            }
            Spacer(Modifier.width(6.dp))
            ToggleCircle(selected = item.pinned, color = ModuleClay, onClick = onTogglePinned)
        }
        if (item.imageUri != null) {
            Spacer(Modifier.height(8.dp))
            WorkbenchImage(item.imageUri, Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(10.dp)))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            item.status?.let { TagChip(it) }
            item.source?.let { TagChip(it) }
            tags.forEach { TagChip(it) }
            Text(formatDate(item.date), style = MaterialTheme.typography.labelSmall, color = InkSecondary)
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            PinAction(
                    pinned = item.pinned,
                    tint = if (item.pinned) ModuleClay else InkSecondary,
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
            Icon(Icons.Default.Check, contentDescription = "已置顶", tint = Color.White, modifier = Modifier.size(20.dp))
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
private fun ClippingEditorSheet(
    initial: Clipping?,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, status: String?, source: String?, tags: String, layout: String, date: String, imageUri: String?) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var content by remember { mutableStateOf(initial?.content ?: "") }
    var status by remember { mutableStateOf(initial?.status ?: STATUSES.first()) }
    var source by remember { mutableStateOf(initial?.source ?: "") }
    var layout by remember { mutableStateOf(initial?.layout ?: "default") }
    var tags by remember { mutableStateOf(initial?.tags ?: "") }
    var date by remember { mutableStateOf(initial?.date ?: LocalDate.now().toString()) }
    val attachment = rememberImageAttachment(initial?.imageUri)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(if (initial == null) "新增剪报" else "编辑剪报", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("标题") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("正文") }, minLines = 3, modifier = Modifier.fillMaxWidth())

            Text("状态", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                STATUSES.forEach { s ->
                    FilterChip(selected = status == s, onClick = { status = s }, label = { Text(s) })
                }
            }

            Text("卡片布局", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = layout == "default", onClick = { layout = "default" }, label = { Text("标准") })
                FilterChip(selected = layout == "quote", onClick = { layout = "quote" }, label = { Text("引文") })
                FilterChip(selected = layout == "feature", onClick = { layout = "feature" }, label = { Text("大图") })
            }

            OutlinedTextField(value = source, onValueChange = { source = it }, label = { Text("来源") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = tags, onValueChange = { tags = it }, label = { Text("标签（逗号分隔）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("日期 (yyyy-MM-dd)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            WorkbenchImagePicker(
                current = attachment.draft,
                onPick = attachment::onPick,
                onRemove = attachment::onRemove
            )
            Button(
                onClick = {
                    onSave(title, content, status, source, tags, layout, date.ifBlank { LocalDate.now().toString() }, attachment.resolve())
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("保存") }
        }
    }
}
