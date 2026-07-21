package com.accounting.app.parser.intent

import com.accounting.app.data.repository.AppRepository
import com.accounting.app.log.AppLogger
import com.accounting.app.parser.time.TimeUtils
import com.accounting.app.domain.classification.CategoryService

data class MappingMatchResult(
    val categoryId: Long,
    val subcategoryId: Long?,
    val keyword: String,
    val source: String,
    val confidence: Float
)

object MappingMatcher {

    private var repository: AppRepository? = null

    fun init(repo: AppRepository) {
        repository = repo
    }

    suspend fun match(type: String, text: String, requestId: String, billIndex: Int = 1): MappingMatchResult? {
        val repo = repository ?: return null
        val mapping = repo.matchMapping(type, text) ?: return null

        val confidence = when (mapping.source) {
            "MANUAL" -> 0.95f
            "AUTO" -> 0.8f
            else -> 0.7f
        }

        val category = CategoryService.getCategoryById(mapping.categoryId)?.name ?: "其他"
        val subcategory = mapping.subcategoryId?.let { CategoryService.getCategoryById(it)?.name }
        val message = "映射命中（${mapping.source}）→ 关键词：${mapping.keyword}，分类：${category}-${subcategory ?: ""}"
        AppLogger.d(requestId, "分类匹配", message, billIndex)
        AppLogger.decision(requestId, "分类匹配", mapping.keyword, "关键词映射命中，分类:$category", confidence, mapping.source, billIndex)

        recordHit(mapping.keyword, mapping.type)

        return MappingMatchResult(
            categoryId = mapping.categoryId,
            subcategoryId = mapping.subcategoryId,
            keyword = mapping.keyword,
            source = mapping.source,
            confidence = confidence
        )
    }

    private suspend fun recordHit(keyword: String, type: String) {
        val repo = repository ?: return
        val mapping = repo.findMappingByKeywordAndType(keyword, type)
        if (mapping != null) {
            repo.incrementMappingHitCount(mapping.id)
        }
    }
}