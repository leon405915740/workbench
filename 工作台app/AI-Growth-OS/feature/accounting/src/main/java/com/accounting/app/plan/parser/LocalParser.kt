package com.accounting.app.plan.parser

import com.accounting.app.log.AppLogger
import com.accounting.app.parser.time.TimeUtils
import com.accounting.app.ai.model.AiOutput
import com.accounting.app.ai.model.AiItem
import com.accounting.app.plan.model.NormalizedItem
import java.util.Date

object LocalParser {

    fun parse(aiOutput: AiOutput, requestId: String): List<NormalizedItem> {
        return aiOutput.items.mapIndexedNotNull { index, aiItem ->
            try {
                val billIndex = index + 1
                NormalizedItem(
                    description = aiItem.description ?: "未命名",
                    amount = parseAmount(aiItem.amount),
                    time = parseTime(aiItem.time_hint, requestId, billIndex),
                    categoryHint = aiItem.category_hint,
                    note = aiItem.note,
                    sourceRaw = aiItem.description ?: ""
                )
            } catch (e: Exception) {
                AppLogger.d(requestId, "标准化", "单条失败: ${aiItem.description}, error: ${e.message}")
                null
            }
        }
    }

    private fun parseAmount(amountStr: String?): Double {
        if (amountStr.isNullOrBlank()) return 0.0
        val clean = amountStr.replace("元", "").replace("块", "").replace("钱", "").trim()
        return try {
            clean.toDouble()
        } catch (e: Exception) {
            0.0
        }
    }

    private fun parseTime(timeHint: String?, requestId: String, billIndex: Int): Date {
        AppLogger.d(requestId, "时间解析", "time_hint=$timeHint", billIndex)
        return Date(TimeUtils.parseOrDefault(timeHint, requestId, billIndex))
    }
}