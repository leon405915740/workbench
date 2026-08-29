package com.aigrowth.os.ui.onboarding

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 首次启动引导页
 * 简洁介绍工作台的核心模块，点击「开始使用」进入应用。
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current

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
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "欢迎使用\n工作台",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "把计划、习惯、阅读、运动、记账和随笔\n安排进每天的日常",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

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
                    FeatureItem(Icons.Default.Checklist, "今日计划", "安排每天的计划与待办")
                    FeatureItem(Icons.Default.CheckCircle, "习惯打卡", "坚持每天的好习惯")
                    FeatureItem(Icons.Default.MenuBook, "阅读", "记录阅读进度与笔记")
                    FeatureItem(Icons.Default.DirectionsRun, "运动", "追踪每日运动打卡")
                    FeatureItem(Icons.Default.AccountBalanceWallet, "记账", "清晰掌握日常收支")
                    FeatureItem(Icons.Default.EditNote, "随笔", "随时记录灵感与想法")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    setOnboardingComplete(context)
                    onComplete()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("开始使用")
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

// SharedPreferences helpers
private const val PREFS_NAME = "AI_Growth_OS_Prefs"
private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"

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