package com.aigrowth.os.core.aiengine

import com.aigrowth.os.core.database.entity.AgentType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 创作Agent
 * 处理自媒体创作相关的AI请求
 */
@Singleton
class CreatorAgent @Inject constructor(
    private val aiClient: AIClient,
    private val responseParser: ResponseParser,
    private val promptManager: PromptManager,
    private val apiKeyService: ApiKeyService
) : AIAgent {

    override val agentName: String = AgentType.CREATOR_AGENT.name

    override suspend fun process(request: AIRequest): Result<AIResponse> {
        val apiKey = apiKeyService.getApiKey()
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("API Key 未配置，请在设置中设置"))
        }
        return aiClient.call(apiKey, apiKeyService.getSelectedModel(), request)
    }

    /**
     * 生成内容创意
     * 基于学习主题和目标受众，生成自媒体内容创意
     */
    suspend fun generateContentIdea(
        topic: String,
        targetAudience: String,
        contentType: String,
        apiKey: String
    ): Result<ContentIdeaResponse> {
        val systemPrompt = promptManager.getSystemPersonalityPrompt()
        val userPrompt = promptManager.getContentIdeaPrompt(topic, targetAudience, contentType)

        val request = AIRequest(
            systemPrompt = systemPrompt,
            userMessage = userPrompt
        )

        val response = aiClient.call(apiKey, apiKeyService.getSelectedModel(), request)

        return response.fold(
            onSuccess = { aiResponse ->
                responseParser.parseJson(aiResponse.content, ContentIdeaResponse::class.java)
            },
            onFailure = {
                Result.failure(it)
            }
        )
    }

    /**
     * 生成成长报告
     * 基于学习数据，生成可作为自媒体内容的成长报告
     */
    suspend fun generateGrowthReport(
        learningData: String,
        reportType: String,
        apiKey: String
    ): Result<GrowthReportResponse> {
        val systemPrompt = promptManager.getSystemPersonalityPrompt()
        val userPrompt = promptManager.getGrowthReportPrompt(learningData, reportType)

        val request = AIRequest(
            systemPrompt = systemPrompt,
            userMessage = userPrompt
        )

        val response = aiClient.call(apiKey, apiKeyService.getSelectedModel(), request)

        return response.fold(
            onSuccess = { aiResponse ->
                responseParser.parseJson(aiResponse.content, GrowthReportResponse::class.java)
            },
            onFailure = {
                Result.failure(it)
            }
        )
    }

    /**
     * 爆款内容分析
     * 分析爆款内容的结构和要素
     */
    suspend fun analyzeViralContent(
        contentTitle: String,
        contentUrl: String?,
        apiKey: String
    ): Result<ViralAnalysisResponse> {
        val systemPrompt = promptManager.getSystemPersonalityPrompt()
        val userPrompt = promptManager.getViralAnalysisPrompt(contentTitle, contentUrl)

        val request = AIRequest(
            systemPrompt = systemPrompt,
            userMessage = userPrompt
        )

        val response = aiClient.call(apiKey, apiKeyService.getSelectedModel(), request)

        return response.fold(
            onSuccess = { aiResponse ->
                responseParser.parseJson(aiResponse.content, ViralAnalysisResponse::class.java)
            },
            onFailure = {
                Result.failure(it)
            }
        )
    }

    /**
     * 生成内容脚本
     * 基于创意生成详细的内容脚本
     */
    suspend fun generateContentScript(
        idea: String,
        platform: String,
        durationMinutes: Int,
        apiKey: String
    ): Result<ContentScriptResponse> {
        val systemPrompt = promptManager.getSystemPersonalityPrompt()
        val userPrompt = promptManager.getContentScriptPrompt(idea, platform, durationMinutes)

        val request = AIRequest(
            systemPrompt = systemPrompt,
            userMessage = userPrompt
        )

        val response = aiClient.call(apiKey, apiKeyService.getSelectedModel(), request)

        return response.fold(
            onSuccess = { aiResponse ->
                responseParser.parseJson(aiResponse.content, ContentScriptResponse::class.java)
            },
            onFailure = {
                Result.failure(it)
            }
        )
    }

    /**
     * 推荐学习资源
     * 基于学习主题和水平，推荐适合的资源
     */
    suspend fun recommendResources(
        topic: String,
        userLevel: String,
        apiKey: String
    ): Result<ResourceRecommendationResponse> {
        val systemPrompt = promptManager.getSystemPersonalityPrompt()
        val userPrompt = promptManager.getResourceRecommendationPrompt(topic, userLevel)

        val request = AIRequest(
            systemPrompt = systemPrompt,
            userMessage = userPrompt
        )

        val response = aiClient.call(apiKey, apiKeyService.getSelectedModel(), request)

        return response.fold(
            onSuccess = { aiResponse ->
                responseParser.parseJson(aiResponse.content, ResourceRecommendationResponse::class.java)
            },
            onFailure = {
                Result.failure(it)
            }
        )
    }

    /**
     * 生成7天学习计划
     * 基于学习目标生成7天的详细计划
     */
    suspend fun generateWeeklyPlan(
        goal: String,
        availableDays: Int,
        apiKey: String
    ): Result<WeeklyPlanResponse> {
        val systemPrompt = promptManager.getSystemPersonalityPrompt()
        val userPrompt = promptManager.getWeeklyPlanPrompt(goal, availableDays)

        val request = AIRequest(
            systemPrompt = systemPrompt,
            userMessage = userPrompt
        )

        val response = aiClient.call(apiKey, apiKeyService.getSelectedModel(), request)

        return response.fold(
            onSuccess = { aiResponse ->
                responseParser.parseJson(aiResponse.content, WeeklyPlanResponse::class.java)
            },
            onFailure = {
                Result.failure(it)
            }
        )
    }

    private fun getApiKey(): String {
        return apiKeyService.getApiKey()
    }
}

// ===== 创作响应数据模型 =====

/**
 * 内容创意响应
 */
data class ContentIdeaResponse(
    val ideas: List<ContentIdea>,
    val topicAnalysis: String,
    val targetAudienceInsights: String
)

/**
 * 内容创意
 */
data class ContentIdea(
    val title: String,
    val hook: String,              // 开场钩子
    val keyPoints: List<String>,   // 核心要点
    val estimatedDuration: Int,    // 预估时长（秒）
    val targetPlatforms: List<String>,
    val difficulty: String         // 难度：简单/中等/困难
)

/**
 * 成长报告响应
 */
data class GrowthReportResponse(
    val title: String,
    val summary: String,
    val keyAchievements: List<String>,
    val lessonsLearned: List<String>,
    val growthCurve: String,           // 成长曲线描述
    val nextSteps: List<String>,
    val shareableContent: String,      // 可分享的内容
    val hashtags: List<String> = emptyList()  // 社交媒体标签
)

/**
 * 爆款分析响应
 */
data class ViralAnalysisResponse(
    val titleAnalysis: String,         // 标题分析
    val hookAnalysis: String,          // 钩子分析
    val structureAnalysis: String,     // 结构分析
    val emotionalAppeal: String,       // 情感诉求
    val targetAudience: String,        // 目标受众
    val conversionPath: String,        // 转化路径
    val actionableInsights: List<String> // 可操作建议
)

/**
 * 内容脚本响应
 */
data class ContentScriptResponse(
    val title: String,
    val hook: String,
    val scenes: List<Scene>,
    val callToAction: String,
    val hashtags: List<String>
)

/**
 * 脚本场景
 */
data class Scene(
    val order: Int,
    val duration: Int,          // 时长（秒）
    val visual: String,         // 视觉描述
    val narration: String,      // 旁白/字幕
    val notes: String?          // 备注
)

/**
 * 资源推荐响应
 */
data class ResourceRecommendationResponse(
    val topic: String,
    val userLevel: String,
    val recommendedResources: List<RecommendedResource>,
    val learningPathSuggestion: String
)

/**
 * 推荐资源
 */
data class RecommendedResource(
    val name: String,
    val type: String,            // 书籍/课程/文章/视频
    val description: String,
    val suitableFor: String,    // 适合人群
    val difficulty: String,     // 入门/进阶/高级
    val duration: String,       // 预估学习时长
    val url: String?            // 链接（可选）
)

/**
 * 周计划响应
 */
data class WeeklyPlanResponse(
    val goal: String,
    val planSummary: String,
    val dailyPlans: List<DailyPlan>,
    val tips: List<String>
)

/**
 * 每日计划
 */
data class DailyPlan(
    val day: Int,               // 1-7
    val theme: String,          // 学习主题
    val tasks: List<String>,    // 任务列表
    val estimatedMinutes: Int, // 预估时长
    val resources: List<String> // 推荐资源
)
