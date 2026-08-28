package com.accounting.app.plan.builder

import com.accounting.app.data.model.BillExecutePlan
import com.accounting.app.log.AppLogger
import com.accounting.app.ai.service.AiPlanner
import com.accounting.app.ai.model.AiOutput
import com.accounting.app.plan.parser.LocalParser

class PlanBuilder(
    private val aiPlanner: AiPlanner
) {

    suspend fun buildPlan(rawInput: String, requestId: String): BillExecutePlan? {
        return try {
            val aiOutput = aiPlanner.parse(rawInput, requestId)
            
            val normalizedItems = LocalParser.parse(aiOutput, requestId)
            
            if (normalizedItems.isEmpty()) {
                AppLogger.d(requestId, "计划生成", "标准化后无有效条目")
                return null
            }
            
            PlanMerger.merge(normalizedItems, requestId, rawInput)
        } catch (e: Exception) {
            AppLogger.e(requestId, "计划生成", "生成计划异常：${e.message}", e)
            null
        }
    }
}