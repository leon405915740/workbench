package com.accounting.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accounting.app.ui.MainViewModel
import com.accounting.app.ui.components.EditRecordDialog
import com.accounting.app.ui.components.getCategoryEmoji
import com.accounting.app.ui.model.ChatMessage
import com.accounting.app.ui.model.DashTab
import com.accounting.app.ui.model.RecentRecord
import com.accounting.app.ui.model.UiState
import com.accounting.app.ui.theme.BackgroundGray
import com.accounting.app.ui.theme.CardWhite
import com.accounting.app.ui.theme.DividerColor
import com.accounting.app.ui.theme.TextAmount
import com.accounting.app.ui.theme.TextDelete
import com.accounting.app.ui.theme.TextIncome
import com.accounting.app.ui.theme.TextPrimary
import com.accounting.app.ui.theme.TextSecondary
import com.accounting.app.ui.theme.WeChatGreen
import com.accounting.app.util.AmountUtils
import com.accounting.app.util.TimeUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 财务主布局：把原有记账能力在单一响应式主区中重新组织，替代内部底部双 Tab。
 *
 * 结构（自上而下）：
 * 1. 顶部概览（收入 / 支出 / 结余 / 笔数）
 * 2. 工具栏（搜索 / 新建 / AI 记账）
 * 3. 账单列表（收支合并，按时间倒序，点击编辑、删除）
 * 4. 详细统计（分类占比 + 近 7 日趋势 + 收支概况）
 * 5. 统计问答（保留原有 chatQuery）
 *
 * 数据与写入全部沿用 [MainViewModel] 与现有 Repository，不建立第二套账本。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(
    viewModel: MainViewModel,
    openAiEntry: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAiSheet by remember { mutableStateOf(false) }
    var deleteConfirm by remember { mutableStateOf<Pair<Long, String>?>(null) }

    // 首页「记一笔」跳转进入时，自动弹出 AI 录入
    LaunchedEffect(openAiEntry) {
        if (openAiEntry) showAiSheet = true
    }

    val filtered = rememberFilteredRecords(uiState.financeRecords, uiState.financeSearchQuery)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "overview") { FinanceOverview(uiState.financeRecords) }
                item(key = "toolbar") {
                    FinanceToolbar(
                        uiState = uiState,
                        onSearchChange = viewModel::updateFinanceSearch,
                        onSendQuery = {
                            viewModel.updateDashboardInput(uiState.financeSearchQuery)
                            viewModel.sendDashboardQuery()
                        },
                        onAiEntry = { showAiSheet = true }
                    )
                }
                if (uiState.dashboardMessages.isNotEmpty() || uiState.dashboardIsLoading) {
                    item(key = "qa-inline") { FinanceQAResultInline(uiState) }
                }
                item(key = "list-title") {
                    Text(
                        text = "全部记录",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                if (filtered.isEmpty()) {
                    item(key = "empty") { EmptyState(uiState.financeSearchQuery.isNotBlank()) }
                } else {
                    items(filtered, key = { "${it.type}-${it.id}" }) { record ->
                        FinanceRecordItem(record = record, onClick = { viewModel.openEditDialogFromDashboard(record) })
                    }
                }

                item(key = "stats") {
                    FinanceStatsSection(
                        uiState = uiState,
                        onSwitchDashTab = viewModel::switchDashTab
                    )
                }
            }
        }
    }

    if (showAiSheet) {
        AiEntrySheet(
            onDismiss = { showAiSheet = false },
            onAiSend = { text ->
                showAiSheet = false
                viewModel.submitAiEntry(text)
            },
            onManualEntry = {
                showAiSheet = false
                viewModel.openManualEntry()
            }
        )
    }

    // 编辑 / 新建 / AI 确认弹窗（沿用原有 EditRecordDialog）
    uiState.showEditDialog?.let { data ->
        EditRecordDialog(
            data = data,
            onSubmit = { updatedData ->
                viewModel.submitManualEntry(
                    updatedData.type, updatedData.amount, updatedData.category,
                    updatedData.merchant, updatedData.time, updatedData.note,
                    updatedData.rawInput, updatedData.pendingRequestId,
                    updatedData.attachmentPath
                )
            },
            onEditConfirm = viewModel::confirmEditRecord,
            onDismiss = viewModel::dismissEditDialog,
            onDeleteRequest = { deleteConfirm = (data.recordId ?: -1L) to data.type }
        )
    }

    deleteConfirm?.let { (id, type) ->
        AlertDialog(
            onDismissRequest = { deleteConfirm = null },
            title = { Text("删除账单", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
            text = { Text("确定删除这条记录吗？删除后不可恢复。", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRecord(id, type)
                    viewModel.dismissEditDialog()
                    deleteConfirm = null
                }) { Text("删除", color = TextDelete) }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirm = null }) {
                    Text("取消", color = TextSecondary)
                }
            }
        )
    }

    // 关键词学习确认弹窗
    uiState.showLearnDialog?.let { dialog ->
        AlertDialog(
            onDismissRequest = viewModel::dismissLearnDialog,
            title = { Text("保存分类记忆", fontWeight = FontWeight.SemiBold, color = TextPrimary) },
            text = {
                Text(
                    text = "将「${dialog.category}」与关键词「${dialog.triggerWord}」关联，下次自动识别。",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmLearnKeyword(dialog.triggerWord.trim(), dialog.type, dialog.category) }) {
                    Text("保存", color = WeChatGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissLearnDialog) {
                    Text("忽略", color = TextSecondary)
                }
            }
        )
    }
}

// ===================== 顶部概览 =====================

@Composable
private fun FinanceOverview(records: List<RecentRecord>) {
    val totalIncome = records.filter { it.type == "income" }.sumOf { it.amount }
    val totalExpense = records.filter { it.type == "expense" }.sumOf { it.amount }
    val balance = totalIncome - totalExpense

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp), clip = false)
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        OverviewMini("收入", AmountUtils.fenToYuanWithSymbol(totalIncome), TextIncome)
        OverviewMini("支出", AmountUtils.fenToYuanWithSymbol(totalExpense), TextAmount)
        OverviewMini("结余", AmountUtils.fenToYuanWithSymbol(balance), if (balance >= 0) TextIncome else TextDelete)
        OverviewMini("笔数", "${records.size}", TextPrimary)
    }
}

@Composable
private fun OverviewMini(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 12.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            maxLines = 1
        )
    }
}

// ===================== 工具栏 =====================

@Composable
private fun FinanceToolbar(
    uiState: UiState,
    onSearchChange: (String) -> Unit,
    onSendQuery: () -> Unit,
    onAiEntry: () -> Unit
) {
    val query = uiState.financeSearchQuery
    val sendEnabled = query.isNotBlank() && !uiState.dashboardIsLoading
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp), clip = false)
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .padding(12.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("搜索商户、分类、备注…", fontSize = 14.sp, color = TextSecondary) },
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = "搜索", tint = TextSecondary, modifier = Modifier.size(20.dp))
            },
            trailingIcon = {
                IconButton(onClick = onSendQuery, enabled = sendEnabled) {
                    Icon(
                        Icons.Filled.Send,
                        contentDescription = "发送问题",
                        tint = if (sendEnabled) WeChatGreen else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = BackgroundGray,
                unfocusedContainerColor = BackgroundGray,
                focusedIndicatorColor = WeChatGreen,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = onAiEntry,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = WeChatGreen)
        ) {
            Text("记一笔", color = CardWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ===================== 账单列表项 =====================

@Composable
private fun FinanceRecordItem(record: RecentRecord, onClick: () -> Unit) {
    val isIncome = record.type == "income"
    val amountColor = if (isIncome) TextIncome else TextAmount
    val amountText = if (isIncome) {
        "+${AmountUtils.fenToYuanWithSymbol(record.amount)}"
    } else {
        AmountUtils.fenToYuanWithSymbol(record.amount)
    }

    val infoText = buildString {
        record.merchant?.takeIf { it.isNotBlank() }?.let { append(it).append(" · ") }
        append(TimeUtils.formatTimeRelative(record.time))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp), clip = false)
            .clip(RoundedCornerShape(12.dp))
            .background(CardWhite)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${getCategoryEmoji(record.category, record.type)} ${record.category}",
                fontSize = 15.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = infoText, fontSize = 12.sp, color = TextSecondary)
            record.note?.takeIf { it.isNotBlank() }?.let { note ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = note, fontSize = 12.sp, color = TextSecondary, maxLines = 1)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = amountText,
            fontSize = 16.sp,
            color = amountColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun EmptyState(isSearch: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "📊", fontSize = 40.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (isSearch) "没有匹配的记录" else "暂无记录，点「记一笔」添加第一笔",
                fontSize = 14.sp,
                color = TextPrimary
            )
        }
    }
}

// ===================== 详细统计 =====================

@Composable
private fun FinanceStatsSection(uiState: UiState, onSwitchDashTab: (DashTab) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 分类占比
        CategoryStatsCard(uiState, onSwitchDashTab)

        // 近 7 日趋势
        TrendCard(records = uiState.financeRecords)

        // 收支概况
        SummaryDetailCard(uiState)
    }
}

@Composable
private fun CategoryStatsCard(uiState: UiState, onSwitchDashTab: (DashTab) -> Unit) {
    val isExpense = uiState.dashTab == DashTab.EXPENSE
    val monthTotal = if (isExpense) uiState.monthExpense else uiState.monthIncome

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp), clip = false)
            .clip(RoundedCornerShape(12.dp))
            .background(CardWhite)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "分类占比",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(BackgroundGray)
                    .padding(2.dp)
            ) {
                MiniTab("支出", isExpense) { onSwitchDashTab(DashTab.EXPENSE) }
                MiniTab("收入", !isExpense) { onSwitchDashTab(DashTab.INCOME) }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        val top5 = uiState.categoryStats.sortedByDescending { it.totalAmount }.take(5)
        if (top5.isEmpty() || monthTotal <= 0) {
            Text(
                text = "暂无分类数据",
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            top5.forEachIndexed { index, stat ->
                if (index > 0) Spacer(modifier = Modifier.height(10.dp))
                CategoryRatioRow(
                    name = stat.category,
                    amount = stat.totalAmount,
                    total = monthTotal,
                    type = if (isExpense) "expense" else "income"
                )
            }
        }
    }
}

@Composable
private fun MiniTab(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) CardWhite else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = if (selected) WeChatGreen else TextSecondary,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun CategoryRatioRow(name: String, amount: Long, total: Long, type: String) {
    val progress = if (total > 0) (amount.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
    val percent = (progress * 100).toInt()

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = getCategoryEmoji(name, type), fontSize = 15.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = name,
                fontSize = 14.sp,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = AmountUtils.fenToYuanWithSymbol(amount),
                fontSize = 13.sp,
                color = TextAmount,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$percent%",
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.End,
                modifier = Modifier.width(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (type == "expense") TextAmount else TextIncome,
            trackColor = DividerColor
        )
    }
}

@Composable
private fun TrendCard(records: List<RecentRecord>) {
    val trend = rememberTrend(records)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp), clip = false)
            .clip(RoundedCornerShape(12.dp))
            .background(CardWhite)
            .padding(16.dp)
    ) {
        Text(
            text = "近 7 日支出趋势",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(14.dp))

        val maxExpense = trend.maxOfOrNull { it.expense } ?: 0L
        Row(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            trend.forEach { day ->
                val barHeight = if (maxExpense > 0) (day.expense.toFloat() / maxExpense.toFloat() * 80.dp.value) else 0f
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    if (day.expense > 0) {
                        Text(
                            text = AmountUtils.fenToYuan(day.expense),
                            fontSize = 9.sp,
                            color = TextSecondary,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height(androidx.compose.ui.unit.Dp(barHeight.takeIf { it > 2f } ?: 2f))
                            .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                            .background(TextAmount)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = day.label, fontSize = 10.sp, color = TextSecondary)
                }
            }
        }
    }
}

private data class TrendDay(val label: String, val expense: Long)

@Composable
private fun rememberTrend(records: List<RecentRecord>): List<TrendDay> {
    return remember(records) {
        val fmt = SimpleDateFormat("MM-dd", Locale.CHINA)
        (6 downTo 0).map { offset ->
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -offset)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val dayStart = cal.timeInMillis
            val dayEnd = dayStart + 86_400_000L
            val expense = records.filter { it.type == "expense" && it.time in dayStart until dayEnd }.sumOf { it.amount }
            TrendDay(label = fmt.format(Date(dayStart)), expense = expense)
        }
    }
}

@Composable
private fun SummaryDetailCard(uiState: UiState) {
    val totalIncome = uiState.financeRecords.filter { it.type == "income" }.sumOf { it.amount }
    val totalExpense = uiState.financeRecords.filter { it.type == "expense" }.sumOf { it.amount }
    val balance = totalIncome - totalExpense
    val count = uiState.financeRecords.size

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp), clip = false)
            .clip(RoundedCornerShape(12.dp))
            .background(CardWhite)
            .padding(16.dp)
    ) {
        Text(
            text = "收支概况",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        DetailRow("总收入", AmountUtils.fenToYuanWithSymbol(totalIncome), TextIncome)
        DetailRow("总支出", AmountUtils.fenToYuanWithSymbol(totalExpense), TextAmount)
        DetailRow("净结余", AmountUtils.fenToYuanWithSymbol(balance), if (balance >= 0) TextIncome else TextDelete)
        DetailRow("今日支出", AmountUtils.fenToYuanWithSymbol(uiState.todayExpense), TextAmount)
        DetailRow("总笔数", "$count 笔", TextPrimary)
    }
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(text = value, fontSize = 14.sp, color = valueColor, fontWeight = FontWeight.Medium)
    }
}

// ===================== 问答结果内联区 =====================

@Composable
private fun FinanceQAResultInline(uiState: UiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp), clip = false)
            .clip(RoundedCornerShape(12.dp))
            .background(CardWhite)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        uiState.dashboardMessages.takeLast(6).forEach { message ->
            when (message) {
                is ChatMessage.UserMessage -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 260.dp)
                                .clip(RoundedCornerShape(14.dp, 4.dp, 14.dp, 14.dp))
                                .background(WeChatGreen)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(message.text, fontSize = 13.sp, color = Color.White)
                        }
                    }
                }
                is ChatMessage.AiTextMessage -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 280.dp)
                                .clip(RoundedCornerShape(4.dp, 14.dp, 14.dp, 14.dp))
                                .background(BackgroundGray)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(message.content, fontSize = 13.sp, color = TextPrimary)
                        }
                    }
                }
                else -> {}
            }
        }
        if (uiState.dashboardIsLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                color = WeChatGreen,
                trackColor = DividerColor
            )
        }
    }
}

// ===================== AI 录入弹层 =====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiEntrySheet(
    onDismiss: () -> Unit,
    onAiSend: (String) -> Unit,
    onManualEntry: () -> Unit
) {
    var text by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .imePadding()
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "记一笔",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "输入一句话，自动识别金额与分类，例如：午饭 25 元麦当劳",
                fontSize = 13.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("例如：午饭 25 元", fontSize = 14.sp, color = TextSecondary) },
                singleLine = true,
                leadingIcon = {
                    IconButton(
                        onClick = {
                            onDismiss()
                            onManualEntry()
                        }
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "手动填写", tint = WeChatGreen, modifier = Modifier.size(20.dp))
                    }
                },
                trailingIcon = {
                    IconButton(
                        onClick = { onAiSend(text) },
                        enabled = text.isNotBlank()
                    ) {
                        Icon(
                            Icons.Filled.Send,
                            contentDescription = "发送",
                            tint = if (text.isNotBlank()) WeChatGreen else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = CardWhite,
                    unfocusedContainerColor = CardWhite,
                    focusedIndicatorColor = WeChatGreen,
                    unfocusedIndicatorColor = DividerColor
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ===================== 工具 =====================

@Composable
private fun rememberFilteredRecords(records: List<RecentRecord>, query: String): List<RecentRecord> {
    return remember(records, query) {
        val q = query.trim()
        if (q.isBlank()) records
        else records.filter { r ->
            r.category.contains(q, ignoreCase = true) ||
                r.merchant?.contains(q, ignoreCase = true) == true ||
                r.note?.contains(q, ignoreCase = true) == true ||
                r.rawInput.contains(q, ignoreCase = true)
        }
    }
}