package com.accounting.app.parser.category

import com.accounting.app.data.repository.AppRepository
import com.accounting.app.log.AppLogger
import com.accounting.app.domain.classification.CategoryService
import com.accounting.app.parser.intent.MappingMatcher
import com.accounting.app.parser.intent.RuleMatcher
import com.accounting.app.parser.time.TimeRuleMatcher
import com.accounting.app.parser.model.MatchRequest
import com.accounting.app.parser.model.MatchResult
import com.accounting.app.parser.model.MatchSource

object ClassificationService {

    var aiClassifier: AiClassifier? = null
    private var repository: AppRepository? = null

    fun init(repo: AppRepository) {
        repository = repo
    }

    suspend fun match(request: MatchRequest, requestId: String, billIndex: Int? = null): MatchResult {
        // 先对完整描述做收支方向判定，mapping/memory/ai_hint/ai_correction/fallback
        // 各分支统一沿用该判定结果，修复此前各分支硬编码 type="expense" 导致
        // 「意外收入400」等收入输入被记成支出的问题。
        // 对完整描述做收支方向判定（log=false：完整「意图分流」全文解析已由 IntentRouter 唯一输出，此处仅复算类型不打全文）
        val type = RuleMatcher.preJudgeType(request.description, requestId, log = false)
        val defaultCategory = getDefaultCategory(type)

        val mappingResult = MappingMatcher.match(type, request.description, requestId, billIndex ?: 1)
        if (mappingResult != null) {
            val category = CategoryService.getCategoryById(mappingResult.categoryId)
            val categoryName = category?.name ?: defaultCategory
            val message = "待匹配：${request.description}，来源：mapping，分类：$categoryName"
            if (billIndex != null) AppLogger.d(requestId, "分类匹配", message, billIndex)
            else AppLogger.d(requestId, "分类匹配", message)
            return MatchResult(
                type = type,
                category = categoryName,
                subCategory = null,
                source = MatchSource.MAPPING,
                confidence = mappingResult.confidence
            )
        }

        // CategoryMemory 查询
        val repo = repository
        if (repo != null) {
            val memoryResult = repo.matchMemory(type, request.description)
            if (memoryResult != null) {
                val message = "待匹配：${request.description}，来源：memory，分类：${memoryResult.category}"
                if (billIndex != null) AppLogger.d(requestId, "分类匹配", message, billIndex)
                else AppLogger.d(requestId, "分类匹配", message)
                return MatchResult(
                    type = type,
                    category = memoryResult.category,
                    subCategory = memoryResult.subcategory,
                    source = MatchSource.MEMORY,
                    confidence = if (memoryResult.source == "user") 1.0f else 0.95f,
                    matchedMemory = true,
                    memoryId = memoryResult.id
                )
            }
        }

        val ruleResult = RuleMatcher.match(request, requestId, billIndex, preJudgedType = type)
        if (ruleResult != null && ruleResult.confidence >= 0.9f) {
            if (ruleResult.category != defaultCategory) {
                return ruleResult
            }
        }

        val timeResult = TimeRuleMatcher.match(request, requestId, billIndex)
        if (timeResult != null) {
            return timeResult
        }

        val aiHint = request.categoryHint
        if (!aiHint.isNullOrBlank()) {
            val aiCategory = CategoryService.getCategoryByName(type, aiHint)
            if (aiCategory != null) {
                val matchType = if (aiCategory.name == aiHint.trim()) "EXACT" else "CONTAINS"
                val message = "AI推断命中: hint=$aiHint → category=${aiCategory.name}, matchType=$matchType, confidence=0.6, source=AI_HINT"
                if (billIndex != null) AppLogger.d(requestId, "分类匹配", message, billIndex)
                else AppLogger.d(requestId, "分类匹配", message)
                if (billIndex != null) AppLogger.decision(requestId, "分类匹配", aiHint, "AI推断命中，分类:${aiCategory.name}", 0.6f, "ai_hint", billIndex)
                else AppLogger.decision(requestId, "分类匹配", aiHint, "AI推断命中，分类:${aiCategory.name}", 0.6f, "ai_hint")
                return MatchResult(
                    type = type,
                    category = aiCategory.name,
                    source = MatchSource.AI_HINT,
                    confidence = 0.6f
                )
            }
        }

        // AI 兜底分类纠正
        val aiClassifier = this.aiClassifier
        if (aiClassifier != null) {
            val aiResult = aiClassifier.correct(request.description, type, requestId, billIndex)
            if (aiResult != null) {
                val aiMessage = "待匹配：${request.description}，来源：ai_correction，分类：${aiResult.category}，最终分类：${aiResult.category}"
                if (billIndex != null) AppLogger.d(requestId, "分类匹配", aiMessage, billIndex)
                else AppLogger.d(requestId, "分类匹配", aiMessage)
                if (billIndex != null) AppLogger.decision(requestId, "分类匹配", request.description, "AI兜底纠正，分类:${aiResult.category}", 0.6f, "ai_correction", billIndex)
                else AppLogger.decision(requestId, "分类匹配", request.description, "AI兜底纠正，分类:${aiResult.category}", 0.6f, "ai_correction")
                return aiResult
            }
        }

        val fallbackCategory = defaultCategory
        val message = "待匹配：${request.description}，来源：fallback，分类：$fallbackCategory，最终分类：$fallbackCategory"
        if (billIndex != null) AppLogger.d(requestId, "分类匹配", message, billIndex)
        else AppLogger.d(requestId, "分类匹配", message)
        if (billIndex != null) AppLogger.decision(requestId, "分类匹配", request.description, "无匹配规则，使用默认分类:$fallbackCategory", 0.3f, "fallback", billIndex)
        else AppLogger.decision(requestId, "分类匹配", request.description, "无匹配规则，使用默认分类:$fallbackCategory", 0.3f, "fallback")
        return MatchResult(
            type = type,
            category = fallbackCategory,
            source = MatchSource.FALLBACK,
            confidence = 0.3f
        )
    }

    private fun getDefaultCategory(type: String): String {
        return if (type == "income") "其他收入" else "其他支出"
    }
}