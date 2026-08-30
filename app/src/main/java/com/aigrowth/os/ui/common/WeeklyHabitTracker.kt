package com.aigrowth.os.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aigrowth.os.core.database.workbench.entity.ExerciseCategoryEnum
import com.aigrowth.os.core.database.workbench.entity.Habit
import com.aigrowth.os.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ponytail: 固定标签列宽 + 7 个均分 weight 列，窄屏（约 320dp）下方块随列自动收缩，无需横向滚动。
private val labelColumnWidth = 84.dp
private val cellSize = 26.dp

/** 习惯类别 -> 模块色：heatmap 方块、习惯卡片图标底共用。 */
fun habitCategoryColor(category: ExerciseCategoryEnum?): Color = when (category) {
    ExerciseCategoryEnum.CARDIO -> ModuleOchre
    ExerciseCategoryEnum.UPPER, ExerciseCategoryEnum.LOWER -> ModuleBlue
    ExerciseCategoryEnum.CORE -> ModuleGreen
    ExerciseCategoryEnum.FUNCTIONAL -> ModuleOlive
    ExerciseCategoryEnum.OTHER, null -> ModuleGreen
}

/**
 * 本周打卡追踪表（Mon–Sun，heatmap 样式）。主区方块可点击（过去与今天可补打/取消，未来禁用）。
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
    val range = if (week.size == 7) {
        "${week.first().format(DateTimeFormatter.ofPattern("M.d"))} - ${week.last().format(DateTimeFormatter.ofPattern("M.d"))}"
    } else ""
    WorkbenchCard(contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(14.dp, 14.dp).background(ModuleGreen, RoundedCornerShape(4.dp)))
            Spacer(Modifier.width(8.dp))
            Text("本周追踪", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(range, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "习惯",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(labelColumnWidth)
                )
                week.forEachIndexed { i, day ->
                    val isToday = day.toString() == today
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            weekLabels[i],
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isToday) ModuleGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Visible
                        )
                    }
                }
            }
            activeHabits.forEach { habit ->
                val color = habitCategoryColor(habit.category)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier.width(labelColumnWidth),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(6.dp).background(color, RoundedCornerShape(3.dp)))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            habit.title,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    week.forEach { day ->
                        val date = day.toString()
                        val checked = "${habit.id}|$date" in checkedKeys
                        val enabled = date <= today
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            HeatmapTile(
                                checked = checked,
                                enabled = enabled,
                                color = color,
                                onClick = { onToggle(habit.id, date) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatmapTile(checked: Boolean, enabled: Boolean, color: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .size(cellSize)
            .then(
                if (checked) Modifier.background(color, shape)
                else Modifier
                    .dashedRoundBorder(if (enabled) color.copy(alpha = 0.45f) else PaperBorder, 8.dp)
                    .background(if (enabled) PaperNested else Color.Transparent, shape)
            )
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {}
}

private fun Modifier.dashedRoundBorder(color: Color, cornerRadius: Dp): Modifier = drawBehind {
    val stroke = 1.dp.toPx()
    val r = cornerRadius.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(stroke / 2, stroke / 2),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(r),
        style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
    )
}
