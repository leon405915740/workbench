package com.accounting.app.plan.parser

import com.accounting.app.log.AppLogger
import com.accounting.app.parser.time.TimeUtils
import com.accounting.app.ai.model.AiOutput
import com.accounting.app.ai.model.AiItem
import com.accounting.app.plan.model.NormalizedItem
import com.accounting.app.util.AmountUtils
import java.util.Date

object LocalParser {

    fun parse(aiOutput: AiOutput, requestId: String): List<NormalizedItem> {
        val parsed = aiOutput.items.mapIndexedNotNull { index, aiItem ->
            val description = aiItem.description?.trim()
            if (description.isNullOrEmpty()) return@mapIndexedNotNull null
            try {
                val billIndex = index + 1
                val amount = parseAmount(aiItem.amount) ?: return@mapIndexedNotNull null
                NormalizedItem(
                    description = description,
                    amountFen = amount,
                    time = parseTime(aiItem.time_hint, requestId, billIndex),
                    categoryHint = aiItem.category_hint,
                    note = aiItem.note,
                    sourceRaw = description
                )
            } catch (e: Exception) {
                AppLogger.d(requestId, "标准化", "单条失败: ${aiItem.description}, error: ${e.message}")
                null
            }
        }
        return if (parsed.size == aiOutput.items.size) parsed else emptyList()
    }

    private fun parseAmount(amountStr: String?): Long? {
        if (amountStr.isNullOrBlank()) return null
        val clean = amountStr.trim()
            .removeSuffix("块钱")
            .removeSuffix("元")
            .removeSuffix("块")
            .trim()
        if (clean.any { it in "元块钱角毛分点" }) return null
        val yuan = if (clean.toBigDecimalOrNull() != null) clean
        else AmountUtils.chineseIntegerToLong(clean)?.toString()
        return yuan?.let { runCatching { AmountUtils.yuanToFen(it) }.getOrNull() }
            ?.takeIf { it > 0L }
    }

    private fun parseTime(timeHint: String?, requestId: String, billIndex: Int): Date {
        AppLogger.d(requestId, "时间解析", "time_hint=$timeHint", billIndex)
        return Date(TimeUtils.parseOrDefault(timeHint, requestId, billIndex))
    }
}
