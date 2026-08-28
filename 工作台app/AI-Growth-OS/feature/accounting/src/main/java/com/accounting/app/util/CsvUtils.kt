package com.accounting.app.util

import com.accounting.app.data.local.entity.ExpenseEntity
import com.accounting.app.data.local.entity.IncomeEntity

/**
 * CSV 生成工具。
 *
 * 将支出与收入记录统一导出为 CSV 字符串：
 * - 表头：类型,金额(元),一级分类,二级分类,商家,时间,备注,置信度,原始输入,创建时间
 * - 支出在前，收入在后
 * - 字段含逗号时用双引号包裹，符合 RFC 4180
 * - 字符串开头添加 UTF-8 BOM，避免 Excel 打开中文乱码
 */
object CsvUtils {

    /** CSV 表头 */
    private const val CSV_HEADER =
        "类型,金额(元),一级分类,二级分类,商家,时间,备注,置信度,原始输入,创建时间"

    /**
     * 生成 CSV 字符串。
     *
     * @param expenses 支出记录列表
     * @param incomes  收入记录列表
     * @return 带 UTF-8 BOM 的 CSV 字符串
     */
    fun generateCsv(
        expenses: List<ExpenseEntity>,
        incomes: List<IncomeEntity>
    ): String {
        val sb = StringBuilder()
        // UTF-8 BOM 前缀，避免 Excel 打开中文乱码
        sb.append("\uFEFF")
        // 表头
        sb.append(CSV_HEADER).append("\n")

        // 支出记录
        expenses.forEach { entity ->
            sb.appendRow(
                type = "支出",
                amount = entity.amount,
                category = entity.category,
                subcategory = entity.subcategory,
                merchant = entity.merchant,
                time = entity.time,
                note = entity.note,
                confidence = entity.confidence,
                rawInput = entity.rawInput,
                createdAt = entity.createdAt
            )
        }

        // 收入记录
        incomes.forEach { entity ->
            sb.appendRow(
                type = "收入",
                amount = entity.amount,
                category = entity.category,
                subcategory = entity.subcategory,
                merchant = entity.merchant,
                time = entity.time,
                note = entity.note,
                confidence = entity.confidence,
                rawInput = entity.rawInput,
                createdAt = entity.createdAt
            )
        }

        return sb.toString()
    }

    /**
     * 追加一行 CSV 数据。
     *
     * 字段顺序与 [CSV_HEADER] 保持一致。
     * 字段含逗号、换行或双引号时，用双引号包裹并把内部双引号转义为两个双引号。
     */
    private fun StringBuilder.appendRow(
        type: String,
        amount: Long,
        category: String,
        subcategory: String?,
        merchant: String?,
        time: Long,
        note: String?,
        confidence: Float,
        rawInput: String,
        createdAt: Long
    ) {
        append(escapeField(type)).append(',')
        append(escapeField(AmountUtils.fenToYuan(amount))).append(',')
        append(escapeField(category)).append(',')
        append(escapeField(subcategory)).append(',')
        append(escapeField(merchant)).append(',')
        append(escapeField(TimeUtils.formatTime(time))).append(',')
        append(escapeField(note)).append(',')
        append(escapeField(formatConfidence(confidence))).append(',')
        append(escapeField(rawInput)).append(',')
        append(escapeField(TimeUtils.formatTime(createdAt))).append('\n')
    }

    /** 置信度格式化为两位小数字符串 */
    private fun formatConfidence(confidence: Float): String {
        return String.format("%.2f", confidence)
    }

    /**
     * CSV 字段转义。
     * - 字段为 null 返回空串
     * - 字段含逗号、双引号、换行时，用双引号包裹并把内部双引号替换为两个双引号
     */
    private fun escapeField(value: String?): String {
        if (value == null) return ""
        val needQuote = value.contains(',') ||
                value.contains('"') ||
                value.contains('\n') ||
                value.contains('\r')
        if (!needQuote) return value
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}
