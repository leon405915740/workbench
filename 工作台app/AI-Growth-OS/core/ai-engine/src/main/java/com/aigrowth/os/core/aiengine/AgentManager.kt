package com.aigrowth.os.core.aiengine

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Agent接口
 */
interface AIAgent {
    val agentName: String
    suspend fun process(request: AIRequest): Result<AIResponse>
}

/**
 * Agent管理器
 * 注册和管理各种AI Agent，统一的Agent调用接口
 */
@Singleton
class AgentManager @Inject constructor(
    private val aiClient: AIClient,
    private val responseParser: ResponseParser,
    private val learningAgent: LearningAgent,
    private val creatorAgent: CreatorAgent
) {
    private val agents = mutableMapOf<String, AIAgent>()

    init {
        registerAgent(learningAgent)
        registerAgent(creatorAgent)
    }
    
    /**
     * 注册Agent
     */
    fun registerAgent(agent: AIAgent) {
        agents[agent.agentName] = agent
    }
    
    /**
     * 获取Agent
     */
    fun getAgent(agentName: String): AIAgent? {
        return agents[agentName]
    }
    
    /**
     * 调用Agent
     */
    suspend fun callAgent(agentName: String, request: AIRequest): Result<AIResponse> {
        val agent = agents[agentName]
            ?: return Result.failure(Exception("Agent not found: $agentName"))
        
        return agent.process(request)
    }
    
    /**
     * 获取所有Agent名称
     */
    fun getAgentNames(): List<String> {
        return agents.keys.toList()
    }
}