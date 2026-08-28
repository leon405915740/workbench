package com.aigrowth.os.core.aiengine

import com.aigrowth.os.core.database.entity.AgentType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 学习Agent
 * 处理学习相关的AI请求
 */
@Singleton
class LearningAgent @Inject constructor(
    private val aiClient: AIClient,
    private val responseParser: ResponseParser,
    private val promptManager: PromptManager,
    private val contextBuilder: ContextBuilder,
    private val apiKeyService: ApiKeyService
) : AIAgent {
    
    override val agentName: String = AgentType.LEARNING_AGENT.name
    
    override suspend fun process(request: AIRequest): Result<AIResponse> {
        val apiKey = apiKeyService.getApiKey()
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("API Key 未配置，请在设置中设置"))
        }
        return aiClient.call(apiKey, apiKeyService.getSelectedModel(), request)
    }
    
    /**
     * 生成学习路线
     */
    suspend fun generateLearningPath(
        topic: String,
        userLevel: String,
        apiKey: String
    ): Result<LearningPathResponse> {
        val systemPrompt = promptManager.getSystemPersonalityPrompt()
        val userPrompt = promptManager.getLearningPathPrompt(topic, userLevel)
        
        val request = AIRequest(
            systemPrompt = systemPrompt,
            userMessage = userPrompt
        )
        
        val response = aiClient.call(apiKey, apiKeyService.getSelectedModel(), request)
        
        return response.fold(
            onSuccess = { aiResponse ->
                responseParser.parseLearningPathResponse(aiResponse.content)
            },
            onFailure = { 
                Result.failure(it) 
            }
        )
    }
    
    /**
     * 生成每日任务
     */
    suspend fun generateDailyTask(
        levelTitle: String,
        previousTasks: String,
        apiKey: String
    ): Result<DailyTaskResponse> {
        val systemPrompt = promptManager.getSystemPersonalityPrompt()
        val userPrompt = promptManager.getDailyTaskPrompt(levelTitle, previousTasks)
        
        val request = AIRequest(
            systemPrompt = systemPrompt,
            userMessage = userPrompt
        )
        
        val response = aiClient.call(apiKey, apiKeyService.getSelectedModel(), request)
        
        return response.fold(
            onSuccess = { aiResponse ->
                responseParser.parseDailyTaskResponse(aiResponse.content)
            },
            onFailure = { 
                Result.failure(it) 
            }
        )
    }
    
    /**
     * 评估答案
     */
    suspend fun evaluateAnswer(
        task: String,
        userAnswer: String,
        apiKey: String
    ): Result<EvaluationResponse> {
        val systemPrompt = promptManager.getSystemPersonalityPrompt()
        val userPrompt = promptManager.getEvaluationPrompt(task, userAnswer)
        
        val request = AIRequest(
            systemPrompt = systemPrompt,
            userMessage = userPrompt
        )
        
        val response = aiClient.call(apiKey, apiKeyService.getSelectedModel(), request)
        
        return response.fold(
            onSuccess = { aiResponse ->
                responseParser.parseEvaluationResponse(aiResponse.content)
            },
            onFailure = { 
                Result.failure(it) 
            }
        )
    }
    
    /**
     * 生成知识卡片
     */
    suspend fun generateKnowledgeCard(
        topic: String,
        context: String,
        apiKey: String
    ): Result<KnowledgeCardResponse> {
        val systemPrompt = promptManager.getSystemPersonalityPrompt()
        val userPrompt = promptManager.getKnowledgeCardPrompt(topic, context)
        
        val request = AIRequest(
            systemPrompt = systemPrompt,
            userMessage = userPrompt
        )
        
        val response = aiClient.call(apiKey, apiKeyService.getSelectedModel(), request)
        
        return response.fold(
            onSuccess = { aiResponse ->
                responseParser.parseKnowledgeCardResponse(aiResponse.content)
            },
            onFailure = { 
                Result.failure(it) 
            }
        )
    }
    
    /**
     * 生成成长复盘报告
     */
    suspend fun generateGrowthReview(
        periodSummary: String,
        apiKey: String
    ): Result<GrowthReviewResponse> {
        val systemPrompt = promptManager.getSystemPersonalityPrompt()
        val userPrompt = promptManager.getGrowthReviewPrompt(periodSummary)

        val request = AIRequest(
            systemPrompt = systemPrompt,
            userMessage = userPrompt
        )

        val response = aiClient.call(apiKey, apiKeyService.getSelectedModel(), request)

        return response.fold(
            onSuccess = { aiResponse ->
                responseParser.parseJson(aiResponse.content, GrowthReviewResponse::class.java)
            },
            onFailure = {
                Result.failure(it)
            }
        )
    }

    /**
     * 费曼学习对话
     * 支持多轮对话
     */
    suspend fun feynmanDialog(
        topic: String,
        userExplanation: String,
        conversationHistory: List<FeynmanMessage> = emptyList(),
        apiKey: String
    ): Result<FeynmanResponse> {
        val systemPrompt = promptManager.getSystemPersonalityPrompt()
        val userPrompt = promptManager.getFeynmanPrompt(topic, userExplanation, conversationHistory)

        val request = AIRequest(
            systemPrompt = systemPrompt,
            userMessage = userPrompt
        )

        val response = aiClient.call(apiKey, apiKeyService.getSelectedModel(), request)

        return response.fold(
            onSuccess = { aiResponse ->
                responseParser.parseJson(aiResponse.content, FeynmanResponse::class.java)
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

/**
 * 费曼学习响应
 */
data class FeynmanResponse(
    val score: Int,
    val childResponse: String,
    val improvementSuggestions: List<String>,
    val isUnderstood: Boolean
)

/**
 * 费曼学习对话消息
 */
data class FeynmanMessage(
    val role: FeynmanRole,
    val content: String
)

enum class FeynmanRole {
    USER,       // 用户解释
    AI_CHILD    // AI扮演的12岁孩子
}