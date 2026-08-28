package com.accounting.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accounting.app.ui.components.ExpenseCard
import com.accounting.app.ui.model.ChatMessage
import com.accounting.app.ui.model.LearnDialogData
import com.accounting.app.ui.model.UiState
import com.accounting.app.ui.theme.BackgroundGray
import com.accounting.app.ui.theme.BorderDefault
import com.accounting.app.ui.theme.BubbleAi
import com.accounting.app.ui.theme.BubbleError
import com.accounting.app.ui.theme.BubbleUser
import com.accounting.app.ui.theme.CardWhite
import com.accounting.app.ui.theme.DividerColor
import com.accounting.app.ui.theme.NavActive
import com.accounting.app.ui.theme.WeChatGreen
import com.accounting.app.ui.theme.TextPrimary
import com.accounting.app.ui.theme.TextSecondary
import com.accounting.app.util.TimeUtils

@Composable
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
fun ChatScreen(
    uiState: UiState,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onEditRecord: (ChatMessage.CardMessage) -> Unit,
    onDelete: (Long, String) -> Unit,
    onManualEntry: (String) -> Unit,
    onLearnKeyword: (ChatMessage.CardMessage) -> Unit,
    onDismissLearn: () -> Unit,
    onConfirmLearn: (triggerWord: String, type: String, category: String) -> Unit
) {
    val listState = rememberLazyListState()
    val keyboard = LocalSoftwareKeyboardController.current
    val messages = uiState.messages

    LaunchedEffect(messages.size, uiState.isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // 重复点击记账Tab → 滚动到顶部
    LaunchedEffect(uiState.chatResetSignal) {
        if (uiState.chatResetSignal > 0 && messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "\uD83D\uDCB0",
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "输入消费内容，例如：午饭 25 元 麦当劳",
                            fontSize = 14.sp,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "AI 帮你自动记账",
                            fontSize = 12.sp,
                            color = TextSecondary.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 14.dp,
                        vertical = 14.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages, key = { it.timestamp }) { message ->
                        when (message) {
                            is ChatMessage.UserMessage -> UserBubble(message)
                            is ChatMessage.AiMessage -> AiBubble(message)
                            is ChatMessage.CardMessage -> {
                                ExpenseCard(
                                    message = message,
                                    onEditRecord = { onEditRecord(message) },
                                    onDelete = { onDelete(message.recordId, message.type) },
                                    onLearnKeyword = {
                                        onLearnKeyword(message)
                                    }
                                )
                            }
                            is ChatMessage.ErrorMessage -> ErrorBubble(
                                message = message,
                                onManualEntry = { onManualEntry(message.rawInput) }
                            )
                            is ChatMessage.AiTextMessage -> AiTextBubble(message)
                        }
                    }
                    if (uiState.isLoading) {
                        item { LoadingIndicator() }
                    }
                }
            }
        }

        BottomInputBar(
            text = uiState.inputText,
            isLoading = uiState.isLoading,
            onInputChange = onInputChange,
            onSend = {
                keyboard?.hide()
                onSend()
            },
            onManualEntry = { onManualEntry("") }
        )
    }

    // 关键词学习确认弹窗
    uiState.showLearnDialog?.let { dialog ->
        LearnConfirmDialog(
            dialog = dialog,
            onConfirm = { word, type, cat ->
                onConfirmLearn(word, type, cat)
            },
            onDismiss = onDismissLearn
        )
    }
}

@Composable
private fun UserBubble(message: ChatMessage.UserMessage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = TimeUtils.formatTimeRelative(message.timestamp),
            fontSize = 10.sp,
            color = TextSecondary,
            modifier = Modifier.padding(end = 6.dp, bottom = 3.dp)
        )
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp))
                .background(BubbleUser)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(text = message.text, fontSize = 15.sp, color = TextPrimary)
        }
    }
}

@Composable
private fun LearnConfirmDialog(
    dialog: LearnDialogData,
    onConfirm: (triggerWord: String, type: String, category: String) -> Unit,
    onDismiss: () -> Unit
) {
    var triggerWord by remember { mutableStateOf(dialog.triggerWord) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("保存分类记忆", fontWeight = FontWeight.SemiBold, color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "将「${dialog.category}」与关键词关联，下次自动识别。",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = triggerWord,
                    onValueChange = { triggerWord = it },
                    label = { Text("触发词", fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = WeChatGreen,
                        unfocusedIndicatorColor = DividerColor
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(triggerWord.trim(), dialog.type, dialog.category) },
                enabled = triggerWord.isNotBlank()
            ) {
                Text("保存", color = WeChatGreen)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("忽略", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun AiBubble(message: ChatMessage.AiMessage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = TimeUtils.formatTimeRelative(message.timestamp),
            fontSize = 10.sp,
            color = TextSecondary,
            modifier = Modifier.padding(start = 6.dp, bottom = 3.dp)
        )
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp),
            colors = CardDefaults.cardColors(containerColor = BubbleAi),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Text(
                text = message.text,
                fontSize = 15.sp,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun AiTextBubble(message: ChatMessage.AiTextMessage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = TimeUtils.formatTimeRelative(message.timestamp),
            fontSize = 10.sp,
            color = TextSecondary,
            modifier = Modifier.padding(start = 6.dp, bottom = 3.dp)
        )
        Card(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp),
            colors = CardDefaults.cardColors(containerColor = BubbleAi),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Text(
                text = message.content,
                fontSize = 14.sp,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun ErrorBubble(
    message: ChatMessage.ErrorMessage,
    onManualEntry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = TimeUtils.formatTimeRelative(message.timestamp),
            fontSize = 10.sp,
            color = TextSecondary,
            modifier = Modifier.padding(start = 6.dp, bottom = 3.dp)
        )
        Card(
            modifier = Modifier.widthIn(max = 320.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BubbleError),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = message.text, fontSize = 14.sp, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onManualEntry,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Text("手动记账", fontSize = 13.sp, color = WeChatGreen, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(14.dp),
            strokeWidth = 2.dp,
            color = NavActive
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "解析中...", fontSize = 12.sp, color = TextSecondary)
    }
}

@Composable
private fun BottomInputBar(
    text: String,
    isLoading: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onManualEntry: () -> Unit
) {
    val canSend = text.isNotBlank() && !isLoading
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(DividerColor)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardWhite)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onManualEntry,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "手动记账",
                    tint = WeChatGreen
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            OutlinedTextField(
                value = text,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = if (isLoading) "解析中..." else "输入消费内容",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                },
                enabled = !isLoading,
                singleLine = true,
                shape = RoundedCornerShape(22.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = CardWhite,
                    unfocusedContainerColor = CardWhite,
                    disabledContainerColor = CardWhite,
                    focusedIndicatorColor = WeChatGreen,
                    unfocusedIndicatorColor = BorderDefault,
                    disabledIndicatorColor = BorderDefault
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onSend,
                enabled = canSend,
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WeChatGreen,
                    disabledContainerColor = WeChatGreen.copy(alpha = 0.35f)
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 14.dp,
                    vertical = 10.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Send,
                    contentDescription = "发送",
                    tint = CardWhite,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text("发送", color = CardWhite, fontSize = 14.sp)
            }
        }
    }
}
