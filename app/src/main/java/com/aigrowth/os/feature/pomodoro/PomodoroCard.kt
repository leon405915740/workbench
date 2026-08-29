package com.aigrowth.os.feature.pomodoro

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aigrowth.os.ui.common.WorkbenchCard
import kotlinx.coroutines.flow.StateFlow

@Composable
fun PomodoroCard(
    uiFlow: StateFlow<PomodoroUi>,
    onStartFocus: () -> Unit,
    onResume: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit
) {
    val ui by uiFlow.collectAsState()
    val phaseLabel = when (ui.phase) {
        PomodoroPhase.BREAK -> "休息"
        else -> "专注"
    }

    WorkbenchCard(contentPadding = PaddingValues(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("番茄钟", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Text(
                    "完成 ${ui.focusCount} 次 · 累计 ${ui.totalFocusMinutes} 分钟",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                phaseLabel,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF397565),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            formatClock(ui.remainSeconds),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E2A26)
        )
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { ui.progress },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = Color(0xFF397565),
            trackColor = Color(0xFFDDE7E2)
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            when {
                ui.running -> {
                    Button(onClick = onPause, modifier = Modifier.weight(1f)) { Text("暂停") }
                }
                ui.remainSeconds < ui.totalSeconds -> {
                    Button(onClick = onResume, modifier = Modifier.weight(1f)) { Text("继续") }
                }
                else -> {
                    Button(onClick = onStartFocus, modifier = Modifier.weight(1f)) {
                        Text(if (ui.phase == PomodoroPhase.BREAK) "开始休息" else "开始专注")
                    }
                }
            }
            OutlinedButton(onClick = onReset) { Text("重置") }
        }
    }
}

private fun formatClock(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}