package com.aigrowth.os.feature.learning.domain

import com.aigrowth.os.core.database.dao.GoalDao
import com.aigrowth.os.core.database.entity.Goal
import com.aigrowth.os.core.database.entity.GoalStatus
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 目标仓库
 */
@Singleton
class GoalRepository @Inject constructor(
    private val goalDao: GoalDao
) {
    /**
     * 获取所有目标
     */
    fun getAllGoals(): Flow<List<Goal>> {
        return goalDao.getAllGoals()
    }
    
    /**
     * 获取进行中的目标
     */
    fun getActiveGoals(): Flow<List<Goal>> {
        return goalDao.getGoalsByStatus(GoalStatus.ACTIVE)
    }
    
    /**
     * 获取目标详情
     */
    suspend fun getGoalById(id: String): Goal? {
        return goalDao.getGoalById(id)
    }
    
    /**
     * 创建目标
     */
    suspend fun createGoal(
        title: String,
        description: String
    ): Goal {
        val now = System.currentTimeMillis()
        val goal = Goal(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            status = GoalStatus.ACTIVE,
            learningPathId = null,
            createdAt = now,
            updatedAt = now
        )
        goalDao.insertGoal(goal)
        return goal
    }
    
    /**
     * 更新目标
     */
    suspend fun updateGoal(goal: Goal) {
        val updatedGoal = goal.copy(updatedAt = System.currentTimeMillis())
        goalDao.updateGoal(updatedGoal)
    }
    
    /**
     * 删除目标
     */
    suspend fun deleteGoal(goal: Goal) {
        goalDao.deleteGoal(goal)
    }
    
    /**
     * 更新目标状态
     */
    suspend fun updateGoalStatus(id: String, status: GoalStatus) {
        goalDao.updateGoalStatus(id, status)
    }
    
    /**
     * 关联学习路径
     */
    suspend fun linkLearningPath(goalId: String, learningPathId: String) {
        val goal = goalDao.getGoalById(goalId) ?: return
        val updatedGoal = goal.copy(
            learningPathId = learningPathId,
            updatedAt = System.currentTimeMillis()
        )
        goalDao.updateGoal(updatedGoal)
    }
}