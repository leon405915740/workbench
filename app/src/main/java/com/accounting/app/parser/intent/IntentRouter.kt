package com.accounting.app.parser.intent

import com.accounting.app.data.model.BillExecutePlan
import com.accounting.app.data.model.BillPlanItem
import com.accounting.app.data.model.PlanAction
import com.accounting.app.domain.classification.CategoryService
import com.accounting.app.parser.category.ClassificationService
import com.accounting.app.parser.intent.MappingMatchResult
import com.accounting.app.parser.intent.MappingMatcher
import com.accounting.app.parser.model.MatchRequest
import com.accounting.app.parser.model.RoutingResult
import com.accounting.app.ai.service.AiPlanner
import com.accounting.app.plan.builder.PlanBuilder
import com.accounting.app.parser.amount.AmountUtils
import com.accounting.app.log.AppLogger
import com.accounting.app.parser.time.TimeUtils
import com.accounting.app.capture.model.PaymentInfo
import java.math.BigDecimal
import java.util.Date

class IntentRouter(
    private val planBuilder: PlanBuilder,
    private val aiPlanner: AiPlanner
) {

    suspend fun route(input: String, requestId: String): RoutingResult {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            return RoutingResult.Failure("输入为空")
        }

        val mappingResult = MappingMatcher.match("expense", trimmed, requestId)
        if (mappingResult != null && mappingResult.confidence > 0.9f) {
            val plan = buildPlanFromMapping(mappingResult, requestId, trimmed)
            val categoryName = CategoryService.getCategoryById(mappingResult.categoryId)?.name ?: "其他"
            AppLogger.d(requestId, "Layer1-映射匹配命中", "关键词: ${mappingResult.keyword}, 分类: $categoryName")
            AppLogger.decision(requestId, "意图路由", mappingResult.keyword, "映射匹配命中，路由到记账", mappingResult.confidence, "mapping")
            return RoutingResult.Success(plan, "mapping")
        }

        val segments = AmountUtils.extractAmounts(trimmed, requestId)
        if (segments.isNotEmpty()) {
            val plan = buildPlanFromRegex(segments, requestId, trimmed)
            if (plan != null) {
                AppLogger.d(requestId, "Layer1-正则命中", "共 ${plan.totalCount} 笔")
                AppLogger.decision(requestId, "意图路由", null, "正则提取到金额，路由到记账", 0.7f, "regex")
                return RoutingResult.Success(plan, "regex_fallback")
            }
        }

        AppLogger.d(requestId, "Layer3-AI分类推断", "映射匹配未命中，调用AI分类推断")
        return try {
            val plan = planBuilder.buildPlan(trimmed, requestId)
            if (plan != null) {
                AppLogger.d(requestId, "Layer2-AI解析", "共 ${plan.totalCount} 笔")
                AppLogger.decision(requestId, "意图路由", null, "AI解析成功，路由到记账", 0.6f, "ai")
                RoutingResult.AiSuccess(plan, "ai_parser")
            } else {
                val fallbackPlan = buildFallbackPlan(trimmed, requestId)
                if (fallbackPlan != null) {
                    AppLogger.decision(requestId, "意图路由", null, "AI解析失败，使用本地fallback", 0.3f, "fallback")
                    RoutingResult.Success(fallbackPlan, "local_fallback")
                } else {
                    RoutingResult.Failure("无法识别输入内容，请尝试更明确的描述")
                }
            }
        } catch (e: Exception) {
            AppLogger.e(requestId, "Layer2-AI解析", "AI解析失败，降级本地兜底: ${e.message}", e)
            val fallbackPlan = buildFallbackPlan(trimmed, requestId)
            if (fallbackPlan != null) {
                RoutingResult.Success(fallbackPlan, "ai_fallback")
            } else {
                RoutingResult.Failure("AI解析失败，请检查网络或API Key配置")
            }
        }
    }

    private suspend fun buildPlanFromMapping(
        mapping: MappingMatchResult,
        requestId: String,
        rawInput: String
    ): BillExecutePlan {
        val category = CategoryService.getCategoryById(mapping.categoryId)
        val subcategory = mapping.subcategoryId?.let { CategoryService.getCategoryById(it) }
        val amountFen = extractAmountFromInput(rawInput)
        val time = TimeUtils.now()

        val items = listOf(BillPlanItem(
            action = PlanAction.ADD,
            type = "expense",
            amount = amountFen,
            category = category?.name ?: "其他",
            subCategory = subcategory?.name,
            merchant = mapping.keyword,
            billTime = time,
            remark = null,
            confidence = mapping.confidence,
            source = "mapping",
            matchedMemory = true,
            memoryId = null
        ))

        return BillExecutePlan(
            requestId = requestId,
            totalCount = 1,
            totalAmount = amountFen,
            items = items,
            rawInput = rawInput
        )
    }

    private fun extractAmountFromInput(input: String): Long {
        val amountMatch = Regex("""\d+(\.\d+)?""").find(input)
        val amount = amountMatch?.value?.toDoubleOrNull() ?: 0.0
        return BigDecimal(amount.toString()).movePointRight(2).toLong()
    }

    private suspend fun buildPlanFromRegex(
        segments: List<AmountUtils.AmountSegment>,
        requestId: String,
        rawInput: String
    ): BillExecutePlan? {
        if (segments.isEmpty()) return null

        val items = segments.mapIndexedNotNull { index, segment ->
            val billIndex = index + 1
            val description = AmountUtils.cleanSegment(segment.textBefore)
            if (description.isBlank()) return@mapIndexedNotNull null

            val matchRequest = MatchRequest(
                hint = null,
                description = description,
                time = Date(TimeUtils.now())
            )
            val matchResult = ClassificationService.match(matchRequest, requestId, billIndex)

            BillPlanItem(
                action = PlanAction.ADD,
                type = matchResult.type,
                amount = segment.amountFen,
                category = matchResult.category,
                subCategory = matchResult.subCategory,
                merchant = description,
                billTime = TimeUtils.now(),
                remark = null,
                confidence = matchResult.confidence,
                source = "regex",
                matchedMemory = false
            )
        }

        if (items.isEmpty()) return null

        val totalAmount = items.sumOf { it.amount }
        return BillExecutePlan(
            requestId = requestId,
            totalCount = items.size,
            totalAmount = totalAmount,
            items = items,
            rawInput = rawInput
        )
    }

    private suspend fun buildFallbackPlan(rawInput: String, requestId: String): BillExecutePlan? {
        val amountMatch = Regex("""\d+(\.\d+)?""").find(rawInput)
        val amount = amountMatch?.value?.toDoubleOrNull() ?: return null
        val amountFen = BigDecimal(amount.toString()).movePointRight(2).toLong()

        val description = AmountUtils.cleanSegment(rawInput)
        val matchRequest = MatchRequest(
            hint = null,
            description = description,
            time = Date(TimeUtils.now())
        )
        val matchResult = ClassificationService.match(matchRequest, requestId)

        val items = listOf(BillPlanItem(
            action = PlanAction.ADD,
            type = matchResult.type,
            amount = amountFen,
            category = matchResult.category,
            subCategory = matchResult.subCategory,
            merchant = description,
            billTime = TimeUtils.now(),
            remark = null,
            confidence = 0.5f,
            source = "fallback",
            matchedMemory = false
        ))

        return BillExecutePlan(
            requestId = requestId,
            totalCount = 1,
            totalAmount = amountFen,
            items = items,
            rawInput = rawInput
        )
    }

    suspend fun autoCapture(info: PaymentInfo): RoutingResult {
        val requestId = info.captureId
        val merchant = info.merchant ?: ""
        val amount = info.amount ?: run {
            AppLogger.e(requestId, "AutoCapture_Plan", "金额为空，无法生成计划", null)
            return RoutingResult.Failure("金额为空")
        }
        val payTime = info.payTime ?: TimeUtils.now()

        val description = "$merchant ${amount}分"
        val matchRequest = MatchRequest(
            hint = null,
            description = merchant,
            time = Date(payTime)
        )
        val matchResult = ClassificationService.match(matchRequest, requestId)
        AppLogger.d(requestId, "AutoCapture_Plan", "分类匹配结果：type=${matchResult.type}, category=${matchResult.category}, subCategory=${matchResult.subCategory}")

        val items = listOf(BillPlanItem(
            action = PlanAction.ADD,
            type = matchResult.type,
            amount = amount,
            category = matchResult.category,
            subCategory = matchResult.subCategory,
            merchant = merchant,
            billTime = payTime,
            remark = "自动采集",
            confidence = info.confidence,
            source = "auto_capture",
            matchedMemory = false
        ))

        val plan = BillExecutePlan(
            requestId = requestId,
            totalCount = 1,
            totalAmount = amount,
            items = items,
            rawInput = description
        )

        AppLogger.i(requestId, "AutoCapture_Plan", "自动采集生成计划，merchant=$merchant, amount=$amount, category=${matchResult.category}")
        return RoutingResult.Success(plan, "auto_capture")
    }
}