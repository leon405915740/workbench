package com.aigrowth.os.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aigrowth.os.core.database.workbench.entity.Habit
import java.time.LocalDate

/**
 * 本周打卡追踪表（Mon–Sun）。主区单元格可点击（过去与今天可补打/取消，未来禁用）。
 * 首页与习惯打卡页共用。
 */
@Composable
fun WeeklyHabitTracker(
    activeHabits: List<Habit>,
    week: List<LocalDate>,
    today: String,
    checkedKeys: Set<String>,
    onToggle: (habitId: String, date: String) -> Unit
) {
    val weekLabels = listOf("一", "二", "三", "四", "五", "六", "日")
    WorkbenchCard(contentPadding = PaddingValues(16.dp)) {
        Text("本周打卡", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(12.dp))
        Column(Modifier.horizontalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(76.dp))
                week.forEachIndexed { i, day ->
                    val isToday = day.toString() == today
                    Box(Modifier.width(30.dp), contentAlignment = Alignment.Center) {
                        Text(
                            weekLabels[i],
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isToday) Color(0xFF397565) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            activeHabits.forEach { habit ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        habit.title,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(76.dp)
                    )
                    week.forEach { day ->
                        val date = day.toString()
                        val checked = "${habit.id}|$date" in checkedKeys
                        val enabled = date <= today
                        WeekTrackerCell(
                            checked = checked,
                            enabled = enabled,
                            isToday = date == today,
                            onClick = { onToggle(habit.id, date) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekTrackerCell(checked: Boolean, enabled: Boolean, isToday: Boolean, onClick: () -> Unit) {
    val accent = Color(0xFF397565)
    Box(
        modifier = Modifier
            .width(30.dp)
            .height(30.dp)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Box(
                modifier = Modifier.size(20.dp).background(accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
        } else {
            val ringColor = when {
                isToday -> accent
                enabled -> Color(0xFFB9C6C0)
                else -> Color(0xFFE3E9E6)
            }
            Box(Modifier.size(20.dp).border(1.dp, ringColor, CircleShape))
        }
    }
}