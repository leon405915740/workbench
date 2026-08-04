package com.accounting.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.accounting.app.capture.model.PaymentInfo
import com.accounting.app.ui.MainViewModel
import com.accounting.app.ui.components.EditRecordDialog
import com.accounting.app.ui.model.AppTab
import com.accounting.app.ui.screens.ChatScreen
import com.accounting.app.ui.screens.DashboardScreen
import com.accounting.app.ui.screens.MemoryManageScreen
import com.accounting.app.ui.screens.SettingsScreen
import com.accounting.app.ui.theme.AccountingTheme
import com.accounting.app.ui.theme.NavActive
import com.accounting.app.ui.theme.NavInactive
import com.accounting.app.ui.theme.TextDelete
import com.accounting.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 单 Activity 入口，使用 Jetpack Compose 渲染界面。
 *
 * - AccountingTheme 提供配色与排版
 * - Scaffold + NavigationBar 构建底部三 Tab 框架
 * - 暂时只实现 ChatScreen，Dashboard/Settings 用占位 Text
 */
class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = AccountingApp.getInstance().appRepository
        viewModel = ViewModelProvider(
            this,
            MainViewModel.factory(repository)
        )[MainViewModel::class.java]

        setContent {
            AccountingTheme {
                MainScreen(viewModel)
            }
        }

        // 冷启动收到带 EXTRA_PAYMENT_INFO 的 Intent（App 被系统回收后重启的场景）
        handlePaymentIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // App 在后台时，Service startActivity 会走这条路径把记账 App 拉到前台
        handlePaymentIntent(intent)
    }

    /**
     * 从 Intent extra 取出 PaymentInfo，触发预填记账确认弹窗。
     * 来自 PaymentAccessibilityService 检测到付款成功页后的 startActivity 跳转。
     */
    private fun handlePaymentIntent(intent: Intent?) {
        val info: PaymentInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_PAYMENT_INFO, PaymentInfo::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_PAYMENT_INFO) as? PaymentInfo
        }
        info?.let { viewModel.onPaymentCapturedFromIntent(it) }
    }

    companion object {
        const val EXTRA_PAYMENT_INFO = "extra_payment_info"
    }
}

/**
 * 主框架：底部导航 + 当前 Tab 对应页面。
 */
@Composable
fun MainScreen(viewModel: MainViewModel) {
    // 注意：项目未引入 lifecycle-runtime-compose，这里使用 collectAsState
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 设置页内是否展示记忆管理子页面（独立状态，避免侵入 UiState）
    var showMemoryManage by rememberSaveable { mutableStateOf(false) }

    // 待写入的 CSV 内容，SAF 创建文件成功后用于写入
    var pendingCsvContent by rememberSaveable { mutableStateOf<String?>(null) }
    // 待写入的日志内容，SAF 创建文件成功后用于写入
    var pendingLogContent by rememberSaveable { mutableStateOf<String?>(null) }
    // 删除账单二次确认：保存待删除的 recordId（非空时显示确认弹窗）
    var deleteConfirmRecordId by rememberSaveable { mutableStateOf<Long?>(null) }

    // SAF 创建文件 launcher，文件名格式：记账导出_YYYYMMDD_HHMMSS.csv
    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val content = pendingCsvContent
        if (uri != null && content != null) {
            // 写入 CSV 内容（CSV 字符串本身已带 UTF-8 BOM）
            val writeOk = try {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(content.toByteArray(Charsets.UTF_8))
                }
                true
            } catch (e: Exception) {
                false
            }
            val msg = if (writeOk) "导出成功" else "导出失败"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
        // 无论成功失败，都清空待写入内容和 UiState 中的导出数据
        pendingCsvContent = null
        viewModel.clearCsvExportData()
    }

    // 监听 csvExportData：非空时启动 SAF 创建文件
    LaunchedEffect(uiState.csvExportData) {
        val content = uiState.csvExportData
        if (content != null) {
            pendingCsvContent = content
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(Date())
            createCsvLauncher.launch("记账导出_$timeStamp.csv")
        }
    }

    // SAF 创建日志文件 launcher
    val createLogLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        val content = pendingLogContent
        if (uri != null && content != null) {
            val writeOk = try {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(content.toByteArray(Charsets.UTF_8))
                }
                true
            } catch (e: Exception) {
                false
            }
            Toast.makeText(context, if (writeOk) "日志导出成功" else "日志导出失败", Toast.LENGTH_SHORT).show()
        }
        pendingLogContent = null
        viewModel.clearLogExportData()
    }

    // 监听 logExportData：非空时启动 SAF 创建日志文件
    LaunchedEffect(uiState.logExportData) {
        val content = uiState.logExportData
        if (content != null) {
            pendingLogContent = content
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(Date())
            createLogLauncher.launch("记账日志_$timeStamp.log")
        }
    }

    // Toast 处理：监听 uiState.toast，显示后清空
    LaunchedEffect(uiState.toast) {
        uiState.toast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    Scaffold(
        bottomBar = {
            BottomNavBar(
                currentTab = uiState.currentTab,
                onTabSelected = { tab ->
                    // 如果在子页面（如记忆管理），先关闭子页面再回到一级页面
                    if (showMemoryManage) {
                        showMemoryManage = false
                    }
                    viewModel.switchTab(tab)
                }
            )
        }
    ) { padding ->
        when (uiState.currentTab) {
            AppTab.CHAT -> Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                ChatScreen(
                    uiState = uiState,
                    onInputChange = viewModel::updateInputText,
                    onSend = viewModel::sendMessage,
                    onEditRecord = viewModel::openEditDialog,
                    onDelete = viewModel::deleteRecord,
                    onManualEntry = { viewModel.openManualEntry(it) },
                    onLearnKeyword = viewModel::openLearnDialog,
                    onDismissLearn = viewModel::dismissLearnDialog,
                    onConfirmLearn = viewModel::confirmLearnKeyword
                )
            }
            AppTab.DASHBOARD -> Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                DashboardScreen(
                    uiState = uiState,
                    onSwitchTab = viewModel::switchDashTab,
                    onEditRecord = viewModel::openEditDialogFromDashboard
                )
            }
            AppTab.SETTINGS -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    if (showMemoryManage) {
                        MemoryManageScreen(
                            uiState = uiState,
                            onLoadMemories = viewModel::loadMemories,
                            onAddMemory = viewModel::addMemory,
                            onDeleteMemory = viewModel::deleteMemory,
                            onClearAll = viewModel::clearAllMemories,
                            onBack = { showMemoryManage = false },
                            onSearch = viewModel::onMemorySearch,
                            onToggleExpand = viewModel::toggleCategoryExpand,
                            onSourceFilter = viewModel::setMemorySourceFilter
                        )
                    } else {
                        SettingsScreen(
                            uiState = uiState,
                            onSaveApiKey = viewModel::saveApiKey,
                            onManageMemories = {
                                showMemoryManage = true
                                viewModel.loadMemories("expense")
                            },
                            onRestoreMemories = viewModel::restoreDefaultMemories,
                            onExportCsv = viewModel::prepareCsvExport,
                            onExportLog = viewModel::prepareLogExport,
                            onToggleAutoLearn = viewModel::toggleAutoLearn
                        )
                    }
                }
            }
        }
    }

    // 编辑/新建账单弹窗（双模式：recordId=null=新建，非空=编辑）
    uiState.showEditDialog?.let { data ->
        EditRecordDialog(
            data = data,
            onSubmit = { updatedData ->
                viewModel.submitManualEntry(
                    updatedData.type, updatedData.amount, updatedData.category,
                    updatedData.merchant, updatedData.time, updatedData.note,
                    updatedData.rawInput
                )
            },
            onEditConfirm = viewModel::confirmEditRecord,
            onDismiss = viewModel::dismissEditDialog,
            onDeleteRequest = { deleteConfirmRecordId = data.recordId }
        )
    }

    // 删除账单二次确认
    deleteConfirmRecordId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteConfirmRecordId = null },
            title = { Text("删除账单") },
            text = { Text("确定删除这条记录吗？删除后不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    val type = uiState.showEditDialog?.type ?: "expense"
                    viewModel.deleteRecord(id, type)
                    viewModel.dismissEditDialog()
                    deleteConfirmRecordId = null
                }) { Text("删除", color = TextDelete) }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmRecordId = null }) {
                    Text("取消", color = TextSecondary)
                }
            }
        )
    }
}

/**
 * 底部导航栏：记账 / 统计 / 设置。
 *
 * 图标使用 Compose 内置 Icons.Outlined 系列（core 集合可用图标）。
 * 选中态 NavActive（绿），未选中态 NavInactive（灰）。
 */
@Composable
private fun BottomNavBar(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) {
    val items = listOf(
        BottomNavItem(AppTab.CHAT, "记账", Icons.Outlined.Edit),
        BottomNavItem(AppTab.DASHBOARD, "统计", Icons.Outlined.List),
        BottomNavItem(AppTab.SETTINGS, "设置", Icons.Outlined.Settings)
    )

    NavigationBar {
        items.forEach { item ->
            val isSelected = currentTab == item.tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(item.tab) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (isSelected) NavActive else NavInactive
                    )
                },
                label = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(width = 16.dp, height = 3.dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(NavActive)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                        Text(
                            text = item.label,
                            color = if (isSelected) NavActive else NavInactive
                        )
                    }
                },
                alwaysShowLabel = true
            )
        }
    }
}

/** 底部导航项数据 */
private data class BottomNavItem(
    val tab: AppTab,
    val label: String,
    val icon: ImageVector
)
