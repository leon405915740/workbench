package com.accounting.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accounting.app.BuildConfig
import com.accounting.app.log.AppLogger
import com.accounting.app.ui.model.UiState
import com.accounting.app.ui.theme.BackgroundGray
import com.accounting.app.ui.theme.CardWhite
import com.accounting.app.ui.theme.DividerColor
import com.accounting.app.ui.theme.NavActive
import com.accounting.app.ui.theme.TextPrimary
import com.accounting.app.ui.theme.TextSecondary

/**
 * 设置主页面。
 *
 * 分组列表布局，使用白色卡片包裹每组配置项：
 * - AI 配置：API Key 设置、分类记忆管理
 * - 数据管理：导出 CSV、恢复默认记忆
 * - 关于：版本号
 *
 * 底部展示个人自用提示文字。
 */
@Composable
fun SettingsScreen(
    uiState: UiState,
    onSaveApiKey: (String) -> Unit,
    onManageMemories: () -> Unit,
    onRestoreMemories: () -> Unit,
    onExportCsv: () -> Unit,
    onExportLog: () -> Unit,
    onToggleAutoLearn: (() -> Unit)? = null
) {
    val scrollState = rememberScrollState()

    // API Key 弹窗显示状态
    var showApiKeyDialog by remember { mutableStateOf(false) }
    // 恢复默认记忆确认弹窗状态
    var showRestoreDialog by remember { mutableStateOf(false) }
    // 调试日志开关（运行时可切换，默认跟随构建类型）
    var debugLog by remember { mutableStateOf(AppLogger.isDebugLogEnabled()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
            .verticalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        // ===== 第一组：AI 配置 =====
        SettingsGroup {
            SettingsItem(
                title = "API Key 设置",
                onClick = { showApiKeyDialog = true }
            )
            SettingsDivider()
            SettingsItem(
                title = "分类记忆管理",
                onClick = onManageMemories
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ===== 第二组：学习偏好 =====
        SettingsGroup {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "记账后自动弹出学习确认",
                    fontSize = 16.sp,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (onToggleAutoLearn != null) {
                    Switch(
                        checked = uiState.autoLearnEnabled,
                        onCheckedChange = { onToggleAutoLearn() },
                        colors = SwitchDefaults.colors(checkedThumbColor = CardWhite, checkedTrackColor = NavActive)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ===== 第三组：数据管理 =====
        SettingsGroup {
            SettingsItem(
                title = "导出数据 CSV",
                onClick = onExportCsv
            )
            SettingsDivider()
            SettingsItem(
                title = "导出日志",
                onClick = onExportLog
            )
            SettingsDivider()
            SettingsItem(
                title = "恢复默认记忆",
                onClick = { showRestoreDialog = true }
            )
            SettingsDivider()
            // 调试日志开关：Release 包默认关闭详细日志，可在此开启便于排查
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "调试日志（详细）",
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "开启后记录完整运行日志，崩溃与错误始终记录",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = debugLog,
                    onCheckedChange = {
                        debugLog = it
                        AppLogger.setDebugLogEnabled(it)
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = CardWhite, checkedTrackColor = NavActive)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ===== 第三组：关于 =====
        SettingsGroup {
            SettingsItem(
                title = "版本号",
                value = "记账 v${BuildConfig.VERSION_NAME}",
                onClick = {}
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ===== 底部提示 =====
        Text(
            text = "个人自用·数据本地存储",
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }

    // API Key 弹窗
    if (showApiKeyDialog) {
        ApiKeyDialog(
            initialKey = uiState.savedApiKey.ifBlank { BuildConfig.DEEPSEEK_API_KEY },
            onSave = {
                onSaveApiKey(it)
                showApiKeyDialog = false
            },
            onDismiss = { showApiKeyDialog = false }
        )
    }

    // 恢复默认记忆二次确认弹窗
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = {
                Text(
                    text = "恢复默认记忆",
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            },
            text = {
                Text(text = "将清空当前所有记忆并恢复默认值，是否继续？")
            },
            confirmButton = {
                TextButton(onClick = {
                    onRestoreMemories()
                    showRestoreDialog = false
                }) {
                    Text("确认", color = NavActive)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("取消", color = TextSecondary)
                }
            }
        )
    }
}

/**
 * 设置分组卡片：白色背景 + 圆角，包裹一组设置项。
 */
@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(content = content)
    }
}

/**
 * 单行设置项。
 *
 * - 左侧标题 16sp 主色文字
 * - 右侧若提供 value 显示 14sp 灰色值文字，否则显示右箭头图标
 * - 行高至少 56dp，整行可点击
 */
@Composable
private fun SettingsItem(
    title: String,
    value: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        if (value != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                color = TextSecondary
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 行间浅灰分割线：1dp 高，左右各内缩 16dp。
 */
@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(horizontal = 16.dp)
            .background(DividerColor)
    )
}

/**
 * API Key 输入弹窗。
 *
 * 回填已保存值，默认掩码显示（密码模式），用户可直接输入覆盖。
 */
@Composable
private fun ApiKeyDialog(
    initialKey: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var key by remember { mutableStateOf(initialKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "API Key 设置", fontWeight = FontWeight.SemiBold, color = TextPrimary)
        },
        text = {
            OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(text = "请输入 DeepSeek API Key", color = TextSecondary) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = NavActive,
                    unfocusedIndicatorColor = DividerColor
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(key) }) {
                Text("保存", color = NavActive)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextSecondary)
            }
        }
    )
}
