package com.aigrowth.os.feature.simple

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import com.accounting.app.log.AppLogger
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreen(onOpenDrawer: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { SimpleDataStore(context) }
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }
    val records by store.items("fitness").collectAsState(emptyList())
    val checked = records.any { it.id == today.toString() && it.done }
    val streak = remember(records, today) {
        val dates = records.filter { it.done }.mapNotNull { runCatching { LocalDate.parse(it.id) }.getOrNull() }.toSet()
        var cursor = if (today in dates) today else today.minusDays(1)
        var count = 0
        while (cursor in dates) {
            count++
            cursor = cursor.minusDays(1)
        }
        count
    }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("健身打卡") },
            navigationIcon = {
                IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, "打开导航") }
            },
        )
        Column(Modifier.padding(24.dp)) {
            Text(if (checked) "今日已打卡" else "今天还没有打卡", style = MaterialTheme.typography.headlineSmall)
            Text("连续打卡：$streak 天", Modifier.padding(vertical = 16.dp))
            Button(onClick = {
                scope.launch {
                    val requestId = AppLogger.generateRequestId()
                    val updated = if (checked) {
                        records.filterNot { it.id == today.toString() }
                    } else {
                        records + SimpleItem(today.toString(), today.toString(), true)
                    }
                    store.save("fitness", updated, "CheckInScreen", requestId)
                }
            }) { Text(if (checked) "取消今日打卡" else "完成今日打卡") }
        }
    }
}
