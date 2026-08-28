package com.aigrowth.os.feature.creator.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aigrowth.os.core.aiengine.ContentIdea
import com.aigrowth.os.core.aiengine.ContentIdeaResponse
import com.aigrowth.os.core.aiengine.GrowthReportResponse
import com.aigrowth.os.core.aiengine.ViralAnalysisResponse
import com.aigrowth.os.core.aiengine.ContentScriptResponse
import com.aigrowth.os.core.database.entity.ContentType
import com.aigrowth.os.feature.creator.presentation.CreatorViewModel

/**
 * 创作工作台主页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorWorkbenchScreen(
    onNavigateToResourceRecommend: () -> Unit = {},
    onNavigateToWeeklyPlan: () -> Unit = {},
    viewModel: CreatorViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("创意生成", "爆款分析", "脚本生成", "成长报告", "资源推荐", "学习计划")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "创作工作台",
                        fontWeight = FontWeight.Bold
                    )
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
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                        enabled = true
                    )
                }
            }

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                when (selectedTab) {
                    0 -> IdeaGenerationContent(viewModel = viewModel)
                    1 -> ViralAnalysisContent(viewModel = viewModel)
                    2 -> ScriptGenerationContent(viewModel = viewModel)
                    3 -> GrowthReportContent(viewModel = viewModel)
                    4 -> ResourceRecommendQuickContent(
                        viewModel = viewModel,
                        onMoreClick = onNavigateToResourceRecommend
                    )
                    5 -> WeeklyPlanQuickContent(
                        viewModel = viewModel,
                        onMoreClick = onNavigateToWeeklyPlan
                    )
                }
            }
        }
    }
}

/**
 * 创意生成内容
 */
@Composable
private fun IdeaGenerationContent(viewModel: CreatorViewModel) {
    var topic by remember { mutableStateOf("") }
    var targetAudience by remember { mutableStateOf("") }
    var contentType by remember { mutableStateOf("短视频") }

    val ideaResponse by viewModel.ideaResponse.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val error by viewModel.error.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 输入表单
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "生成内容创意",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("学习主题") },
                    placeholder = { Text("如：AI编程、英语学习、健身") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = targetAudience,
                    onValueChange = { targetAudience = it },
                    label = { Text("目标受众") },
                    placeholder = { Text("如：零基础学习者、职场人士") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    text = "内容类型",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("短视频", "长视频", "文章", "图文").forEach { type ->
                        FilterChip(
                            selected = contentType == type,
                            onClick = { contentType = type },
                            label = { Text(type) }
                        )
                    }
                }

                Button(
                    onClick = {
                        if (topic.isNotBlank() && targetAudience.isNotBlank()) {
                            viewModel.generateContentIdea(topic, targetAudience, contentType)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isGenerating && topic.isNotBlank() && targetAudience.isNotBlank()
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isGenerating) "生成中..." else "✨ 生成创意")
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

        // 创意结果
        ideaResponse?.let { response ->
            IdeaResultCard(response = response)
        }
    }
}

@Composable
private fun IdeaResultCard(response: ContentIdeaResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI创意建议",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (response.ideas.isNotEmpty()) {
                response.ideas.forEachIndexed { index, idea ->
                    IdeaItem(idea = idea, index = index + 1)
                    if (index < response.ideas.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }

            if (response.topicAnalysis.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "📊 主题分析",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = response.topicAnalysis,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (response.targetAudienceInsights.isNotBlank()) {
                Text(
                    text = "🎯 受众洞察",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = response.targetAudienceInsights,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun IdeaItem(idea: ContentIdea, index: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "#$index",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = idea.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            SuggestionChip(
                onClick = {},
                label = { Text(idea.difficulty) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = "🎣 ",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "钩子：${idea.hook}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = "💡 ",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "要点：${idea.keyPoints.joinToString("、")}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "⏱️ ",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${idea.estimatedDuration}秒 · ${idea.targetPlatforms.joinToString("、")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 爆款分析内容
 */
@Composable
private fun ViralAnalysisContent(viewModel: CreatorViewModel) {
    var contentTitle by remember { mutableStateOf("") }
    var contentUrl by remember { mutableStateOf("") }

    val viralResponse by viewModel.viralResponse.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val error by viewModel.error.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "爆款内容拆解",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = contentTitle,
                    onValueChange = { contentTitle = it },
                    label = { Text("爆款标题") },
                    placeholder = { Text("如：3天学会Python的秘密") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = contentUrl,
                    onValueChange = { contentUrl = it },
                    label = { Text("内容链接（可选）") },
                    placeholder = { Text("如：https://...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        if (contentTitle.isNotBlank()) {
                            viewModel.analyzeViralContent(
                                contentTitle,
                                contentUrl.ifBlank { null }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isGenerating && contentTitle.isNotBlank()
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isGenerating) "分析中..." else "🔍 开始拆解")
                }
            }
        }

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

        viralResponse?.let { response ->
            ViralAnalysisResultCard(response = response)
        }
    }
}

@Composable
private fun ViralAnalysisResultCard(response: ViralAnalysisResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "📊 爆款拆解分析",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            AnalysisItem("🎯 标题拆解", response.titleAnalysis)
            AnalysisItem("🪝 钩子分析", response.hookAnalysis)
            AnalysisItem("📐 结构分析", response.structureAnalysis)
            AnalysisItem("❤️ 情感诉求", response.emotionalAppeal)
            AnalysisItem("👥 目标受众", response.targetAudience)
            AnalysisItem("🔄 转化路径", response.conversionPath)

            if (response.actionableInsights.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "💡 可操作建议",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                response.actionableInsights.forEach { insight ->
                    Text(
                        text = "• $insight",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalysisItem(title: String, content: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
}

/**
 * 脚本生成内容
 */
@Composable
private fun ScriptGenerationContent(viewModel: CreatorViewModel) {
    var idea by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf("抖音") }
    var durationMinutes by remember { mutableIntStateOf(1) }

    val scriptResponse by viewModel.scriptResponse.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val error by viewModel.error.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "生成内容脚本",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = idea,
                    onValueChange = { idea = it },
                    label = { Text("创意/主题") },
                    placeholder = { Text("如：如何用AI提高学习效率") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "发布平台",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("抖音", "B站", "小红书", "微信").forEach { p ->
                            FilterChip(
                                selected = platform == p,
                                onClick = { platform = p },
                                label = { Text(p) }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "视频时长",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(1, 3, 5, 10).forEach { d ->
                            FilterChip(
                                selected = durationMinutes == d,
                                onClick = { durationMinutes = d },
                                label = { Text("${d}分钟") }
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        if (idea.isNotBlank()) {
                            viewModel.generateContentScript(idea, platform, durationMinutes)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isGenerating && idea.isNotBlank()
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isGenerating) "生成中..." else "📝 生成脚本")
                }
            }
        }

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

        scriptResponse?.let { response ->
            ScriptResultCard(response = response)
        }
    }
}

@Composable
private fun ScriptResultCard(response: ContentScriptResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = response.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "🎣 开场钩子",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = response.hook,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Text(
                text = "🎬 分镜脚本",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )

            response.scenes.forEach { scene ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "场景 ${scene.order}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${scene.duration}秒",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "🎬 画面：${scene.visual}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "🎙️ 旁白：${scene.narration}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        scene.notes?.let { note ->
                            Text(
                                text = "📝 备注：$note",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            Text(
                text = "📢 结尾引导",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = response.callToAction,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = response.hashtags.joinToString(" "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * 成长报告内容
 */
@Composable
private fun GrowthReportContent(viewModel: CreatorViewModel) {
    var learningData by remember { mutableStateOf("") }
    var reportType by remember { mutableStateOf("周报") }

    val reportResponse by viewModel.reportResponse.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val error by viewModel.error.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "生成成长报告",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "将学习数据转化为可分享的内容",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = learningData,
                    onValueChange = { learningData = it },
                    label = { Text("学习数据") },
                    placeholder = { Text("如：本周学习AI编程20小时，完成3个项目...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("周报", "月报", "里程碑", "学年总结").forEach { type ->
                        FilterChip(
                            selected = reportType == type,
                            onClick = { reportType = type },
                            label = { Text(type) }
                        )
                    }
                }

                Button(
                    onClick = {
                        if (learningData.isNotBlank()) {
                            viewModel.generateGrowthReport(learningData, reportType)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isGenerating && learningData.isNotBlank()
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isGenerating) "生成中..." else "📊 生成报告")
                }
            }
        }

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

        reportResponse?.let { response ->
            GrowthReportResultCard(response = response)
        }
    }
}

@Composable
private fun GrowthReportResultCard(response: GrowthReportResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = response.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = response.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (response.keyAchievements.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🏆 关键成就",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                response.keyAchievements.forEach { achievement ->
                    Text(
                        text = "✅ $achievement",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (response.lessonsLearned.isNotEmpty()) {
                Text(
                    text = "💡 经验教训",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                response.lessonsLearned.forEach { lesson ->
                    Text(
                        text = "📌 $lesson",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (response.growthCurve.isNotBlank()) {
                Text(
                    text = "📈 成长曲线",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = response.growthCurve,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (response.nextSteps.isNotEmpty()) {
                Text(
                    text = "🎯 下一步计划",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                response.nextSteps.forEach { step ->
                    Text(
                        text = "→ $step",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            HorizontalDivider()

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "🌟 可分享内容",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = response.shareableContent,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

/**
 * 资源推荐快速内容
 */
@Composable
private fun ResourceRecommendQuickContent(
    viewModel: CreatorViewModel,
    onMoreClick: () -> Unit
) {
    var topic by remember { mutableStateOf("") }
    var userLevel by remember { mutableStateOf("入门") }

    val resourceResponse by viewModel.resourceResponse.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val error by viewModel.error.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "AI资源推荐",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("学习主题") },
                    placeholder = { Text("如：Android开发、英语口语") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("入门", "进阶", "高级").forEach { level ->
                        FilterChip(
                            selected = userLevel == level,
                            onClick = { userLevel = level },
                            label = { Text(level) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (topic.isNotBlank()) {
                                viewModel.recommendResources(topic, userLevel)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isGenerating && topic.isNotBlank()
                    ) {
                        Text(if (isGenerating) "推荐中..." else "🔍 AI推荐")
                    }
                    OutlinedButton(
                        onClick = onMoreClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("更多 →")
                    }
                }
            }
        }

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
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = response.learningPathSuggestion,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            response.recommendedResources.take(3).forEach { resource ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = resource.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = resource.description,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = resource.type,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = resource.difficulty,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = resource.duration,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (response.recommendedResources.size > 3) {
                TextButton(
                    onClick = onMoreClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("查看全部 ${response.recommendedResources.size} 个资源 →")
                }
            }
        }
    }
}

/**
 * 周计划快速内容
 */
@Composable
private fun WeeklyPlanQuickContent(
    viewModel: CreatorViewModel,
    onMoreClick: () -> Unit
) {
    var goal by remember { mutableStateOf("") }

    val weeklyPlanResponse by viewModel.weeklyPlanResponse.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val error by viewModel.error.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "7天学习计划",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = goal,
                    onValueChange = { goal = it },
                    label = { Text("学习目标") },
                    placeholder = { Text("如：7天掌握Git基础") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (goal.isNotBlank()) {
                                viewModel.generateWeeklyPlan(goal, 7)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isGenerating && goal.isNotBlank()
                    ) {
                        Text(if (isGenerating) "生成中..." else "📅 生成计划")
                    }
                    OutlinedButton(
                        onClick = onMoreClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("更多天数 →")
                    }
                }
            }
        }

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

        weeklyPlanResponse?.let { response ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = response.goal,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = response.planSummary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            response.dailyPlans.take(3).forEach { plan ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Day ${plan.day}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${plan.estimatedMinutes}分钟",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = plan.theme,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        plan.tasks.take(2).forEach { task ->
                            Text(
                                text = "• $task",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            if (response.dailyPlans.size > 3) {
                TextButton(
                    onClick = onMoreClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("查看全部 ${response.dailyPlans.size} 天计划 →")
                }
            }

            if (response.tips.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "💡 学习建议",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        response.tips.take(2).forEach { tip ->
                            Text(
                                text = "• $tip",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
