package com.aigrowth.os.core.aiengine

import com.aigrowth.os.core.database.dao.AIMemoryDao
import com.aigrowth.os.core.database.entity.AIMemory
import com.aigrowth.os.core.database.entity.MemoryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI记忆管理器
 * 管理用户记忆的提取、存储、查询和注入
 */
@Singleton
class MemoryManager @Inject constructor(
    private val aiMemoryDao: AIMemoryDao,
    private val aiClient: AIClient,
    private val responseParser: ResponseParser,
    private val promptManager: PromptManager,
    private val apiKeyService: ApiKeyService
) {

    /**
     * 获取所有记忆
     */
    fun getAllMemories(): Flow<List<AIMemory>> {
        return aiMemoryDao.getAllMemories()
    }

    /**
     * 按类型获取记忆
     */
    fun getMemoriesByType(type: MemoryType): Flow<List<AIMemory>> {
        return aiMemoryDao.getMemoriesByType(type)
    }

    /**
     * 保存记忆
     */
    suspend fun saveMemory(
        type: MemoryType,
        content: String,
        importance: Int = 3,
        sourceType: String = "manual",
        sourceId: String? = null
    ) {
        val now = System.currentTimeMillis()
        val memory = AIMemory(
            id = UUID.randomUUID().toString(),
            memoryType = type,
            content = content,
            importance = importance.coerceIn(1, 5),
            sourceType = sourceType,
            sourceId = sourceId,
            createdAt = now,
            lastAccessedAt = now
        )
        aiMemoryDao.insertMemory(memory)
    }

    /**
     * 更新记忆
     */
    suspend fun updateMemory(memory: AIMemory) {
        aiMemoryDao.updateMemory(memory)
    }

    /**
     * 删除记忆
     */
    suspend fun deleteMemory(memory: AIMemory) {
        aiMemoryDao.deleteMemory(memory)
    }

    /**
     * 从对话中自动提取记忆
     */
    suspend fun extractMemoriesFromConversation(
        conversationText: String,
        apiKey: String
    ): Result<List<ExtractedMemory>> {
        val systemPrompt = promptManager.getSystemPersonalityPrompt()
        val userPrompt = promptManager.getMemoryExtractionPrompt(conversationText)

        val request = AIRequest(
            systemPrompt = systemPrompt,
            userMessage = userPrompt
        )

        val response = aiClient.call(apiKey, apiKeyService.getSelectedModel(), request)

        return response.fold(
            onSuccess = { aiResponse ->
                responseParser.parseMemoryExtractionResponse(aiResponse.content)
            },
            onFailure = {
                Result.failure(it)
            }
        )
    }

    /**
     * 保存提取的记忆到数据库
     */
    suspend fun saveExtractedMemories(
        extractedMemories: List<ExtractedMemory>,
        sourceType: String = "conversation",
        sourceId: String? = null
    ) {
        extractedMemories.forEach { extracted ->
            // 检查是否已存在相似记忆（简单去重）
            val existing = getAllMemories().first().any { existing ->
                existing.memoryType.name == extracted.type &&
                existing.content == extracted.content
            }

            if (!existing) {
                saveMemory(
                    type = MemoryType.valueOf(extracted.type),
                    content = extracted.content,
                    importance = extracted.importance,
                    sourceType = sourceType,
                    sourceId = sourceId
                )
            }
        }
    }

    /**
     * 获取用于AI上下文的记忆
     * 返回最重要的若干条记忆
     */
    suspend fun getMemoriesForContext(
        limit: Int = 10,
        types: List<MemoryType>? = null
    ): List<AIMemory> {
        val allMemories = getAllMemories().first()
        val filtered = if (types != null) {
            allMemories.filter { it.memoryType in types }
        } else {
            allMemories
        }
        return filtered
            .sortedByDescending { it.importance }
            .take(limit)
            .also { memories ->
                // 更新访问时间
                val now = System.currentTimeMillis()
                memories.forEach { memory ->
                    aiMemoryDao.updateLastAccessed(memory.id, now)
                }
            }
    }

    /**
     * 记录用户成就
     */
    suspend fun recordAchievement(
        content: String,
        importance: Int = 4,
        sourceId: String? = null
    ) {
        saveMemory(
            type = MemoryType.ACHIEVEMENT,
            content = content,
            importance = importance,
            sourceType = "achievement",
            sourceId = sourceId
        )
    }

    /**
     * 记录用户薄弱点
     */
    suspend fun recordWeakness(
        content: String,
        importance: Int = 4,
        sourceId: String? = null
    ) {
        saveMemory(
            type = MemoryType.WEAKNESS,
            content = content,
            importance = importance,
            sourceType = "evaluation",
            sourceId = sourceId
        )
    }
}
