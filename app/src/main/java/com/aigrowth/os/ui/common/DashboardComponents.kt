package com.aigrowth.os.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aigrowth.os.ui.theme.InkSecondary
import com.aigrowth.os.ui.theme.InkTertiary
import com.aigrowth.os.ui.theme.ModuleGreen
import com.aigrowth.os.ui.theme.PaperNested

data class ReportRow(
    val label: String,
    val valueText: String,
    val sublabel: String? = null,
    val fraction: Float? = null
)

@Composable
fun StatCard(
    title: String,
    value: String,
    caption: String,
    icon: ImageVector,
    tint: Color,
    progress: Float?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    WorkbenchCard(
        modifier = modifier.clickable(onClick = onClick),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(tint.copy(alpha = 0.12f), RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(6.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, color = InkSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            caption,
            style = MaterialTheme.typography.labelSmall,
            color = InkTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (progress != null) {
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = ModuleGreen,
                trackColor = PaperNested
            )
        }
    }
}

@Composable
fun ReportRowItem(row: ReportRow) {
    val accent = ModuleGreen
    val track = PaperNested
    if (row.fraction == null) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(row.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(row.valueText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    row.label,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(58.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .background(track, RoundedCornerShape(5.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(row.fraction)
                            .fillMaxHeight()
                            .background(accent, RoundedCornerShape(5.dp))
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    row.valueText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            row.sublabel?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 58.dp)
                )
            }
        }
    }
}
