package com.accounting.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.List
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.accounting.app.log.AppLogger
import com.accounting.app.ui.MainViewModel
import com.accounting.app.ui.components.EditRecordDialog
import com.accounting.app.ui.model.AppTab
import com.accounting.app.ui.screens.ChatScreen
import com.accounting.app.ui.screens.DashboardScreen
import com.accounting.app.ui.theme.AccountingTheme
import com.accounting.app.ui.theme.CardWhite
import com.accounting.app.ui.theme.NavActive
import com.accounting.app.ui.theme.NavInactive
import com.accounting.app.ui.theme.TextDelete
import com.accounting.app.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    companion object {
        const val EXTRA_QUICK_PAYMENT_AMOUNT = "extra_quick_payment_amount"
        const val EXTRA_QUICK_PAYMENT_MERCHANT = "extra_quick_payment_merchant"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = AccountingApp.getInstance().appRepository
        viewModel = ViewModelProvider(
            this,
            MainViewModel.factory(repository)
        )[MainViewModel::class.java]

        handlePaymentIntent(intent)

        setContent {
            AccountingTheme {
                MainScreen(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePaymentIntent(intent)
    }

    /**
     * 处理付款通知唤起：带金额则预填记账弹窗，用后即清 extra 避免配置重建重复弹出。
     */
    private fun handlePaymentIntent(intent: Intent?) {
        val amount = intent?.getLongExtra(EXTRA_QUICK_PAYMENT_AMOUNT, -1L) ?: -1L
        if (amount <= 0) return
        val merchant = intent?.getStringExtra(EXTRA_QUICK_PAYMENT_MERCHANT)
        val requestId = AppLogger.generateRequestId()
        AppLogger.d(requestId, "付款唤起", "MainActivity 收到唤起: amount=${amount}分, merchant=$merchant")
        intent?.removeExtra(EXTRA_QUICK_PAYMENT_AMOUNT)
        intent?.removeExtra(EXTRA_QUICK_PAYMENT_MERCHANT)
        viewModel.openPaymentQuickEntry(amount, merchant)
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel, showBottomNavigation: Boolean = true) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var deleteConfirmRecordId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(uiState.toast) {
        uiState.toast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    val imeVisible = androidx.compose.foundation.layout.WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0
    Scaffold(
        modifier = Modifier.imePadding(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomNavigation && !imeVisible) {
            BottomNavBar(
                currentTab = uiState.currentTab,
                onTabSelected = viewModel::switchTab
            )
            }
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
                    onEditRecord = viewModel::openEditDialogFromDashboard,
                    onDashInputChange = viewModel::updateDashboardInput,
                    onDashSend = viewModel::sendDashboardQuery
                )
            }
        }
    }

    uiState.showEditDialog?.let { data ->
        EditRecordDialog(
            data = data,
            onSubmit = { updatedData ->
                viewModel.submitManualEntry(
                    updatedData.type, updatedData.amount, updatedData.category,
                    updatedData.merchant, updatedData.time, updatedData.note,
                    updatedData.rawInput,
                    updatedData.pendingRequestId
                )
            },
            onEditConfirm = viewModel::confirmEditRecord,
            onDismiss = viewModel::dismissEditDialog,
            onDeleteRequest = { deleteConfirmRecordId = data.recordId }
        )
    }

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

@Composable
private fun BottomNavBar(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) {
    val items = listOf(
        BottomNavItem(AppTab.CHAT, "记账", Icons.Outlined.Edit),
        BottomNavItem(AppTab.DASHBOARD, "统计", Icons.Outlined.List)
    )

    NavigationBar(
        containerColor = CardWhite,
        tonalElevation = 0.dp
    ) {
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

private data class BottomNavItem(
    val tab: AppTab,
    val label: String,
    val icon: ImageVector
)
