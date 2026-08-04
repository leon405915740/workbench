package com.accounting.app.parser.category

import com.accounting.app.log.AppLogger
import com.accounting.app.domain.classification.CategoryService
import com.accounting.app.parser.intent.MappingMatcher
import com.accounting.app.parser.intent.RuleMatcher
import com.accounting.app.parser.time.TimeRuleMatcher
import com.accounting.app.parser.model.MatchRequest
import com.accounting.app.parser.model.MatchResult
import com.accounting.app.parser.model.MatchSource
import java.util.Date

object ClassificationService {

    var aiClassifier: AiClassifier? = null

    suspend fun match(request: MatchRequest, requestId: String, billIndex: Int? = null): MatchResult {
        val mappingResult = MappingMatcher.match("expense", request.description, requestId, billIndex ?: 1)
        if (mappingResult != null) {
            val category = CategoryService.getCategoryById(mappingResult.categoryId)
            val categoryName = category?.name ?: "其他支出"
            val message = "待匹配：${request.description}，来源：mapping，分类：$categoryName"
            if (billIndex != null) AppLogger.d(requestId, "分类匹配", message, billIndex)
            else AppLogger.d(requestId, "分类匹配", message)
            return MatchResult(
                type = "expense",
                category = categoryName,
                subCategory = null,
                source = MatchSource.MAPPING,
                confidence = mappingResult.confidence
            )
        }

        val ruleResult = RuleMatcher.match(request, requestId, billIndex)
        if (ruleResult != null && ruleResult.confidence >= 0.9f) {
            if (ruleResult.category != "其他支出") {
                return ruleResult
            }
        }

        val timeResult = TimeRuleMatcher.match(request, requestId, billIndex)
        if (timeResult != null) {
            return timeResult
        }

        val aiHint = request.categoryHint
        if (!aiHint.isNullOrBlank()) {
            val aiCategory = CategoryService.getCategoryByName("expense", aiHint)
            if (aiCategory != null) {
                val matchType = if (aiCategory.name == aiHint.trim()) "EXACT" else "CONTAINS"
                val message = "AI推断命中: hint=$aiHint → category=${aiCategory.name}, matchType=$matchType, confidence=0.6, source=AI_HINT"
                if (billIndex != null) AppLogger.d(requestId, "分类匹配", message, billIndex)
                else AppLogger.d(requestId, "分类匹配", message)
                if (billIndex != null) AppLogger.decision(requestId, "分类匹配", aiHint, "AI推断命中，分类:${aiCategory.name}", 0.6f, "ai_hint", billIndex)
                else AppLogger.decision(requestId, "分类匹配", aiHint, "AI推断命中，分类:${aiCategory.name}", 0.6f, "ai_hint")
                return MatchResult(
                    type = "expense",
                    category = aiCategory.name,
                    source = MatchSource.AI_HINT,
                    confidence = 0.6f
                )
            }
        }

        // AI 兜底分类纠正
        val aiClassifier = this.aiClassifier
        if (aiClassifier != null) {
            val aiResult = aiClassifier.correct(request.description, requestId, billIndex)
            if (aiResult != null) {
                val aiMessage = "待匹配：${request.description}，来源：ai_correction，分类：${aiResult.category}，最终分类：${aiResult.category}"
                if (billIndex != null) AppLogger.d(requestId, "分类匹配", aiMessage, billIndex)
                else AppLogger.d(requestId, "分类匹配", aiMessage)
                if (billIndex != null) AppLogger.decision(requestId, "分类匹配", request.description, "AI兜底纠正，分类:${aiResult.category}", 0.6f, "ai_correction", billIndex)
                else AppLogger.decision(requestId, "分类匹配", request.description, "AI兜底纠正，分类:${aiResult.category}", 0.6f, "ai_correction")
                return aiResult
            }
        }

        val fallbackCategory = getDefaultCategory(request.time)
        val message = "待匹配：${request.description}，来源：fallback，分类：$fallbackCategory，最终分类：$fallbackCategory"
        if (billIndex != null) AppLogger.d(requestId, "分类匹配", message, billIndex)
        else AppLogger.d(requestId, "分类匹配", message)
        if (billIndex != null) AppLogger.decision(requestId, "分类匹配", request.description, "无匹配规则，使用默认分类:$fallbackCategory", 0.3f, "fallback", billIndex)
        else AppLogger.decision(requestId, "分类匹配", request.description, "无匹配规则，使用默认分类:$fallbackCategory", 0.3f, "fallback")
        return MatchResult(
            type = "expense",
            category = fallbackCategory,
            source = MatchSource.FALLBACK,
            confidence = 0.3f
        )
    }

    private fun getDefaultCategory(@Suppress("UNUSED_PARAMETER") time: Date): String {
        return "其他支出"
    }
}