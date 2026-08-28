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

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = {
                IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, "打开导航") }
            },
        )
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(input, { input = it }, Modifier.weight(1f), label = { Text("新增事项") })
            Button(onClick = {
                if (input.isNotBlank()) {
                    val id = editing ?: store.newId()
                    saveItems(items.filterNot { it.id == id } + SimpleItem(id, input, items.firstOrNull { it.id == id }?.done == true))
                    input = ""
                    editing = null
                }
            }) { Text(if (editing == null) "添加" else "保存") }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(items, key = { it.id }) { item ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(item.done, { saveItems(items.map { if (it.id == item.id) it.copy(done = !it.done) else it }) })
                    Text(item.text, Modifier.weight(1f).padding(top = 12.dp))
                    TextButton(onClick = { input = item.text; editing = item.id }) { Text("编辑") }
                    TextButton(onClick = { saveItems(items.filterNot { it.id == item.id }) }) { Text("删除") }
                }
            }
        }
    }
}
