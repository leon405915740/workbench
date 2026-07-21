package com.accounting.app.plan.builder

import com.accounting.app.data.model.BillExecutePlan
import com.accounting.app.data.model.BillPlanItem
import com.accounting.app.data.model.PlanAction
import com.accounting.app.data.model.PlanSummary
import com.accounting.app.parser.category.ClassificationService
import com.accounting.app.parser.model.MatchRequest
import com.accounting.app.log.AppLogger
import com.accounting.app.plan.model.NormalizedItem
import com.accounting.app.plan.model.PlanType
import com.accounting.app.plan.validator.PlanValidator
import com.accounting.app.plan.validator.ValidationResult

object PlanMerger {

    suspend fun merge(
        normalizedItems: List<NormalizedItem>,
        requestId: String,
        rawInput: String
    ): BillExecutePlan {
        val finalItems = normalizedItems.mapIndexed { index, normalized ->
            val matchRequest = MatchRequest(
                hint = normalized.categoryHint,
                description = normalized.description,
                time = normalized.time,
                categoryHint = normalized.categoryHint
            )
            val billIndex = index + 1
            val matchResult = ClassificationService.match(matchRequest, requestId, billIndex)

            BillPlanItem(
                action = PlanAction.ADD,
                type = matchResult.type,
                amount = (normalized.amount * 100).toLong(),
                category = matchResult.category,
                subCategory = matchResult.subCategory,
                merchant = normalized.description,
                billTime = normalized.time.time,
                remark = normalized.note,
                confidence = matchResult.confidence,
                source = matchResult.source.name.lowercase(),
                matchedMemory = matchResult.source == com.accounting.app.parser.model.MatchSource.MEMORY
            )
        }

        val totalAmount = finalItems.sumOf { it.amount }
        val planType = detectPlanType(finalItems)

        val summary = PlanSummary(
            totalAmount = finalItems.sumOf { it.amount.toDouble() / 100 },
            count = finalItems.size,
            firstCategoryHint = finalItems.firstOrNull()?.category
        )

        val plan = BillExecutePlan(
            requestId = requestId,
            totalCount = finalItems.size,
            totalAmount = totalAmount,
            items = finalItems,
            rawInput = rawInput,
            type = planType,
            summary = summary
        )

        val validationResult = PlanValidator.validate(plan, requestId)
        if (validationResult is ValidationResult.Failure) {
            AppLogger.d(requestId, "计划合并", "Plan合并警告: ${validationResult.reason}")
        }

        return plan
    }

    private fun detectPlanType(items: List<BillPlanItem>): PlanType {
        return when {
            items.any { it.action == PlanAction.UPDATE } -> PlanType.BULK_UPDATE
            items.any { it.action == PlanAction.DELETE } -> PlanType.BULK_DELETE
            items.size > 1 -> PlanType.MULTI_ADD
            else -> PlanType.SINGLE
        }
    }
}