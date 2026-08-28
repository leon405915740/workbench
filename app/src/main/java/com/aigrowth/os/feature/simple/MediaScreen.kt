package com.aigrowth.os.feature.simple

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.accounting.app.log.AppLogger
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.aigrowth.os.ui.common.WorkbenchCard
import com.aigrowth.os.ui.common.WorkbenchPage
import com.aigrowth.os.ui.common.WorkbenchTopBar
import com.aigrowth.os.core.design.MorandiEmptyState
import androidx.compose.material.icons.filled.Inbox

@Composable
fun MediaScreen(onOpenDrawer: () -> Unit) = SimpleListScreen("自媒体", "media", onOpenDrawer)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SimpleListScreen(title: String, kind: String, onOpenDrawer: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { SimpleDataStore(context) }
    val scope = rememberCoroutineScope()
    val items by store.items(kind).collectAsState(emptyList())
    var input by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<String?>(null) }

    fun saveItems(updated: List<SimpleItem>) {
        scope.launch {
            val requestId = AppLogger.generateRequestId()
            store.save(kind, updated, title, requestId)
        }
    }

    WorkbenchPage(Modifier.fillMaxSize()) {
        WorkbenchTopBar(title, onOpenDrawer, if (kind == "media") "灵感与创作，逐项完成" else "每天积累一点语言输入")
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            WorkbenchCard(color = Color(0xFFE7F0F2)) { Text("今天也向前一步", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("把想做的事写下来，完成后轻轻勾选。", modifier = Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(input, { input = it }, Modifier.weight(1f), placeholder = { Text("输入一件要完成的事") }, shape = RoundedCornerShape(20.dp), singleLine = true)
            Button(onClick = {
                if (input.isNotBlank()) {
                    val id = editing ?: store.newId()
                    saveItems(items.filterNot { it.id == id } + SimpleItem(id, input, items.firstOrNull { it.id == id }?.done == true))
                    input = ""
                    editing = null
                }
            }, shape = RoundedCornerShape(18.dp)) { Text(if (editing == null) "添加" else "保存") }
            }
        }
        if (items.isEmpty()) MorandiEmptyState(Icons.Default.Inbox, "还没有事项", "在上方输入第一件小事吧", Modifier.fillMaxSize())
        else LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items, key = { it.id }) { item ->
                WorkbenchCard(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), color = if (item.done) Color(0xFFF0F4F1) else MaterialTheme.colorScheme.surface) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(item.done, { saveItems(items.map { if (it.id == item.id) it.copy(done = !it.done) else it }) })
                    Text(item.text, Modifier.weight(1f).padding(top = 12.dp))
                    TextButton(onClick = { input = item.text; editing = item.id }) { Text("编辑") }
                    TextButton(onClick = { saveItems(items.filterNot { it.id == item.id }) }) { Text("删除") }
                }
                }
            }
        }
    }
}
