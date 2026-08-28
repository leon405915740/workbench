package com.aigrowth.os.feature.learning.presentation.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aigrowth.os.core.aiengine.FeynmanMessage
import com.aigrowth.os.core.aiengine.FeynmanRole
import com.aigrowth.os.feature.learning.presentation.FeynmanViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeynmanLearningScreen(
    topic: String,
    knowledgeCardId: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: FeynmanViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isCompleted by viewModel.isCompleted.collectAsState()
    val lastResponse by viewModel.lastResponse.collectAsState()
    val error by viewModel.error.collectAsState()
    val currentSession by viewModel.currentSession.collectAsState()

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // 启动会话
    LaunchedEffect(topic) {
        if (currentSession == null) {
            viewModel.startSession(topic, knowledgeCardId)
        }
    }

    // 自动滚动到底部
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "费曼学习模式",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = topic,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.abandonSession()
                        viewModel.reset()
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "退出")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        bottomBar = {
            if (!isCompleted) {
                MessageInputBar(
                    isLoading = isLoading,
                    onSend = { content ->
                        viewModel.sendExplanation(content)
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                messages.isEmpty() && !isLoading -> {
                    // 初始提示
                    FeynmanInitialGuide(topic = topic)
                }
                isCompleted && lastResponse != null -> {
                    // 完成状态
                    lastResponse?.let { response ->
                        FeynmanCompletionView(
                            score = response.response.score,
                            feedback = response.response.childResponse,
                            suggestions = response.response.improvementSuggestions,
                            onFinish = {
                                viewModel.reset()
                                onNavigateBack()
                            }
                        )
                    }
                }
                else -> {
                    // 对话列表
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(messages, key = { it.hashCode() }) { message ->
                            MessageBubble(message = message)
                        }

                        if (isLoading) {
                            item {
                                LoadingBubble()
                            }
                        }
                    }
                }
            }

            // 错误提示
            error?.let { errorMsg ->
                Snackbar(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("关闭")
                        }
                    }
                ) {
                    Text(errorMsg)
                }
            }
        }
    }
}

@Composable
private fun FeynmanInitialGuide(topic: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Psychology,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "费曼学习法",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "试着用最简单的语言，向一个12岁的孩子解释：",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = topic,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            GuideItem(text = "避免使用专业术语")
            GuideItem(text = "用比喻和例子帮助理解")
            GuideItem(text = "逻辑要清晰连贯")
            GuideItem(text = "像讲故事一样讲解")
        }
    }
}

@Composable
private fun GuideItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MessageBubble(message: FeynmanMessage) {
    val isUser = message.role == FeynmanRole.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // AI头像
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "12",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!isUser) {
                    Text(
                        text = "12岁的孩子",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LoadingBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "12",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 200.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val alpha by rememberInfiniteTransition(label = "loading").animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = index * 150),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                            )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageInputBar(
    isLoading: Boolean,
    onSend: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    Surface(
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("输入你的解释...") },
                modifier = Modifier.weight(1f),
                enabled = !isLoading,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                maxLines = 5
            )

            FilledIconButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onSend(text.trim())
                        text = ""
                    }
                },
                enabled = text.isNotBlank() && !isLoading,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "发送"
                )
            }
        }
    }
}

@Composable
private fun FeynmanCompletionView(
    score: Int,
    feedback: String,
    suggestions: List<String>,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 分数显示
        val scoreColor = when {
            score >= 90 -> MaterialTheme.colorScheme.primary
            score >= 70 -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.error
        }

        Surface(
            shape = CircleShape,
            color = scoreColor.copy(alpha = 0.1f),
            modifier = Modifier.size(140.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$score",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                    Text(
                        text = "分",
                        style = MaterialTheme.typography.titleMedium,
                        color = scoreColor.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 结果标题
        val resultTitle = when {
            score >= 90 -> "太棒了！你已经真正掌握了这个概念"
            score >= 70 -> "不错！还有一些可以改进的地方"
            else -> "继续加油！再试着简化你的解释"
        }

        Text(
            text = resultTitle,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // AI反馈
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Face,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "12岁孩子的反馈",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = feedback,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 改进建议
        if (suggestions.isNotEmpty() && score < 90) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "改进建议",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    suggestions.forEach { suggestion ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "• ",
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // 完成按钮
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("完成")
        }
    }
}
