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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accounting.app.ui.model.DashTab
import com.accounting.app.ui.model.RecentRecord
import com.accounting.app.ui.model.UiState
import com.accounting.app.ui.theme.BackgroundGray
import com.accounting.app.ui.theme.CardWhite
import com.accounting.app.ui.theme.WeChatGreen
import com.accounting.app.ui.theme.WeChatGreenDark
import com.accounting.app.ui.theme.WeChatGreenLight
import com.accounting.app.ui.theme.TextAmount
import com.accounting.app.ui.theme.TextDelete
import com.accounting.app.ui.theme.TextIncome
import com.accounting.app.ui.theme.TextPrimary
import com.accounting.app.ui.theme.TextSecondary
import com.accounting.app.util.AmountUtils
import com.accounting.app.util.TimeUtils
import java.util.Calendar

/**
 * Dashboard 统计页面。
 *
 * 结构：
 * 1. 顶部收支切换 Tab（支出 / 收入）
 * 2. 总览卡片（本月总额 + 今日 + 日均，渐变背景）
 * 3. 分类占比（Top5，进度条 + 百分比）
 * 4. 最近记录（前 10 条，点击弹出删除弹窗）
 *
 * 数据由 UiState 驱动，dashTab 决定展示支出还是收入。
 */
@Composable
fun DashboardScreen(
    uiState: UiState,
    onSwitchTab: (DashTab) -> Unit,
    onDeleteRecord: (Long, String) -> Unit
) {
    // 待删除记录，null 时无弹窗
    var pendingDelete by remember { mutableStateOf<RecentRecord?>(null) }
    val isExpense = uiState.dashTab == DashTab.EXPENSE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
    ) {
        // ===== 1. 顶部收支切换 Tab =====
        DashTabSelector(uiState.dashTab, onSwitchTab)

        // ===== 2/3/4. 内容区（可滚动） =====
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 总览卡片
            item { SummaryCard(uiState, isExpense) }

            // 分类占比
            item { CategoryStatsSection(uiState, isExpense) }

            // 最近记录标题
            item {
                Text(
                    text = "最近记录",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            // 最近记录列表（取前 10 条）
            val records = uiState.recentRecords.take(10)
            if (records.isEmpty()) {
                item { EmptyState() }
            } else {
                items(records, key = { "${it.type}-${it.id}" }) { record ->
                    RecentRecordItem(
                        record = record,
                        isExpense = isExpense,
                        onClick = { pendingDelete = record }
                    )
                }
            }
        }
    }

    // ===== 删除确认弹窗 =====
    pendingDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = {
                Text(
                    text = "删除记录",
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "确认删除该记录？",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteRecord(record.id, record.type)
                    pendingDelete = null
                }) {
                    Text("删除", color = TextDelete)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("取消", color = TextSecondary)
                }
            }
        )
    }
}

/**
 * 顶部收支切换 Tab。
 * 选中态：绿色背景 + 白色文字；未选中态：浅灰背景 + 深灰文字。
 */
@Composable
private fun DashTabSelector(
    currentTab: DashTab,
    onSwitchTab: (DashTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardWhite)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DashTabButton(
            text = "支出",
            isSelected = currentTab == DashTab.EXPENSE,
            onClick = { onSwitchTab(DashTab.EXPENSE) },
            modifier = Modifier.weight(1f)
        )
        DashTabButton(
            text = "收入",
            isSelected = currentTab == DashTab.INCOME,
            onClick = { onSwitchTab(DashTab.INCOME) },
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 单个 Tab 按钮，圆角样式。
 */
@Composable
private fun DashTabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) WeChatGreen else BackgroundGray)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            color = if (isSelected) CardWhite else Color(0xFF666666)
        )
    }
}

/**
 * 总览卡片：渐变背景 + 本月总额 + 今日/日均。
 * 支出用绿色系渐变，收入用蓝色系渐变。
 * 金额展示：支出 "¥25.50"，收入 "+¥25.50"。
 */
@Composable
private fun SummaryCard(uiState: UiState, isExpense: Boolean) {
    val monthTotal = if (isExpense) uiState.monthExpense else uiState.monthIncome
    val todayAmount = if (isExpense) uiState.todayExpense else uiState.todayIncome

    // 当月天数（用于计算日均）
    val daysInMonth = remember {
        Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    // 日均 = 本月总额 / 当月天数
    val dailyAvg = if (daysInMonth > 0) monthTotal / daysInMonth else 0L

    // 渐变：支出绿色系，收入蓝色系
    val gradient = if (isExpense) {
        Brush.linearGradient(listOf(WeChatGreenLight, Color(0xFF5BECA3), WeChatGreen))
    } else {
        Brush.linearGradient(listOf(Color(0xFFDBEAFE), Color(0xFF93C5FD), TextAmount))
    }

    // 金额格式：支出无符号，收入带 + 前缀
    val monthText = formatAmount(monthTotal, isExpense)
    val todayText = formatAmount(todayAmount, isExpense)
    val dailyText = formatAmount(dailyAvg, isExpense)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(gradient)
            .padding(24.dp)
    ) {
        Column {
            Text(
                text = if (isExpense) "本月支出" else "本月收入",
                fontSize = 14.sp,
                color = CardWhite.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = monthText,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = CardWhite
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummarySubItem(label = "今日", value = todayText)
                SummarySubItem(label = "日均", value = dailyText)
            }
        }
    }
}

/**
 * 总览卡片中的小字段（今日 / 日均）。
 */
@Composable
private fun SummarySubItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            color = CardWhite.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            color = CardWhite,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 分类占比区块：Top5 分类，按金额降序，每行含进度条 + 金额 + 百分比。
 * 进度条颜色：支出蓝色，收入绿色。
 * 百分比 = 该分类金额 / 本月总额 * 100。
 */
@Composable
private fun CategoryStatsSection(uiState: UiState, isExpense: Boolean) {
    val monthTotal = if (isExpense) uiState.monthExpense else uiState.monthIncome
    val progressColor = if (isExpense) TextAmount else TextIncome
    val amountColor = if (isExpense) TextAmount else TextIncome

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardWhite)
            .padding(16.dp)
    ) {
        Text(
            text = "分类占比",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Top5 分类，按金额降序
        val top5 = uiState.categoryStats.sortedByDescending { it.totalAmount }.take(5)
        if (top5.isEmpty() || monthTotal <= 0) {
            Text(
                text = "暂无分类数据",
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            top5.forEachIndexed { index, stat ->
                if (index > 0) Spacer(modifier = Modifier.height(12.dp))
                CategoryRow(
                    name = stat.category,
                    amount = stat.totalAmount,
                    total = monthTotal,
                    progressColor = progressColor,
                    amountColor = amountColor,
                    isExpense = isExpense
                )
            }
        }
    }
}

/**
 * 单个分类行：分类名 + 进度条 + 金额 + 百分比。
 */
@Composable
private fun CategoryRow(
    name: String,
    amount: Long,
    total: Long,
    progressColor: Color,
    amountColor: Color,
    isExpense: Boolean
) {
    val progress = if (total > 0) {
        (amount.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val percent = (progress * 100).toInt()
    val amountText = formatAmount(amount, isExpense)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                fontSize = 14.sp,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = amountText,
                fontSize = 13.sp,
                color = amountColor,
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
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = progressColor,
            trackColor = Color(0xFFE5E5E5)
        )
    }
}

/**
 * 最近记录单行：左侧分类名 + 中间商家/时间 + 右侧金额。
 * 支出金额蓝色无符号，收入金额绿色带 + 前缀。
 */
@Composable
private fun RecentRecordItem(
    record: RecentRecord,
    isExpense: Boolean,
    onClick: () -> Unit
) {
    val amountColor = if (isExpense) TextAmount else TextIncome
    val amountText = formatAmount(record.amount, isExpense)

    // 分类名：一级 + 二级
    val categoryText = buildString {
        append(record.category)
        record.subcategory?.takeIf { it.isNotBlank() }?.let {
            append(" · ").append(it)
        }
    }
    // 商家 + 时间
    val infoText = buildString {
        record.merchant?.takeIf { it.isNotBlank() }?.let {
            append(it).append(" · ")
        }
        append(TimeUtils.formatTimeRelative(record.time))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardWhite)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = categoryText,
                fontSize = 15.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = infoText,
                fontSize = 12.sp,
                color = TextSecondary
            )
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

/**
 * 空状态提示。
 */
@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "暂无记录",
            fontSize = 13.sp,
            color = TextSecondary
        )
    }
}

/**
 * 金额格式化：支出 "¥25.50"，收入 "+¥25.50"。
 */
private fun formatAmount(fen: Long, isExpense: Boolean): String {
    return if (isExpense) {
        AmountUtils.fenToYuanWithSymbol(fen)
    } else {
        "+${AmountUtils.fenToYuanWithSymbol(fen)}"
    }
}
