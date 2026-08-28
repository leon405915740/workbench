package com.aigrowth.os.feature.statustrend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aigrowth.os.core.database.workbench.entity.StatusTrendEntry
import com.aigrowth.os.ui.common.WorkbenchCard
import com.aigrowth.os.ui.common.formatDate
import com.aigrowth.os.ui.common.todayString
import java.time.LocalDate

/**
 * 七天状态趋势展示 + 记录入口。首页与洞察页共用。
 */
@Composable
fun StatusTrendSection(
    entries: List<StatusTrendEntry>,
    onRecord: () -> Unit
) {
    val today = LocalDate.now()
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val byDate = remember(entries) { entries.associateBy { it.date } }
    val recorded = entries.size
    val avg = if (recorded == 0) null else entries.map { it.score }.average().toInt()

    WorkbenchCard(contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("七天状态", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Text(
                    when {
                        recorded == 0 -> "还没有状态记录"
                        avg == null -> "记录 $recorded 次"
                        else -> "平均状态 $avg 分 · 共 $recorded 次"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onRecord) { Text("记录今天") }
        }
        Spacer(Modifier.height(16.dp))

        if (recorded == 0) {
            Text(
                "记录每天的状态，一周后就能看到自己的趋势",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().height(96.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                days.forEach { day ->
                    val entry = byDate[day.toString()]
                    val score = entry?.score ?: 0
                    TrendBar(
                        score = score,
                        dayLabel = day.dayOfMonth.toString(),
                        hasEntry = entry != null
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendBar(score: Int, dayLabel: String, hasEntry: Boolean) {
    val accent = Color(0xFF397565)
    val emptyColor = Color(0xFFE3E9E6)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.height(96.dp)
    ) {
        Text(
            if (hasEntry) score.toString() else "–",
            style = MaterialTheme.typography.labelSmall,
            color = if (hasEntry) accent else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(18.dp)
                .height((if (hasEntry) 56f * score / 100f else 5f).dp.coerceAtLeast(5.dp))
                .background(if (hasEntry) accent else emptyColor, RoundedCornerShape(6.dp))
        )
        Spacer(Modifier.height(4.dp))
        Text(dayLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusRecordSheet(
    initialScore: Int?,
    initialNote: String?,
    onSave: (score: Int, note: String) -> Unit,
    onDismiss: () -> Unit
) {
    var score by remember { mutableStateOf(initialScore ?: 70) }
    var note by remember { mutableStateOf(initialNote ?: "") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("记录今天的状态", style = MaterialTheme.typography.titleLarge)
            Text(
                "给自己的状态打个分（0–100）：$score",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = score.toFloat(),
                onValueChange = { score = it.toInt() },
                valueRange = 0f..100f,
                steps = 19
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("一句话记录（可选）") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onSave(score, note) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存")
            }
        }
    }
}