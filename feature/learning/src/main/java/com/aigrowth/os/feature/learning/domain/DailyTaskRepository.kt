package com.aigrowth.os.feature.learning.domain

import com.aigrowth.os.core.aiengine.DailyTaskResponse
import com.aigrowth.os.core.aiengine.LearningAgent
import com.aigrowth.os.core.database.dao.DailyTaskDao
import com.aigrowth.os.core.database.entity.DailyTask
import com.aigrowth.os.core.database.entity.TaskStatus
import com.aigrowth.os.core.database.entity.TaskType
import kotlinx.coroutines.flow.Flow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 每日任务仓库
 */
@Singleton
class DailyTaskRepository @Inject constructor(
    private val dailyTaskDao: DailyTaskDao,
    private val learningAgent: LearningAgent,
    private val growthRecordRepository: GrowthRecordRepository
) {
    
    /**
     * 获取今日任务
     */
    fun getTodayTasks(): Flow<List<DailyTask>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis
        
        return dailyTaskDao.getTasksForDay(startOfDay, endOfDay)
    }
    
    /**
     * 获取任务详情
     */
    suspend fun getTaskById(id: String): DailyTask? {
        return dailyTaskDao.getTaskById(id)
    }
    
    /**
     * AI生成每日任务
     */
    suspend fun generateDailyTasks(
        levelId: String,
        levelTitle: String,
        previousTasks: String,
        apiKey: String
    ): Result<List<DailyTask>> {
        val result = learningAgent.generateDailyTask(levelTitle, previousTasks, apiKey)
        
        return result.fold(
            onSuccess = { response ->
                val tasks = saveDailyTasks(levelId, response)
                Result.success(tasks)
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }
    
    /**
     * 保存每日任务
     */
    private suspend fun saveDailyTasks(
        levelId: String,
        response: DailyTaskResponse
    ): List<DailyTask> {
        val now = System.currentTimeMillis()
        val tasks = response.tasks.map { taskResponse ->
            DailyTask(
                id = UUID.randomUUID().toString(),
                learningLevelId = levelId,
                taskType = TaskType.valueOf(taskResponse.taskType.uppercase()),
                title = taskResponse.title,
                description = taskResponse.description,
                estimatedTime = taskResponse.estimatedMinutes,
                scheduledDate = now,
                status = TaskStatus.PENDING,
                userResponse = null,
                aiFeedback = null,
                score = null,
                completedAt = null,
                createdAt = now
            )
        }
        
        dailyTaskDao.insertTasks(tasks)
        return tasks
    }
    
    /**
     * 完成任务
     */
    suspend fun completeTask(taskId: String) {
        dailyTaskDao.updateTaskStatus(taskId, TaskStatus.COMPLETED, System.currentTimeMillis())
        growthRecordRepository.recordTodayGrowth()
    }
    
    /**
     * 跳过任务
     */
    suspend fun skipTask(taskId: String) {
        dailyTaskDao.updateTaskStatus(taskId, TaskStatus.SKIPPED, null)
        growthRecordRepository.recordTodayGrowth()
    }

    /**
     * 手动添加任务（本地闭环，不依赖AI）
     */
    suspend fun addManualTask(
        title: String,
        description: String,
        estimatedTime: Int,
        taskType: TaskType
    ): DailyTask {
        val now = System.currentTimeMillis()
        val task = DailyTask(
            id = UUID.randomUUID().toString(),
            learningLevelId = null,
            taskType = taskType,
            title = title,
            description = description,
            estimatedTime = estimatedTime,
            scheduledDate = now,
            status = TaskStatus.PENDING,
            userResponse = null,
            aiFeedback = null,
            score = null,
            completedAt = null,
            createdAt = now
        )
        dailyTaskDao.insertTask(task)
        return task
    }
    
    /**
     * 保存用户回答和AI反馈
     */
    suspend fun saveTaskResponse(
        taskId: String,
        userResponse: String,
        aiFeedback: String,
        score: Int
    ) {
        val task = dailyTaskDao.getTaskById(taskId) ?: return
        val updatedTask = task.copy(
            userResponse = userResponse,
            aiFeedback = aiFeedback,
            score = score,
            status = TaskStatus.COMPLETED,
            completedAt = System.currentTimeMillis()
        )
        dailyTaskDao.updateTask(updatedTask)
        growthRecordRepository.recordTodayGrowth()
    }
}