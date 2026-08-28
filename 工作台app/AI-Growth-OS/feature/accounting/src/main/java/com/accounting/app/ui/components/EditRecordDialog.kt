package com.accounting.app.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.accounting.app.ui.model.EditDialogData
import com.accounting.app.ui.theme.BackgroundGray
import com.accounting.app.ui.theme.BorderDefault
import com.accounting.app.ui.theme.CardWhite
import com.accounting.app.ui.theme.TextPrimary
import com.accounting.app.ui.theme.TextSecondary
import com.accounting.app.ui.theme.WeChatGreen
import com.accounting.app.ui.components.getCategoryEmoji
import com.accounting.app.util.AmountUtils
import com.accounting.app.util.TimeUtils
import java.util.Calendar

/**
 * 编辑账单弹窗（统一新建 + 编辑双模式）。
 *
 * - 通过 [data].[recordId][EditDialogData.recordId] 是否为 null 判断模式（null=新建，非空=编辑）
 * - 新建模式：标题「手动记账」，type（支出/收入）Tab 可切换
 * - 编辑模式：标题「编辑账单」，type 只读标签不可切换，底部显示「删除记录」按钮
 * - 提交时构造完整的 [EditDialogData] 回传，ViewModel 不读取 UI State
 *
 * @param data            弹窗初始数据（含模式判断、字段预填、原上下文）
 * @param onSubmit        新建模式提交回调
 * @param onEditConfirm   编辑模式提交回调
 * @param onDismiss       关闭弹窗
 * @param onDeleteRequest 编辑模式点击删除回调（二次确认由外层 MainActivity 控制）
 */
@Composable
fun EditRecordDialog(
    data: EditDialogData,
    onSubmit: (EditDialogData) -> Unit,
    onEditConfirm: (EditDialogData) -> Unit,
    onDismiss: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    val context = LocalContext.current
    val isEditMode = data.recordId != null

    // 收支类型：编辑模式固定不可切换，新建模式可切换
    var type by rememberSaveable { mutableStateOf(data.type) }
    // 金额输入（元字符串），预填时从 data.amount（分）换算
    var amountText by rememberSaveable {
        mutableStateOf(if (data.amount > 0) AmountUtils.fenToYuan(data.amount) else "")
    }
    // 选中的分类：新建模式默认 null（用户选择），编辑模式预填 data.category
    var category by rememberSaveable { mutableStateOf(data.category.takeIf { isEditMode }) }
    // 商家名称
    var merchant by rememberSaveable { mutableStateOf(data.merchant ?: "") }
    // 备注
    var note by rememberSaveable { mutableStateOf(data.note ?: "") }
    // 时间戳，预填 data.time，无则当前时间
    var timeMillis by rememberSaveable { mutableStateOf(if (data.time > 0) data.time else TimeUtils.now()) }
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
            shape = RoundedCornerShape(20.dp),
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
                        text = if (isEditMode) "编辑账单" else "手动记账",
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

                // 收支类型：编辑模式只读标签，新建模式可切换 Tab
                if (isEditMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BackgroundGray)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = if (type == "expense") "支出" else "收入",
                            fontSize = 15.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
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
                                category = null
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
                            }
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
                            focusedContainerColor = CardWhite,
                            unfocusedContainerColor = CardWhite,
                            focusedIndicatorColor = WeChatGreen,
                            unfocusedIndicatorColor = BorderDefault
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
                        Text(
                            text = category?.let { "${getCategoryEmoji(it, type)} $it" } ?: "请选择分类",
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
                            focusedContainerColor = CardWhite,
                            unfocusedContainerColor = CardWhite,
                            focusedIndicatorColor = WeChatGreen,
                            unfocusedIndicatorColor = BorderDefault
                        )
                    )

                    // 备注输入（选填）
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

                // 编辑模式：删除记录按钮
                if (isEditMode) {
                    Button(
                        onClick = onDeleteRequest,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color(0xFFE53935)
                        ),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text("删除记录", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

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
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Text("取消", fontSize = 16.sp)
                    }
                    // 确认按钮：金额 > 0 且分类已选才可点击
                    val amountFen = runCatching { AmountUtils.yuanToFen(amountText) }.getOrDefault(0L)
                    val canSubmit = amountFen > 0 && category != null
                    Button(
                        onClick = {
                            val updatedData = data.copy(
                                type = type,
                                amount = amountFen,
                                category = category!!,
                                merchant = merchant.takeIf { it.isNotBlank() },
                                time = timeMillis,
                                note = note.takeIf { it.isNotBlank() }
                            )
                            if (isEditMode) {
                                onEditConfirm(updatedData)
                            } else {
                                onSubmit(updatedData)
                            }
                        },
                        enabled = canSubmit,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WeChatGreen,
                            disabledContainerColor = WeChatGreen.copy(alpha = 0.4f)
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp)
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
            onConfirm = { c ->
                category = c
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
            .then(
                if (selected) Modifier.shadow(elevation = 2.dp, shape = RoundedCornerShape(20.dp), clip = false)
                else Modifier
            )
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) CardWhite else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            color = if (selected) WeChatGreen else TextPrimary,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
    }
}
