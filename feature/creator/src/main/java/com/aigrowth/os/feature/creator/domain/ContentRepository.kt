package com.aigrowth.os.feature.creator.domain

import com.aigrowth.os.core.aiengine.CreatorAgent
import com.aigrowth.os.core.aiengine.ContentIdeaResponse
import com.aigrowth.os.core.aiengine.ContentScriptResponse
import com.aigrowth.os.core.aiengine.GrowthReportResponse
import com.aigrowth.os.core.aiengine.ResourceRecommendationResponse
import com.aigrowth.os.core.aiengine.ViralAnalysisResponse
import com.aigrowth.os.core.aiengine.WeeklyPlanResponse
import com.aigrowth.os.core.database.dao.ContentDao
import com.aigrowth.os.core.database.entity.Content
import com.aigrowth.os.core.database.entity.ContentStatus
import com.aigrowth.os.core.database.entity.ContentType
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 内容仓库
 * 管理自媒体内容的CRUD和AI生成
 */
@Singleton
class ContentRepository @Inject constructor(
    private val contentDao: ContentDao,
    private val creatorAgent: CreatorAgent
) {

    fun getAllContents(): Flow<List<Content>> {
        return contentDao.getAllContents()
    }

    fun getContentsByType(contentType: ContentType): Flow<List<Content>> {
        return contentDao.getContentsByType(contentType)
    }

    fun getContentsByStatus(status: ContentStatus): Flow<List<Content>> {
        return contentDao.getContentsByStatus(status)
    }

    suspend fun getContentById(id: String): Content? {
        return contentDao.getContentById(id)
    }

    /**
     * 保存内容创意
     */
    suspend fun saveContent(
        title: String,
        contentType: ContentType,
        content: String,
        structure: String = "{}",
        tags: String = "[]"
    ): Content {
        val now = System.currentTimeMillis()
        val contentEntity = Content(
            id = UUID.randomUUID().toString(),
            title = title,
            contentType = contentType,
            content = content,
            structure = structure,
            tags = tags,
            status = ContentStatus.DRAFT,
            publishedAt = null,
            createdAt = now,
            updatedAt = now
        )
        contentDao.insertContent(contentEntity)
        return contentEntity
    }

    suspend fun updateContent(content: Content) {
        contentDao.updateContent(content.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteContent(content: Content) {
        contentDao.deleteContent(content)
    }

    /**
     * AI生成内容创意
     */
    suspend fun generateContentIdea(
        topic: String,
        targetAudience: String,
        contentType: String,
        apiKey: String
    ): Result<ContentIdeaResponse> {
        return creatorAgent.generateContentIdea(topic, targetAudience, contentType, apiKey)
    }

    /**
     * AI生成成长报告
     */
    suspend fun generateGrowthReport(
        learningData: String,
        reportType: String,
        apiKey: String
    ): Result<GrowthReportResponse> {
        return creatorAgent.generateGrowthReport(learningData, reportType, apiKey)
    }

    /**
     * AI分析爆款内容
     */
    suspend fun analyzeViralContent(
        contentTitle: String,
        contentUrl: String?,
        apiKey: String
    ): Result<ViralAnalysisResponse> {
        return creatorAgent.analyzeViralContent(contentTitle, contentUrl, apiKey)
    }

    /**
     * AI生成内容脚本
     */
    suspend fun generateContentScript(
        idea: String,
        platform: String,
        durationMinutes: Int,
        apiKey: String
    ): Result<ContentScriptResponse> {
        return creatorAgent.generateContentScript(idea, platform, durationMinutes, apiKey)
    }

    /**
     * AI推荐学习资源
     */
    suspend fun recommendResources(
        topic: String,
        userLevel: String,
        apiKey: String
    ): Result<ResourceRecommendationResponse> {
        return creatorAgent.recommendResources(topic, userLevel, apiKey)
    }

    /**
     * AI生成7天学习计划
     */
    suspend fun generateWeeklyPlan(
        goal: String,
        availableDays: Int,
        apiKey: String
    ): Result<WeeklyPlanResponse> {
        return creatorAgent.generateWeeklyPlan(goal, availableDays, apiKey)
    }
}
