package com.aigrowth.os.feature.learning.domain

import com.aigrowth.os.core.aiengine.GrowthReviewResponse
import com.aigrowth.os.core.aiengine.LearningAgent
import com.aigrowth.os.core.database.dao.GrowthRecordDao
import com.aigrowth.os.core.database.entity.GrowthRecord
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 成长数据仓库
 * 管理成长记录的CRUD和AI成长复盘
 */
@Singleton
class GrowthRepository @Inject constructor(
    private val growthRecordDao: GrowthRecordDao,
    private val learningAgent: LearningAgent
) {

    fun getAllRecords(): Flow<List<GrowthRecord>> {
        return growthRecordDao.getAllRecords()
    }

    fun getRecentRecords(): Flow<List<GrowthRecord>> {
        return growthRecordDao.getRecentRecords()
    }

    fun getRecordsInRange(startDate: Long, endDate: Long): Flow<List<GrowthRecord>> {
        return growthRecordDao.getRecordsInRange(startDate, endDate)
    }

    suspend fun getRecordByDate(date: Long): GrowthRecord? {
        return growthRecordDao.getRecordByDate(date)
    }

    /**
     * 保存或更新今日成长记录
     */
    suspend fun saveTodayRecord(
        learningMinutes: Int,
        tasksCompleted: Int,
        knowledgeCardsCreated: Int,
        masteryScore: Int
    ): GrowthRecord {
        val now = System.currentTimeMillis()
        // 获取当天的起始时间（0点）
        val dayStart = getDayStart(now)

        val existing = growthRecordDao.getRecordByDate(dayStart)
        val record = if (existing != null) {
            existing.copy(
                learningMinutes = learningMinutes,
                tasksCompleted = tasksCompleted,
                knowledgeCardsCreated = knowledgeCardsCreated,
                masteryScore = masteryScore,
                createdAt = now
            )
        } else {
            GrowthRecord(
                id = UUID.randomUUID().toString(),
                date = dayStart,
                learningMinutes = learningMinutes,
                tasksCompleted = tasksCompleted,
                knowledgeCardsCreated = knowledgeCardsCreated,
                masteryScore = masteryScore,
                aiSummary = null,
                createdAt = now
            )
        }
        growthRecordDao.insertRecord(record)
        return record
    }

    /**
     * 生成AI成长复盘
     */
    suspend fun generateGrowthReview(
        records: List<GrowthRecord>,
        apiKey: String
    ): Result<GrowthReviewResponse> {
        val periodSummary = buildPeriodSummary(records)
        return learningAgent.generateGrowthReview(periodSummary, apiKey)
    }

    private fun getDayStart(timestamp: Long): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun buildPeriodSummary(records: List<GrowthRecord>): String {
        val totalMinutes = records.sumOf { it.learningMinutes }
        val totalTasks = records.sumOf { it.tasksCompleted }
        val totalCards = records.sumOf { it.knowledgeCardsCreated }
        val avgMastery = if (records.isNotEmpty()) records.sumOf { it.masteryScore } / records.size else 0

        return """
            学习时长: ${totalMinutes}分钟
            完成任务: ${totalTasks}个
            知识卡片: ${totalCards}张
            平均掌握度: ${avgMastery}%
            记录天数: ${records.size}天
        """.trimIndent()
    }
}