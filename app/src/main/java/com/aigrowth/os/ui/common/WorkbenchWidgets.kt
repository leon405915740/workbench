package com.aigrowth.os.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun todayString(): String = LocalDate.now().toString()

private val displayFormatter = DateTimeFormatter.ofPattern("M月d日")

fun formatDate(iso: String): String = runCatching {
    LocalDate.parse(iso).format(displayFormatter)
}.getOrDefault(iso)

fun formatProgress(value: Float): String =
    if (value % 1f == 0f) value.toInt().toString() else value.toString()

@Composable
fun SearchField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String = "搜索",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "清除")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)
    )
}

@Composable
fun PriorityBadge(priority: String, modifier: Modifier = Modifier) {
    val (bg, fg) = when (priority) {
        "P0" -> Color(0xFFFDE7E7) to Color(0xFFB3261E)
        "P1" -> Color(0xFFFFF0DF) to Color(0xFFB87414)
        else -> Color(0xFFEFEEE8) to Color(0xFF687069)
    }
    Surface(color = bg, shape = RoundedCornerShape(6.dp), modifier = modifier) {
        Text(
            priority,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun TagChip(text: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun ConfirmDeleteDialog(
    title: String = "删除确认",
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("删除", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}