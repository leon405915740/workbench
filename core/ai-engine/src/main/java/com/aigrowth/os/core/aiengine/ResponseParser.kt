package com.aigrowth.os.core.aiengine

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 响应解析器
 * 解析AI响应，支持JSON格式解析，错误处理和数据验证
 */
@Singleton
class ResponseParser @Inject constructor() {
    
    private val gson = Gson()
    
    /**
     * 解析AI响应为指定类型
     */
    fun <T> parseJson(content: String, clazz: Class<T>): Result<T> {
        return try {
            // 尝试提取JSON部分（可能被markdown包裹）
            val jsonContent = extractJson(content)
            val result = gson.fromJson(jsonContent, clazz)
            Result.success(result)
        } catch (e: JsonSyntaxException) {
            Result.failure(Exception("JSON解析失败: ${e.message}", e))
        } catch (e: Exception) {
            Result.failure(Exception("响应解析失败: ${e.message}", e))
        }
    }
    
    /**
     * 解析学习路线响应
     */
    fun parseLearningPathResponse(content: String): Result<LearningPathResponse> {
        return parseJson(content, LearningPathResponse::class.java)
    }
    
    /**
     * 解析每日任务响应
     */
    fun parseDailyTaskResponse(content: String): Result<DailyTaskResponse> {
        return parseJson(content, DailyTaskResponse::class.java)
    }
    
    /**
     * 解析考核评分响应
     */
    fun parseEvaluationResponse(content: String): Result<EvaluationResponse> {
        return parseJson(content, EvaluationResponse::class.java)
    }
    
    /**
     * 解析知识卡片响应
     */
    fun parseKnowledgeCardResponse(content: String): Result<KnowledgeCardResponse> {
        return parseJson(content, KnowledgeCardResponse::class.java)
    }

    /**
     * 解析记忆提取响应
     */
    fun parseMemoryExtractionResponse(content: String): Result<List<ExtractedMemory>> {
        return try {
            val jsonContent = extractJson(content)
            val response = gson.fromJson(jsonContent, MemoryExtractionResponse::class.java)
            Result.success(response.memories ?: emptyList())
        } catch (e: JsonSyntaxException) {
            Result.failure(Exception("记忆提取解析失败: ${e.message}", e))
        } catch (e: Exception) {
            Result.failure(Exception("响应解析失败: ${e.message}", e))
        }
    }
    
    /**
     * 从markdown或混合内容中提取JSON
     */
    private fun extractJson(content: String): String {
        // 尝试直接解析
        if (content.trim().startsWith("{") || content.trim().startsWith("[")) {
            return content.trim()
        }
        
        // 尝试提取markdown代码块中的JSON
        val jsonBlockPattern = """```(?:json)?\s*([\s\S]*?)```""".toRegex()
        val match = jsonBlockPattern.find(content)
        if (match != null) {
            return match.groupValues[1].trim()
        }
        
        // 尝试找到第一个{或[到最后一个}或]
        val startIndex = content.indexOfFirst { it == '{' || it == '[' }
        val endIndex = content.indexOfLast { it == '}' || it == ']' }
        
        if (startIndex >= 0 && endIndex > startIndex) {
            return content.substring(startIndex, endIndex + 1)
        }
        
        return content.trim()
    }
}

// ===== 响应数据模型 =====

/**
 * 学习路线响应
 */
data class LearningPathResponse(
    val title: String,
    val description: String,
    val levels: List<LevelResponse>
)

/**
 * 学习等级响应
 */
data class LevelResponse(
    val levelNumber: Int,
    val title: String,
    val objective: String,
    val knowledgePoints: List<String>,
    val commonMistakes: List<String>,
    val successCriteria: String
)

/**
 * 每日任务响应
 */
data class DailyTaskResponse(
    val tasks: List<TaskResponse>
)

/**
 * 任务响应
 */
data class TaskResponse(
    val taskType: String,
    val title: String,
    val description: String,
    val estimatedMinutes: Int,
    val content: String?
)

/**
 * 考核评分响应
 */
data class EvaluationResponse(
    val score: Int,
    val understandingLevel: String,
    val applicationAbility: String,
    val errorAnalysis: String,
    val supplementaryKnowledge: String
)

/**
 * 知识卡片响应
 */
data class KnowledgeCardResponse(
    val topic: String,
    val coreDefinition: String,
    val keyConcepts: List<String>,
    val useCases: List<String>,
    val commonMistakes: List<String>,
    val checklist: List<String>,
    val selfTestQuestions: List<String>
)

/**
 * 成长复盘响应
 */
data class GrowthReviewResponse(
    val overallRating: String,
    val keyHighlights: List<String>,
    val areasForImprovement: List<String>,
    val nextWeekRecommendations: List<String>,
    val encouragement: String
)

/**
 * 记忆提取响应
 */
data class MemoryExtractionResponse(
    val memories: List<ExtractedMemory>?
)

/**
 * 提取的记忆
 */
data class ExtractedMemory(
    val type: String,
    val content: String,
    val importance: Int
)