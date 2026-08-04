package com.accounting.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accounting.app.ui.model.ChatMessage
import com.accounting.app.ui.theme.CardWhite
import com.accounting.app.ui.components.getCategoryEmoji
import com.accounting.app.ui.theme.ConfidenceHigh
import com.accounting.app.ui.theme.ConfidenceLow
import com.accounting.app.ui.theme.ConfidenceMedium
import com.accounting.app.ui.theme.WeChatGreenLight
import com.accounting.app.ui.theme.WeChatGreen
import com.accounting.app.ui.theme.TextAmount
import com.accounting.app.ui.theme.TextDelete
import com.accounting.app.ui.theme.TextIncome
import com.accounting.app.ui.theme.TextPrimary
import com.accounting.app.ui.theme.TextSecondary
import com.accounting.app.util.AmountUtils
import com.accounting.app.util.TimeUtils

@Composable
fun ExpenseCard(
    message: ChatMessage.CardMessage,
    onEditRecord: () -> Unit,
    onDelete: () -> Unit,
    onLearnKeyword: (() -> Unit)? = null
) {
    val amountPrefix = if (message.type == "income") "+" else ""
    val amountColor = if (message.type == "income") TextIncome else TextAmount
    val accentColor = if (message.type == "income") TextIncome else TextAmount

    // 仅未命中记忆 + 商家非空时显示「保存关键词」按钮
    val showLearnButton = !message.matchedMemory &&
            !message.merchant.isNullOrBlank() &&
            onLearnKeyword != null

    val confidenceColor = when {
        message.confidence >= 0.9f -> ConfidenceHigh
        message.confidence >= 0.7f -> ConfidenceMedium
        else -> ConfidenceLow
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )
            Column(modifier = Modifier.weight(1f).padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = getCategoryEmoji(message.category, message.type),
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = message.category, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                    if (message.matchedMemory) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(WeChatGreenLight).padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(text = "已匹配记忆", fontSize = 10.sp, color = WeChatGreen, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "$amountPrefix${AmountUtils.fenToYuanWithSymbol(message.amount)}",
                    fontSize = 26.sp, fontWeight = FontWeight.Bold, color = amountColor
                )

                Spacer(modifier = Modifier.height(6.dp))

                val infoText = buildString {
                    message.merchant?.takeIf { it.isNotBlank() }?.let { append(it).append(" · ") }
                    append(TimeUtils.formatTimeRelative(message.recordTime))
                }
                Text(text = infoText, fontSize = 12.sp, color = TextSecondary)

                // 备注（如果有）
                message.note?.takeIf { it.isNotBlank() }?.let { note ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = note, fontSize = 13.sp, color = TextSecondary)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val dots = 3
                    val filledIndex = when {
                        message.confidence >= 0.9f -> 3
                        message.confidence >= 0.7f -> 2
                        else -> 1
                    }
                    repeat(dots) { i ->
                        Box(
                            modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(
                                if (i < filledIndex) confidenceColor else confidenceColor.copy(alpha = 0.2f)
                            )
                        )
                        if (i < dots - 1) Spacer(modifier = Modifier.width(5.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (showLearnButton) {
                        TextButton(onClick = onLearnKeyword!!) {
                            Text("保存关键词", fontSize = 13.sp, color = WeChatGreen, fontWeight = FontWeight.Medium)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onEditRecord) {
                            Text("编辑", color = TextPrimary, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                        TextButton(onClick = onDelete) {
                            Text("删除", color = TextDelete, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
