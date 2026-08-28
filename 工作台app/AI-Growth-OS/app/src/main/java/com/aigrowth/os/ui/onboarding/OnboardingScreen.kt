package com.aigrowth.os.ui.onboarding

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.accounting.app.log.AppLogger
import java.util.Base64

/**
 * 首次启动引导页面
 * 帮助用户配置API Key
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current

    var currentStep by remember { mutableIntStateOf(0) }
    var apiKey by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf("zen") }
    var isLoading by remember { mutableStateOf(false) }

    val steps = listOf("欢迎", "配置AI", "开始使用")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 进度指示器
            if (currentStep == 1) {
                TextButton(
                    onClick = {
                        val requestId = AppLogger.generateRequestId()
                        AppLogger.i(requestId, "OnboardingScreen", "skipAiConfiguration 入口")
                        setOnboardingComplete(context)
                        AppLogger.d(requestId, "OnboardingScreen", "skipAiConfiguration 成功")
                        onComplete()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("跳过，稍后配置") }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                steps.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .background(
                                if (index <= currentStep)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.small
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 内容区域
            when (currentStep) {
                0 -> WelcomeStep()
                1 -> ApiKeyStep(
                    apiKey = apiKey,
                    onApiKeyChange = { apiKey = it },
                    selectedModel = selectedModel,
                    onModelChange = { selectedModel = it }
                )
                2 -> CompleteStep()
            }

            Spacer(modifier = Modifier.weight(1f))

            // 底部按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentStep > 0) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("上一步")
                    }
                }

                Button(
                    onClick = {
                        when (currentStep) {
                            1 -> {
                                // 保存API Key
                                if (apiKey.isNotBlank()) {
                                    isLoading = true
                                    val requestId = AppLogger.generateRequestId()
                                    AppLogger.i(
                                        requestId,
                                        "OnboardingScreen",
                                        "saveApiConfiguration 入口: model=$selectedModel"
                                    )
                                    saveApiKey(context, apiKey, selectedModel)
                                    AppLogger.d(
                                        requestId,
                                        "OnboardingScreen",
                                        "saveApiConfiguration 成功"
                                    )
                                    isLoading = false
                                    currentStep++
                                }
                            }
                            2 -> {
                                setOnboardingComplete(context)
                                onComplete()
                            }
                            else -> {
                                currentStep++
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading && (currentStep != 1 || apiKey.isNotBlank())
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = when (currentStep) {
                                2 -> "开始使用"
                                else -> "下一步"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Psychology,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "欢迎使用\n工作台",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "把健身、自媒体、英语和记账\n安排进每天的工作台",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureItem(Icons.Default.FitnessCenter, "健身打卡", "记录每天的训练")
                FeatureItem(Icons.Default.VideoLibrary, "自媒体", "整理内容创作计划")
                FeatureItem(Icons.Default.Translate, "学英语", "积累每日学习任务")
                FeatureItem(Icons.Default.AccountBalanceWallet, "记账", "清晰掌握日常收支")
            }
        }
    }
}

@Composable
private fun FeatureItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApiKeyStep(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    selectedModel: String,
    onModelChange: (String) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Key,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "配置你的AI助手",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "输入API Key以启用AI功能",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = onApiKeyChange,
                    label = { Text("API Key") },
                    placeholder = { Text("sk-xxx...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Key, contentDescription = null)
                    }
                )

                Text(
                    text = "选择AI模型",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedModel == "deepseek",
                        onClick = { onModelChange("deepseek") },
                        label = { Text("DeepSeek") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedModel == "zen",
                        onClick = { onModelChange("zen") },
                        label = { Text("OpenCode Zen") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "如何获取API Key？",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• DeepSeek: platform.deepseek.com\n• OpenCode Zen: opencode.ai/zen\n\n你的API Key只存储在本地，不会上传到任何服务器。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun CompleteStep() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "准备就绪！",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "工作台已准备好\n从今天的四项日常开始",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "💡 新手建议",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "1. 完成一次健身打卡\n2. 记录一个创作想法\n3. 学习几分钟英语\n4. 记下一笔日常收支",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// SharedPreferences helpers
private const val PREFS_NAME = "AI_Growth_OS_Prefs"
private const val KEY_API_KEY = "api_key"
private const val KEY_MODEL = "ai_model"
private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
private const val ENCODED_API_KEY_PREFIX = "b64:"

private fun saveApiKey(context: Context, apiKey: String, model: String) {
    val encodedApiKey = Base64.getEncoder()
        .encodeToString(apiKey.toByteArray(Charsets.UTF_8))
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_API_KEY, ENCODED_API_KEY_PREFIX + encodedApiKey)
        .putString(KEY_MODEL, model)
        .apply()
}

private fun setOnboardingComplete(context: Context) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_ONBOARDING_COMPLETE, true)
        .apply()
}

fun isOnboardingComplete(context: Context): Boolean {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_ONBOARDING_COMPLETE, false)
}
