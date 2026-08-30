package com.aigrowth.os.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.accounting.app.AccountingApp
import com.accounting.app.log.AppLogger
import com.aigrowth.os.ui.common.WorkbenchCard
import com.aigrowth.os.ui.common.WorkbenchTopBar
import com.aigrowth.os.ui.theme.AccentGreen
import com.aigrowth.os.ui.theme.InkSecondary
import com.aigrowth.os.ui.theme.InkText
import com.aigrowth.os.ui.theme.ModuleBlue
import com.aigrowth.os.ui.theme.PaperBg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onNavigateToMemoryMapping: () -> Unit
) {
    val context = LocalContext.current
    val bridge = remember { AccountingApp.getBridge() }
    val scope = rememberCoroutineScope()

    var pendingCsvRequestId by remember { mutableStateOf<String?>(null) }
    var pendingLogRequestId by remember { mutableStateOf<String?>(null) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PaperBg)
    ) {
        WorkbenchTopBar(
            title = "设置",
            subtitle = "管理记忆分类与数据导出",
            icon = Icons.Default.Settings,
            iconTint = ModuleBlue
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ===== 卡片 1: 记忆与分类管理 =====
            item {
                WorkbenchCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "记忆与分类管理",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = InkText
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToMemoryMapping() }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "进入记忆与分类管理",
                            style = MaterialTheme.typography.bodyLarge,
                            color = InkText
                        )
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = InkSecondary
                        )
                    }
                }
            }

            // ===== 卡片 2: 数据管理 =====
            item {
                WorkbenchCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "数据管理",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = InkText
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
                            contentColor = AccentGreen
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
                            contentColor = AccentGreen
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
