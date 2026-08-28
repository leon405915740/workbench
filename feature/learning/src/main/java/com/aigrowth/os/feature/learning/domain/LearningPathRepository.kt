package com.aigrowth.os.feature.learning.domain

import com.aigrowth.os.core.aiengine.LearningAgent
import com.aigrowth.os.core.aiengine.LearningPathResponse
import com.aigrowth.os.core.database.dao.LearningLevelDao
import com.aigrowth.os.core.database.dao.LearningPathDao
import com.aigrowth.os.core.database.entity.LearningLevel
import com.aigrowth.os.core.database.entity.LearningPath
import com.aigrowth.os.core.database.entity.LevelStatus
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 学习路径仓库
 */
@Singleton
class LearningPathRepository @Inject constructor(
    private val learningPathDao: LearningPathDao,
    private val learningLevelDao: LearningLevelDao,
    private val learningAgent: LearningAgent
) {
    private val gson = Gson()
    
    /**
     * 获取学习路径
     */
    fun getLearningPath(goalId: String): Flow<LearningPath?> {
        return learningPathDao.getLearningPathByGoal(goalId)
    }
    
    /**
     * 获取学习等级
     */
    fun getLearningLevels(learningPathId: String): Flow<List<LearningLevel>> {
        return learningLevelDao.getLevelsByLearningPath(learningPathId)
    }
    
    /**
     * AI生成学习路线
     */
    suspend fun generateLearningPath(
        goalId: String,
        topic: String,
        userLevel: String,
        apiKey: String
    ): Result<LearningPath> {
        // 调用AI生成学习路线
        val result = learningAgent.generateLearningPath(topic, userLevel, apiKey)
        
        return result.fold(
            onSuccess = { response ->
                // 保存学习路径
                val learningPath = saveLearningPath(goalId, response)
                
                // 保存学习等级
                saveLearningLevels(learningPath.id, response.levels)
                
                Result.success(learningPath)
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }
    
    /**
     * 保存学习路径
     */
    private suspend fun saveLearningPath(
        goalId: String,
        response: LearningPathResponse
    ): LearningPath {
        val now = System.currentTimeMillis()
        val learningPath = LearningPath(
            id = UUID.randomUUID().toString(),
            goalId = goalId,
            title = response.title,
            description = response.description,
            totalLevels = response.levels.size,
            currentLevel = 1,
            createdAt = now,
            updatedAt = now
        )
        
        learningPathDao.insertLearningPath(learningPath)
        return learningPath
    }
    
    /**
     * 保存学习等级
     */
    private suspend fun saveLearningLevels(
        learningPathId: String,
        levels: List<com.aigrowth.os.core.aiengine.LevelResponse>
    ) {
        val now = System.currentTimeMillis()
        val learningLevels = levels.mapIndexed { index, level ->
            LearningLevel(
                id = UUID.randomUUID().toString(),
                learningPathId = learningPathId,
                levelNumber = level.levelNumber,
                title = level.title,
                objective = level.objective,
                knowledgePoints = gson.toJson(level.knowledgePoints),
                commonMistakes = gson.toJson(level.commonMistakes),
                successCriteria = level.successCriteria,
                status = if (index == 0) LevelStatus.UNLOCKED else LevelStatus.LOCKED,
                startedAt = null,
                completedAt = null,
                createdAt = now
            )
        }
        
        learningLevelDao.insertLevels(learningLevels)
    }
    
    /**
     * 更新学习等级状态
     */
    suspend fun updateLevelStatus(levelId: String, status: LevelStatus) {
        learningLevelDao.updateLevelStatus(levelId, status)
    }
}