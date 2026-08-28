package com.aigrowth.os.core.aiengine

import com.aigrowth.os.core.database.entity.AIMemory
import com.aigrowth.os.core.database.entity.AIConversation
import com.aigrowth.os.core.database.entity.Goal
import com.aigrowth.os.core.database.entity.LearningLevel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI上下文构建器
 * 构建AI请求的上下文，整合用户记忆、学习进度、历史对话
 */
@Singleton
class ContextBuilder @Inject constructor(
    private val promptManager: PromptManager
) {
    
    /**
     * 构建学习路线生成的上下文
     */
    fun buildLearningPathContext(
        goal: String,
        userLevel: String,
        memories: List<AIMemory>
    ): AIRequest {
        val systemPrompt = promptManager.getSystemPersonalityPrompt()
        val userPrompt = promptManager.getLearningPathPrompt(goal, userLevel)
        
        // 注入用户记忆
        val memoryContext = buildMemoryContext(memories)
        val fullUserPrompt = if (memoryContext.isNotEmpty()) {
            "用户信息：\n$memoryContext\n\n$userPrompt"
        } else {
            userPrompt
        }
        
        return AIRequest(
            systemPrompt = systemPrompt,
            userMessage = fullUserPrompt
        )
    }
    
    /**
     * 构建每日任务生成的上下文
     */
    fun buildDailyTaskContext(
        level: LearningLevel,
        previousTasks: String,
        conversations: List<AIConversation>,
        memories: List<AIMemory>
    ): AIRequest {
        val systemPrompt = promptManager.getSystemPersonalityPrompt()
        val userPrompt = promptManager.getDailyTaskPrompt(
            level = "${level.levelNumber}. ${level.title}",
            previousTasks = previousTasks
        )
        
        // 注入对话历史和记忆
        val context = buildString {
            append("当前学习等级：${level.title}\n")
            append("学习目标：${level.objective}\n")
            
            if (memories.isNotEmpty()) {
                append("\n用户学习特点：\n")
                memories.forEach { memory ->
                    append("- ${memory.memoryType.name}: ${memory.content}\n")
                }
            }
            
            if (conversations.isNotEmpty()) {
                append("\n最近的学习对话：\n")
                conversations.takeLast(5).forEach { conv ->
                    append("${conv.role.name}: ${conv.content}\n")
                }
            }
        }
        
        return AIRequest(
            systemPrompt = systemPrompt,
            userMessage = "$context\n\n$userPrompt"
        )
    }
    
    /**
     * 构建考核评分的上下文
     */
    fun buildEvaluationContext(
        task: String,
        userAnswer: String,
        memories: List<AIMemory>
    ): AIRequest {
        val systemPrompt = promptManager.getSystemPersonalityPrompt()
        val userPrompt = promptManager.getEvaluationPrompt(task, userAnswer)
        
        // 注入用户薄弱点
        val weaknesses = memories.filter { 
            it.memoryType.name == "WEAKNESS" 
        }.map { it.content }
        
        val fullUserPrompt = if (weaknesses.isNotEmpty()) {
            "用户薄弱点：${weaknesses.joinToString(", ")}\n\n$userPrompt"
        } else {
            userPrompt
        }
        
        return AIRequest(
            systemPrompt = systemPrompt,
            userMessage = fullUserPrompt
        )
    }
    
    /**
     * 构建知识卡片生成的上下文
     */
    fun buildKnowledgeCardContext(
        topic: String,
        learningContext: String,
        conversations: List<AIConversation>
    ): AIRequest {
        val systemPrompt = promptManager.getSystemPersonalityPrompt()
        val userPrompt = promptManager.getKnowledgeCardPrompt(topic, learningContext)
        
        // 提取对话中的关键内容
        val relevantConversations = conversations
            .filter { it.content.contains(topic, ignoreCase = true) }
            .takeLast(3)
        
        val context = buildString {
            if (relevantConversations.isNotEmpty()) {
                append("相关学习内容：\n")
                relevantConversations.forEach { conv ->
                    append("- ${conv.content}\n")
                }
            }
        }
        
        return AIRequest(
            systemPrompt = systemPrompt,
            userMessage = if (context.isNotEmpty()) "$context\n\n$userPrompt" else userPrompt
        )
    }
    
    /**
     * 构建用户记忆上下文
     */
    private fun buildMemoryContext(memories: List<AIMemory>): String {
        if (memories.isEmpty()) return ""
        
        return buildString {
            memories.groupBy { it.memoryType }.forEach { (type, memoryList) ->
                append("${type.name}：\n")
                memoryList.take(3).forEach { memory ->
                    append("- ${memory.content}\n")
                }
            }
        }
    }
}