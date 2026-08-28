package com.accounting.app.parser.intent

import com.accounting.app.data.model.BillExecutePlan
import com.accounting.app.data.model.BillPlanItem
import com.accounting.app.data.model.PlanAction
import com.accounting.app.data.repository.AppRepository
import com.accounting.app.domain.classification.CategoryService
import com.accounting.app.parser.category.ClassificationService
import com.accounting.app.parser.intent.MappingMatchResult
import com.accounting.app.parser.intent.MappingMatcher
import com.accounting.app.parser.model.MatchRequest
import com.accounting.app.parser.model.RoutingResult
import com.accounting.app.ai.service.AiPlanner
import com.accounting.app.plan.builder.PlanBuilder
import com.accounting.app.util.AmountUtils
import com.accounting.app.log.AppLogger
import com.accounting.app.parser.time.TimeUtils
import java.math.BigDecimal
import java.util.Date

class IntentRouter(
    private val planBuilder: PlanBuilder,
    private val aiPlanner: AiPlanner,
    private val repository: AppRepository
) {

    suspend fun route(input: String, requestId: String): RoutingResult {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            return RoutingResult.Failure("输入为空")
        }

        // 先判定收支方向，映射匹配按对应类型查询（收入输入应匹配收入映射，而非硬编码 expense）
        val routeType = RuleMatcher.preJudgeType(trimmed, requestId)
        val mappingResult = MappingMatcher.match(routeType, trimmed, requestId)
        if (mappingResult != null && mappingResult.mappingId != null) {
            repository.incrementMappingHitCount(mappingResult.mappingId, requestId)
        }
        if (mappingResult != null && mappingResult.confidence > 0.9f) {
            val plan = buildPlanFromMapping(mappingResult, requestId, trimmed, routeType)
            val defaultCategory = if (routeType == "income") "其他收入" else "其他支出"
            val categoryName = CategoryService.getCategoryById(mappingResult.categoryId)?.name ?: defaultCategory
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
        rawInput: String,
        type: String
    ): BillExecutePlan {
        val category = CategoryService.getCategoryById(mapping.categoryId)
        val subcategory = mapping.subcategoryId?.let { CategoryService.getCategoryById(it) }
        val defaultCategory = if (type == "income") "其他收入" else "其他支出"
        val amountFen = extractAmountFromInput(rawInput)
        val time = TimeUtils.now()

        val items = listOf(BillPlanItem(
            action = PlanAction.ADD,
            type = type,
            amount = amountFen,
            category = category?.name ?: defaultCategory,
            subCategory = subcategory?.name,
            merchant = mapping.keyword,
            billTime = time,
            remark = null,
            confidence = mapping.confidence,
            source = "mapping",
            matchedMemory = false,
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
                matchedMemory = matchResult.matchedMemory,
                memoryId = matchResult.memoryId
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
            matchedMemory = matchResult.matchedMemory,
            memoryId = matchResult.memoryId
        ))

        return BillExecutePlan(
            requestId = requestId,
            totalCount = 1,
            totalAmount = amountFen,
            items = items,
            rawInput = rawInput
        )
    }

}