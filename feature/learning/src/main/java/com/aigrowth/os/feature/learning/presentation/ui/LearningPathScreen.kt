package com.aigrowth.os.feature.learning.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aigrowth.os.core.database.entity.LearningLevel
import com.aigrowth.os.core.database.entity.LevelStatus
import com.aigrowth.os.feature.learning.presentation.LearningPathViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 学习路线页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningPathScreen(
    goalId: String,
    goalTitle: String,
    onNavigateBack: () -> Unit,
    onLevelClick: (String) -> Unit,
    viewModel: LearningPathViewModel = hiltViewModel()
) {
    val learningPath by viewModel.learningPath.collectAsState()
    val learningLevels by viewModel.learningLevels.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val error by viewModel.error.collectAsState()
    
    LaunchedEffect(goalId) {
        viewModel.loadLearningPath(goalId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = learningPath?.title ?: "学习路线",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        if (isGenerating) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "AI正在生成学习路线...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (learningPath == null) {
            // 没有学习路线，显示生成按钮
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.Route,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "还没有学习路线",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "点击下方按钮让AI为你生成",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { 
                            viewModel.generateLearningPath(goalId, goalTitle)
                        }
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI生成学习路线")
                    }
                }
            }
        } else {
            // 显示学习路线
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 学习路线描述
                learningPath?.let { currentPath ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "学习路线说明",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = currentPath.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "当前进度：${currentPath.currentLevel}/${currentPath.totalLevels}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                
                // 学习等级列表
                itemsIndexed(learningLevels) { index, level ->
                    LevelCard(
                        level = level,
                        isCurrent = learningPath?.let { index == it.currentLevel - 1 } ?: false,
                        onClick = { onLevelClick(level.id) }
                    )
                }
            }
        }
        
        // 错误提示
        error?.let {
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("关闭")
                    }
                }
            ) {
                Text(it)
            }
        }
    }
}

/**
 * 学习等级卡片
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelCard(
    level: LearningLevel,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val gson = Gson()
    val isEnabled = level.status != LevelStatus.LOCKED
    
    Card(
        onClick = if (isEnabled) onClick else ({}),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!isEnabled) Modifier.alpha(0.5f) else Modifier
            ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCurrent) 4.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = when (level.status) {
                LevelStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
                LevelStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 等级编号
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.medium,
                color = when (level.status) {
                    LevelStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                    LevelStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.outline
                }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (level.status == LevelStatus.COMPLETED) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    } else if (level.status == LevelStatus.LOCKED) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = level.levelNumber.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 等级内容
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = level.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = level.objective,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // 知识点数量
                val knowledgePoints = runCatching {
                    gson.fromJson<List<String>>(
                        level.knowledgePoints,
                        object : TypeToken<List<String>>() {}.type
                    )
                }.getOrElse { emptyList() }
                if (knowledgePoints.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${knowledgePoints.size}个知识点",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // 箭头
            if (isEnabled) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}