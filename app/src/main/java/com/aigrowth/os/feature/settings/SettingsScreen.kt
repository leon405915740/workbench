package com.aigrowth.os.feature.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.accounting.app.AccountingApp
import com.accounting.app.log.AppLogger
import com.aigrowth.os.core.design.Morandi
import com.aigrowth.os.core.design.MorandiCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToMemoryMapping: () -> Unit
) {
    val context = LocalContext.current
    val bridge = remember { AccountingApp.getBridge() }
    val scope = rememberCoroutineScope()

    var notifGranted by remember { mutableStateOf(false) }
    var overlayGranted by remember { mutableStateOf(false) }
    var pendingCsvRequestId by remember { mutableStateOf<String?>(null) }
    var pendingLogRequestId by remember { mutableStateOf<String?>(null) }
    val autoLearn by bridge.isAutoLearnEnabled().collectAsState(initial = true)
    val quickRecord by bridge.isQuickRecordEnabled().collectAsState(initial = true)

    LaunchedEffect(Unit) {
        notifGranted = isNotificationAccessEnabled(context)
        overlayGranted = Settings.canDrawOverlays(context)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notifGranted = isNotificationAccessEnabled(context)
                overlayGranted = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val requestId = pendingCsvRequestId ?: AppLogger.generateRequestId()
        pendingCsvRequestId = null
        uri?.let {
            scope.launch(Dispatchers.IO) {
                runCatching {
                    val csvData = checkNotNull(bridge.prepareCsvExport()) { "export data unavailable" }
                    checkNotNull(context.contentResolver.openOutputStream(uri)) { "output stream unavailable" }.use { os ->
                        os.write(csvData.toByteArray())
                    }
                }.onSuccess { AppLogger.i(requestId, "SettingsScreen", "requestCsvExport 成功") }
                    .onFailure { AppLogger.e(requestId, "SettingsScreen", "requestCsvExport 失败", it) }
                }
        } ?: AppLogger.i(requestId, "SettingsScreen", "requestCsvExport 取消")
    }

    val logLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        val requestId = pendingLogRequestId ?: AppLogger.generateRequestId()
        pendingLogRequestId = null
        uri?.let {
            scope.launch(Dispatchers.IO) {
                runCatching {
                    val logData = checkNotNull(bridge.prepareLogExport()) { "export data unavailable" }
                    checkNotNull(context.contentResolver.openOutputStream(uri)) { "output stream unavailable" }.use { os ->
                        os.write(logData.toByteArray())
                    }
                }.onSuccess { AppLogger.i(requestId, "SettingsScreen", "requestLogExport 成功") }
                    .onFailure { AppLogger.e(requestId, "SettingsScreen", "requestLogExport 失败", it) }
                }
        } ?: AppLogger.i(requestId, "SettingsScreen", "requestLogExport 取消")
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "设置",
                        fontWeight = FontWeight.Bold,
                        color = Morandi.TextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Morandi.BackgroundGray
                )
            )
        },
        containerColor = Morandi.BackgroundGray
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ===== 卡片 1: 记账设置 =====
            item {
                MorandiCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "记账设置",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Morandi.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "自动学习分类",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Morandi.TextPrimary
                            )
                            Switch(
                                checked = autoLearn,
                                onCheckedChange = { enabled ->
                                    scope.launch(Dispatchers.IO) {
                                        val requestId = AppLogger.generateRequestId()
                                        AppLogger.i(requestId, "SettingsScreen", "setAutoLearnEnabled 入口: enabled=$enabled")
                                        bridge.setAutoLearnEnabled(enabled, requestId)
                                        AppLogger.d(requestId, "SettingsScreen", "setAutoLearnEnabled 出口: 成功")
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Morandi.BrandPrimary,
                                    checkedTrackColor = Morandi.BrandPrimary.copy(alpha = 0.3f),
                                    uncheckedThumbColor = Morandi.TextSecondary,
                                    uncheckedTrackColor = Morandi.DividerColor
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "记账后自动记忆分类选择，提升下次匹配准确率",
                            style = MaterialTheme.typography.bodySmall,
                            color = Morandi.TextSecondary
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "付款后唤起记账",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Morandi.TextPrimary
                            )
                            Switch(
                                checked = quickRecord,
                                onCheckedChange = { enabled ->
                                    scope.launch(Dispatchers.IO) {
                                        val requestId = AppLogger.generateRequestId()
                                        AppLogger.i(requestId, "SettingsScreen", "setQuickRecordEnabled 入口: enabled=$enabled")
                                        bridge.setQuickRecordEnabled(enabled, requestId)
                                        AppLogger.d(requestId, "SettingsScreen", "setQuickRecordEnabled 出口: 成功")
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Morandi.BrandPrimary,
                                    checkedTrackColor = Morandi.BrandPrimary.copy(alpha = 0.3f),
                                    uncheckedThumbColor = Morandi.TextSecondary,
                                    uncheckedTrackColor = Morandi.DividerColor
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "微信/支付宝/云闪付付款成功后，自动唤起记账卡片（需开启通知监听与悬浮窗权限）",
                            style = MaterialTheme.typography.bodySmall,
                            color = Morandi.TextSecondary
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val requestId = AppLogger.generateRequestId()
                                    AppLogger.i(requestId, "SettingsScreen", "openNotificationSettings 入口")
                                    runCatching {
                                        context.startActivity(
                                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                        )
                                    }.onSuccess {
                                        AppLogger.d(requestId, "SettingsScreen", "openNotificationSettings 成功")
                                    }.onFailure {
                                        AppLogger.e(requestId, "SettingsScreen", "openNotificationSettings 失败", it)
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "通知监听权限",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Morandi.TextPrimary
                            )
                            if (notifGranted) {
                                Text(
                                    text = "已开启",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Morandi.TextIncome
                                )
                            } else {
                                Text(
                                    text = "去开启 →",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Morandi.BrandPrimary
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val requestId = AppLogger.generateRequestId()
                                    AppLogger.i(requestId, "SettingsScreen", "openOverlaySettings 入口")
                                    runCatching {
                                        context.startActivity(
                                            Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                android.net.Uri.parse("package:${context.packageName}")
                                            )
                                        )
                                    }.onSuccess {
                                        AppLogger.d(requestId, "SettingsScreen", "openOverlaySettings 成功")
                                    }.onFailure {
                                        AppLogger.e(requestId, "SettingsScreen", "openOverlaySettings 失败", it)
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "悬浮窗权限",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Morandi.TextPrimary
                            )
                            if (overlayGranted) {
                                Text(
                                    text = "已开启",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Morandi.TextIncome
                                )
                            } else {
                                Text(
                                    text = "去开启 →",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Morandi.BrandPrimary
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToMemoryMapping() }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "记忆与分类管理",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Morandi.TextPrimary
                            )
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Morandi.TextSecondary
                            )
                        }
                    }
                }
            }

            // ===== 卡片 2: 数据管理 =====
            item {
                MorandiCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "数据管理",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Morandi.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = {
                                val requestId = AppLogger.generateRequestId()
                                pendingCsvRequestId = requestId
                                AppLogger.i(requestId, "SettingsScreen", "requestCsvExport 入口")
                                csvLauncher.launch("记账数据_${System.currentTimeMillis()}.csv")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Morandi.BrandPrimary
                            )
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("导出 CSV 数据")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                val requestId = AppLogger.generateRequestId()
                                pendingLogRequestId = requestId
                                AppLogger.i(requestId, "SettingsScreen", "requestLogExport 入口")
                                logLauncher.launch("记账日志_${System.currentTimeMillis()}.log")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Morandi.BrandPrimary
                            )
                        ) {
                            Icon(Icons.Default.BugReport, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("导出日志文件")
                        }
                    }
                }
            }
        }
    }
}

private fun isNotificationAccessEnabled(context: Context): Boolean {
    val flat = android.provider.Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    ) ?: return false
    val pkg = context.packageName
    return flat.split(':').any { it.startsWith("$pkg/") || it == pkg }
}
