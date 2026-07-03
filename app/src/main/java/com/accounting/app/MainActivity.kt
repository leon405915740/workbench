package com.accounting.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import com.accounting.app.data.repository.AppRepository
import com.accounting.app.ui.MainViewModel
import com.accounting.app.ui.components.CategoryPicker
import com.accounting.app.ui.components.ManualEntryDialog
import com.accounting.app.ui.model.AppTab
import com.accounting.app.ui.screens.ChatScreen
import com.accounting.app.ui.screens.DashboardScreen
import com.accounting.app.ui.screens.MemoryManageScreen
import com.accounting.app.ui.screens.SettingsScreen
import com.accounting.app.ui.theme.AccountingTheme
import com.accounting.app.ui.theme.NavActive
import com.accounting.app.ui.theme.NavInactive
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = AppRepository(applicationContext)
        val viewModel = ViewModelProvider(
            this,
            MainViewModel.factory(repository)
        )[MainViewModel::class.java]

        setContent {
            AccountingTheme {
                MainScreen(viewModel)
            }
        }
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
    var showMemoryManage by remember { mutableStateOf(false) }

    // 待写入的 CSV 内容，SAF 创建文件成功后用于写入
    var pendingCsvContent by remember { mutableStateOf<String?>(null) }

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
                onTabSelected = viewModel::switchTab
            )
        }
    ) { padding ->
        when (uiState.currentTab) {
            AppTab.CHAT -> Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                ChatScreen(
                    uiState = uiState,
                    onInputChange = viewModel::updateInputText,
                    onSend = viewModel::sendMessage,
                    onEditCategory = viewModel::openEditDialog,
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
                    onDeleteRecord = viewModel::deleteRecord
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
                            onToggleExpand = viewModel::toggleCategoryExpand
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
                            onToggleAutoLearn = viewModel::toggleAutoLearn
                        )
                    }
                }
            }
        }
    }

    // 修改分类弹窗（使用两级分类选择器，type 固定不可切换）
    uiState.showEditDialog?.let { dialog ->
        CategoryPicker(
            type = dialog.type,
            initialCategory = dialog.category,
            initialSubcategory = dialog.subcategory,
            onConfirm = viewModel::confirmEditCategory,
            onDismiss = viewModel::dismissEditDialog
        )
    }

    // 手动记账弹窗
    uiState.showManualEntry?.let { entry ->
        ManualEntryDialog(
            prefillNote = entry.prefillNote,
            onConfirm = { type, amount, category, subcategory, merchant, time, note ->
                // rawInput 优先用备注（通常包含失败解析的原始输入），为空则用默认文案
                val rawInput = note ?: "手动记账"
                viewModel.submitManualEntry(
                    type, amount, category, subcategory, merchant, time, note, rawInput
                )
            },
            onDismiss = viewModel::dismissManualEntry
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
                    Text(
                        text = item.label,
                        color = if (isSelected) NavActive else NavInactive
                    )
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
