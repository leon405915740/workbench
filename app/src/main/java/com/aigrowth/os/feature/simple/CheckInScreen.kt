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
import com.aigrowth.os.ui.common.WorkbenchCard
import com.aigrowth.os.ui.common.WorkbenchPage
import com.aigrowth.os.ui.common.WorkbenchTopBar
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape

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
    WorkbenchPage(Modifier.fillMaxSize()) {
        WorkbenchTopBar("健身打卡", onOpenDrawer, "每天一点点，照顾好自己的身体")
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            WorkbenchCard(color = Color(0xFFE4F1EC)) {
                Text("晚上好，今天也辛苦了 👋", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(if (checked) "今日任务已完成，继续保持这份节奏" else "完成一次打卡，为今天留下一个小小的成就", modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            WorkbenchCard(color = Color(0xFFF2F0E8)) {
                Text("今日进度", style = MaterialTheme.typography.titleLarge)
                Text(if (checked) "1 / 1" else "0 / 1", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
                Text("连续打卡 $streak 天", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
            }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), contentPadding = PaddingValues(vertical = 14.dp)) { Text(if (checked) "取消今日打卡" else "完成今日打卡") }
        }
    }
}
