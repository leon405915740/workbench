package com.accounting.app.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.accounting.app.ui.theme.BackgroundGray
import com.accounting.app.ui.theme.CardWhite
import com.accounting.app.ui.theme.TextPrimary
import com.accounting.app.ui.theme.TextSecondary
import com.accounting.app.ui.theme.WeChatGreen
import com.accounting.app.util.AmountUtils
import com.accounting.app.util.TimeUtils
import java.util.Calendar

/**
 * 手动记账弹窗。
 *
 * - 顶部「支出」「收入」切换 Tab（选中态绿色背景白字）
 * - 表单：金额（数字输入）、分类（点击打开 CategoryPicker）、
 *   时间（点击弹出 DatePickerDialog + TimePickerDialog）、商家、备注
 * - 底部确认（绿色）+ 取消按钮
 * - 校验：金额 > 0 且分类已选才能提交
 */
@Composable
fun ManualEntryDialog(
    prefillNote: String,
    onConfirm: (
        type: String,
        amount: Long,
        category: String,
        subcategory: String?,
        merchant: String?,
        time: Long,
        note: String?
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // 收支类型：expense / income
    var type by rememberSaveable { mutableStateOf("expense") }
    // 金额输入（元字符串）
    var amountText by rememberSaveable { mutableStateOf("") }
    // 选中的一级/二级分类
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var subcategory by rememberSaveable { mutableStateOf<String?>(null) }
    // 商家名称
    var merchant by rememberSaveable { mutableStateOf("") }
    // 备注，预填 prefillNote
    var note by rememberSaveable { mutableStateOf(prefillNote) }
    // 时间戳，默认当前时间
    var timeMillis by rememberSaveable { mutableStateOf(TimeUtils.now()) }
    // 是否展示分类选择器
    var showCategoryPicker by remember { mutableStateOf(false) }
    // 是否展示日期/时间选择器
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(16.dp),
            color = CardWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // 标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "手动记账",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "关闭",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 支出/收入切换 Tab
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(BackgroundGray)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TypeTabButton(
                        text = "支出",
                        selected = type == "expense",
                        modifier = Modifier.weight(1f)
                    ) {
                        if (type != "expense") {
                            type = "expense"
                            // 切换类型时重置分类（不同类型分类列表不同）
                            category = null
                            subcategory = null
                        }
                    }
                    TypeTabButton(
                        text = "收入",
                        selected = type == "income",
                        modifier = Modifier.weight(1f)
                    ) {
                        if (type != "income") {
                            type = "income"
                            category = null
                            subcategory = null
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 表单区（可滚动）
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 金额输入
                    FieldLabel("金额")
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("请输入金额", color = TextSecondary, fontSize = 14.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = BackgroundGray,
                            unfocusedContainerColor = BackgroundGray,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    // 分类选择（点击打开 CategoryPicker）
                    FieldLabel("分类")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BackgroundGray)
                            .clickable { showCategoryPicker = true }
                            .padding(horizontal = 12.dp, vertical = 14.dp)
                    ) {
                        val categoryLabel = if (category != null) {
                            listOfNotNull(category, subcategory).joinToString("-")
                        } else {
                            "请选择分类"
                        }
                        Text(
                            text = categoryLabel,
                            fontSize = 14.sp,
                            color = if (category != null) TextPrimary else TextSecondary
                        )
                    }

                    // 时间选择（点击弹出 DatePicker + TimePicker）
                    FieldLabel("时间")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BackgroundGray)
                            .clickable { showDatePicker = true }
                            .padding(horizontal = 12.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = TimeUtils.formatTime(timeMillis),
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                    }

                    // 商家输入（选填）
                    FieldLabel("商家（选填）")
                    OutlinedTextField(
                        value = merchant,
                        onValueChange = { merchant = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("商家名称（选填）", color = TextSecondary, fontSize = 14.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = BackgroundGray,
                            unfocusedContainerColor = BackgroundGray,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    // 备注输入（选填，预填 prefillNote）
                    FieldLabel("备注（选填）")
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("备注（选填）", color = TextSecondary, fontSize = 14.sp) },
                        shape = RoundedCornerShape(8.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = BackgroundGray,
                            unfocusedContainerColor = BackgroundGray,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 底部按钮：取消 + 确认
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 取消按钮
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BackgroundGray,
                            contentColor = TextPrimary
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp)
                    ) {
                        Text("取消", fontSize = 16.sp)
                    }
                    // 确认按钮：金额 > 0 且分类已选才可点击
                    val amountFen = runCatching { AmountUtils.yuanToFen(amountText) }.getOrDefault(0L)
                    val canSubmit = amountFen > 0 && category != null
                    Button(
                        onClick = {
                            onConfirm(
                                type,
                                amountFen,
                                category!!,
                                subcategory,
                                merchant.takeIf { it.isNotBlank() },
                                timeMillis,
                                note.takeIf { it.isNotBlank() }
                            )
                        },
                        enabled = canSubmit,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WeChatGreen,
                            disabledContainerColor = WeChatGreen.copy(alpha = 0.4f)
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp)
                    ) {
                        Text(
                            text = "确认",
                            color = CardWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    // 分类选择器
    if (showCategoryPicker) {
        CategoryPicker(
            type = type,
            initialCategory = category,
            initialSubcategory = subcategory,
            onConfirm = { c, s ->
                category = c
                subcategory = s
                showCategoryPicker = false
            },
            onDismiss = { showCategoryPicker = false }
        )
    }

    // 日期选择器（选完日期后联动打开时间选择器）
    if (showDatePicker) {
        val cal = remember { Calendar.getInstance().apply { timeInMillis = timeMillis } }
        LaunchedEffect(Unit) {
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    cal.set(Calendar.YEAR, year)
                    cal.set(Calendar.MONTH, month)
                    cal.set(Calendar.DAY_OF_MONTH, day)
                    timeMillis = cal.timeInMillis
                    showDatePicker = false
                    showTimePicker = true
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).apply {
                setOnCancelListener { showDatePicker = false }
            }.show()
        }
    }

    // 时间选择器
    if (showTimePicker) {
        val cal = remember { Calendar.getInstance().apply { timeInMillis = timeMillis } }
        LaunchedEffect(Unit) {
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    cal.set(Calendar.HOUR_OF_DAY, hour)
                    cal.set(Calendar.MINUTE, minute)
                    timeMillis = cal.timeInMillis
                    showTimePicker = false
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).apply {
                setOnCancelListener { showTimePicker = false }
            }.show()
        }
    }
}

/**
 * 表单字段标签
 */
@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = TextSecondary
    )
}

/**
 * 收支类型切换按钮
 */
@Composable
private fun TypeTabButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) WeChatGreen else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            color = if (selected) CardWhite else TextPrimary,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
    }
}
