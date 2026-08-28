package com.accounting.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.accounting.app.domain.rule.RuleSuggestion
import com.accounting.app.ui.theme.BackgroundGray
import com.accounting.app.ui.theme.CardWhite
import com.accounting.app.ui.theme.TextPrimary
import com.accounting.app.ui.theme.TextSecondary
import com.accounting.app.ui.theme.WeChatGreen
import com.accounting.app.ui.components.getCategoryEmoji

@Composable
fun RuleSuggestionDialog(
    suggestion: RuleSuggestion,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "💡 检测到新的记账模式",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Text(
                    text = "当输入包含「${suggestion.keywords.joinToString("」「")}」时",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                val categoryEmoji = getCategoryEmoji(suggestion.category, suggestion.type)
                Text(
                    text = "自动归为「$categoryEmoji ${suggestion.category}${suggestion.subCategory?.let { "-$it" } ?: ""}」",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BackgroundGray,
                            contentColor = TextSecondary
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text("仅本次使用", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onSave,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WeChatGreen),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text("保存规则", fontSize = 14.sp, color = CardWhite)
                    }
                }
            }
        }
    }
}