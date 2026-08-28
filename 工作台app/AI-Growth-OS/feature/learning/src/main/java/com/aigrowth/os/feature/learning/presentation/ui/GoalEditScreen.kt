package com.aigrowth.os.feature.learning.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aigrowth.os.core.design.GradientButton
import com.aigrowth.os.core.design.Morandi
import com.aigrowth.os.core.design.MorandiCard
import com.aigrowth.os.feature.learning.presentation.GoalEditViewModel

/**
 * 创建/编辑目标页面——莫兰迪雾蓝紫设计
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: GoalEditViewModel = hiltViewModel()
) {
    val title by viewModel.title.collectAsState()
    val description by viewModel.description.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    // 监听保存完成事件
    LaunchedEffect(Unit) {
        viewModel.saveComplete.collect { success ->
            if (success) {
                onNavigateBack()
            }
        }
    }

    Scaffold(
        containerColor = Morandi.BackgroundGray,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "新建学习目标",
                        fontWeight = FontWeight.Bold,
                        color = Morandi.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = Morandi.BrandPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 输入卡片
            MorandiCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { viewModel.setTitle(it) },
                        label = { Text("学习目标") },
                        placeholder = { Text("例如：我要学AI开发") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isSaving,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Morandi.BrandPrimary,
                            unfocusedBorderColor = Morandi.BorderDefault,
                            focusedLabelColor = Morandi.BrandPrimary,
                            cursorColor = Morandi.BrandPrimary
                        )
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { viewModel.setDescription(it) },
                        label = { Text("详细描述") },
                        placeholder = { Text("描述你的学习目标和期望") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        enabled = !isSaving,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Morandi.BrandPrimary,
                            unfocusedBorderColor = Morandi.BorderDefault,
                            focusedLabelColor = Morandi.BrandPrimary,
                            cursorColor = Morandi.BrandPrimary
                        )
                    )
                }
            }

            // 提示卡片
            MorandiCard(
                modifier = Modifier.fillMaxWidth(),
                background = Morandi.BubbleAi
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Morandi.BrandPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "创建目标后，AI将为你生成个性化的学习路线，包括5个学习等级和每日任务。",
                        style = MaterialTheme.typography.bodySmall,
                        color = Morandi.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 保存按钮（品牌渐变）
            GradientButton(
                text = if (isSaving) "保存中..." else "创建目标",
                onClick = { viewModel.saveGoal() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = title.isNotBlank() && !isSaving
            )
        }
    }
}
