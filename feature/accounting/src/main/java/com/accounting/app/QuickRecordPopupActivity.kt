package com.accounting.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModelProvider
import com.accounting.app.log.AppLogger
import com.accounting.app.ui.MainViewModel
import com.accounting.app.ui.components.EditRecordDialog
import com.accounting.app.ui.theme.AccountingTheme

/**
 * 付款通知唤起的"快捷记账小卡片"Popup Activity。
 *
 * 设计原则（ponytail）：
 * - 仅做一件事：一次性、窄卡片的快捷记账提交；不承载聊天列表、统计 Tab、底部导航。
 * - 窗口使用 Manifest 里的 Theme.AccountingPopup（单层透明 + 暗化层），内部用 Compose 的 Dialog
 *   承载 EditRecordDialog；**不再叠加 Dialog 父主题**，避免黑边/背景双重嵌套问题。
 * - 启动模式：默认 standard（不使用 singleTop / onNewIntent）。每条通知独立实例，降低
 *   多条连续支付通知的状态管理复杂度；如未来出现重复实例问题，再升级到 singleTop。
 * - 生命周期防重入：savedInstanceState==null 才执行 openPaymentQuickEntry，配合
 *   本地 consumed 标记 与 removeExtra 三重防御，避免旋转屏幕重复打开。
 * - 提交成功判据：仅在 [MainViewModel.submitManualEntry] 的 onSaved(recordId) 回调中 finish。
 *   失败 / 取消都不会走该路径，避免"把关闭当成成功"。
 */
class QuickRecordPopupActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    /**
     * 一次性消费标记，配合 [savedInstanceState] + removeExtra 做三重幂等守卫：
     * 防止配置重建、系统返回栈复用等情况下重复调用 openPaymentQuickEntry。
     */
    private var consumed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = AccountingApp.getInstance().appRepository
        viewModel = ViewModelProvider(
            this,
            MainViewModel.factory(repository)
        )[MainViewModel::class.java]

        // 仅"首次创建"且尚未消费时解析 intent 并打开预填弹窗；
        // 配置重建 (savedInstanceState != null) 由 ViewModelProvider 保留 ViewModel 状态，
        // Compose Dialog 会根据 showEditDialog 自动重绘，无需再打开一次。
        if (savedInstanceState == null && !consumed) {
            val amount = intent.getLongExtra(MainActivity.EXTRA_QUICK_PAYMENT_AMOUNT, -1L)
            val merchant = intent.getStringExtra(MainActivity.EXTRA_QUICK_PAYMENT_MERCHANT)
            if (amount <= 0L) {
                // 不是合法快捷支付入口：直接关闭，不渲染空白透明 Activity。
                AppLogger.w(
                    AppLogger.generateRequestId(),
                    NODE,
                    "Popup 启动缺少有效金额（$amount），立即 finish，merchant=$merchant"
                )
                finish()
                return
            }
            viewModel.openPaymentQuickEntry(amount, merchant)
            consumed = true
            intent.removeExtra(MainActivity.EXTRA_QUICK_PAYMENT_AMOUNT)
            intent.removeExtra(MainActivity.EXTRA_QUICK_PAYMENT_MERCHANT)
        }

        setContent {
            AccountingTheme {
                // MaterialTheme 保证 isSystemWindowInsetEdge 等默认环境就绪（否则无影响）。
                MaterialTheme(content = {
                    QuickRecordPopupRoot(
                        viewModel = viewModel,
                        onFinish = { finish() }
                    )
                })
            }
        }
    }

    companion object {
        private const val NODE = "快捷记账卡片"
    }
}

@Composable
private fun QuickRecordPopupRoot(
    viewModel: MainViewModel,
    onFinish: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    // toast：成功 / 失败（"记账失败"/附件失败）统一用系统 Toast；
    // 成功 toast 之后会立刻 onSaved→finish，用户仍能短暂看到（或下一帧看到）。
    LaunchedEffect(uiState.toast) {
        uiState.toast?.let { text ->
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // error：长时 Toast + 不覆盖 toast；不引入额外 clearError 方法（ponytail：
    // Activity 生命周期极短，下一次用户交互会被新 state 覆盖）。
    uiState.error?.let { errorText ->
        LaunchedEffect(errorText) {
            Toast.makeText(context, "错误：$errorText", Toast.LENGTH_LONG).show()
        }
    }

    val dialogData = uiState.showEditDialog
    if (dialogData != null) {
        // 外层半透明罩：窗口主题已经有 backgroundDim（系统级），但再叠加一层可控的黑色蒙版
        // 能让暗度在不同机型/OEM 上更一致。alpha=0.18（0.32 以内，符合 T3R5 的 ≤0.4 要求）。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            EditRecordDialog(
                data = dialogData,
                onSubmit = { updated ->
                    // 只有 submitManualEntry 的 onSaved 成功回调才真正 finish。
                    // 提交失败时 ViewModel 会写 toast / error，showEditDialog 保留，
                    // 用户可修改后再次提交。
                    viewModel.submitManualEntry(
                        type = updated.type,
                        amount = updated.amount,
                        category = updated.category,
                        merchant = updated.merchant,
                        time = updated.time,
                        note = updated.note,
                        rawInput = updated.rawInput,
                        pendingRequestId = updated.pendingRequestId,
                        attachmentPath = updated.attachmentPath,
                        onSaved = { onFinish() }
                    )
                },
                onEditConfirm = {
                    // popup 永远是新建模式（recordId=null），该回调理论不会触发。
                    // 留空实现即可，避免意外变更。
                },
                onDismiss = {
                    // 用户点击关闭按钮 / 外部 / 系统返回 → 关闭弹窗并 finish Activity。
                    // 注意：配置重建（旋转）时 Compose Dialog 也会 dismiss，但此时
                    // Activity 会立刻重建，ViewModel 的 showEditDialog 仍非空，
                    // 重建后上面的 dialogData != null 会再次渲染，故不会"一转就消失"。
                    viewModel.dismissEditDialog()
                    onFinish()
                },
                onDeleteRequest = {
                    // popup 永远是新建模式，没有删除按钮，留空实现。
                },
                widthFraction = 0.92f
            )
        }
    } else {
        // showEditDialog == null：说明"已经取消/已完成"。
        // 完成时 onSaved 已经 finish；取消时 onDismiss 已经 finish；
        // 如果意外落在这个分支（极罕见），直接 finish 防止透明 Activity 悬挂。
        LaunchedEffect(Unit) { onFinish() }
    }
}
