package com.aigrowth.os.feature.creator.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aigrowth.os.core.aiengine.RecommendedResource
import com.aigrowth.os.feature.creator.presentation.CreatorViewModel

/**
 * 资源推荐页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceRecommendScreen(
    onNavigateBack: () -> Unit,
    viewModel: CreatorViewModel = hiltViewModel()
) {
    var topic by remember { mutableStateOf("") }
    var userLevel by remember { mutableStateOf("入门") }

    val resourceResponse by viewModel.resourceResponse.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val error by viewModel.error.collectAsState()

    val levels = listOf("入门", "进阶", "高级")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AI资源推荐",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 输入区域
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = topic,
                        onValueChange = { topic = it },
                        label = { Text("学习主题") },
                        placeholder = { Text("例如：Android开发、英语口语...") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
                        }
                    )

                    Text(
                        text = "当前水平",
                        style = MaterialTheme.typography.labelMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        levels.forEach { level ->
                            FilterChip(
                                selected = userLevel == level,
                                onClick = { userLevel = level },
                                label = { Text(level) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (topic.isNotBlank()) {
                                viewModel.recommendResources(topic, userLevel)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = topic.isNotBlank() && !isGenerating
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (isGenerating) "推荐中..." else "AI推荐资源")
                    }
                }
            }

            // 错误提示
            error?.let { errorMsg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMsg,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // 资源推荐结果
            resourceResponse?.let { response ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "学习路径建议",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = response.learningPathSuggestion,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                response.recommendedResources.forEach { resource ->
                    ResourceItem(resource = resource)
                }
            }

            // 提示
            if (resourceResponse == null && error == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "输入学习主题，AI将为你推荐适合的资源",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResourceItem(resource: RecommendedResource) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = resource.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                AssistChip(
                    onClick = {},
                    label = { Text(resource.difficulty) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = when (resource.difficulty) {
                            "入门" -> MaterialTheme.colorScheme.primaryContainer
                            "进阶" -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.errorContainer
                        }
                    )
                )
            }

            Text(
                text = resource.description,
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Category,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = resource.type,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = resource.suitableFor,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = resource.duration,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
