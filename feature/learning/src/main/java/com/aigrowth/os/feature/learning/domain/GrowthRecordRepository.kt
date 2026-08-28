package com.aigrowth.os.feature.learning.domain

import com.aigrowth.os.core.aiengine.GrowthReviewResponse
import com.aigrowth.os.core.aiengine.LearningAgent
import com.aigrowth.os.core.database.dao.DailyTaskDao
import com.aigrowth.os.core.database.dao.GrowthRecordDao
import com.aigrowth.os.core.database.dao.KnowledgeCardDao
import com.aigrowth.os.core.database.entity.GrowthRecord
import com.aigrowth.os.core.database.entity.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 成长记录仓库
 * 管理成长数据的记录、查询和AI复盘
 */
@Singleton
class GrowthRecordRepository @Inject constructor(
    private val growthRecordDao: GrowthRecordDao,
    private val dailyTaskDao: DailyTaskDao,
    private val knowledgeCardDao: KnowledgeCardDao,
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
     * 观察今日成长记录（响应式，完成任务后自动刷新仪表盘）
     */
    fun observeTodayRecord(): Flow<GrowthRecord?> {
        return growthRecordDao.observeRecordByDate(getTodayStartMillis())
    }

    /**
     * 记录或更新今日成长数据
     */
    suspend fun recordTodayGrowth(): GrowthRecord {
        val today = getTodayStartMillis()
        val existing = growthRecordDao.getRecordByDate(today)

        // 统计今日数据
        val (startOfDay, endOfDay) = getTodayRange()
        val todayTasks = dailyTaskDao.getTasksForDay(startOfDay, endOfDay).first()
        val completedTasks = todayTasks.filter { it.status == TaskStatus.COMPLETED }
        val totalMinutes = completedTasks.sumOf { it.estimatedTime }

        // 统计今日知识卡片
        val allCards = knowledgeCardDao.getAllCards().first()
        val todayCards = allCards.count {
            it.createdAt >= startOfDay && it.createdAt < endOfDay
        }

        // 计算平均掌握度
        val avgMastery = if (todayCards > 0) {
            allCards.filter { it.createdAt >= startOfDay && it.createdAt < endOfDay }
                .map { it.masteryScore }.average().toInt()
        } else {
            0
        }

        val now = System.currentTimeMillis()
        val record = if (existing != null) {
            existing.copy(
                learningMinutes = totalMinutes,
                tasksCompleted = completedTasks.size,
                knowledgeCardsCreated = todayCards,
                masteryScore = avgMastery,
                createdAt = now
            )
        } else {
            GrowthRecord(
                id = UUID.randomUUID().toString(),
                date = today,
                learningMinutes = totalMinutes,
                tasksCompleted = completedTasks.size,
                knowledgeCardsCreated = todayCards,
                masteryScore = avgMastery,
                aiSummary = null,
                createdAt = now
            )
        }

        growthRecordDao.insertRecord(record)
        return record
    }

    /**
     * 生成AI成长复盘报告
     */
    suspend fun generateGrowthReview(
        days: Int = 7,
        apiKey: String
    ): Result<GrowthReviewResponse> {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        val startTime = calendar.timeInMillis

        val records = growthRecordDao.getRecordsInRange(startTime, endTime).first()

        val periodSummary = buildPeriodSummary(records, days)
        val result = learningAgent.generateGrowthReview(periodSummary, apiKey)

        result.fold(
            onSuccess = { response ->
                records.firstOrNull()?.let { latestRecord ->
                    val updated = latestRecord.copy(aiSummary = response.encouragement)
                    growthRecordDao.updateRecord(updated)
                }
            },
            onFailure = { error ->
                android.util.Log.e("GrowthRecordRepo", "Growth review failed: ${error.message}", error)
            }
        )

        return result
    }

    /**
     * 构建周期数据摘要
     */
    private fun buildPeriodSummary(
        records: List<GrowthRecord>,
        days: Int
    ): String {
        if (records.isEmpty()) {
            return "过去${days}天没有学习记录"
        }

        val totalMinutes = records.sumOf { it.learningMinutes }
        val totalTasks = records.sumOf { it.tasksCompleted }
        val totalCards = records.sumOf { it.knowledgeCardsCreated }
        val avgMastery = if (records.isNotEmpty()) {
            records.map { it.masteryScore }.average().toInt()
        } else 0
        val activeDays = records.size

        return buildString {
            append("过去${days}天学习数据：\n")
            append("- 活跃学习天数：$activeDays 天\n")
            append("- 总学习时长：$totalMinutes 分钟\n")
            append("- 完成任务数：$totalTasks 个\n")
            append("- 创建知识卡片：$totalCards 张\n")
            append("- 平均掌握度：$avgMastery%\n\n")

            records.take(7).forEach { record ->
                val dateStr = java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault())
                    .format(java.util.Date(record.date))
                append("$dateStr: 学习${record.learningMinutes}分钟, 完成${record.tasksCompleted}任务")
                if (record.knowledgeCardsCreated > 0) {
                    append(", 创建${record.knowledgeCardsCreated}张知识卡片")
                }
                append("\n")
            }
        }
    }

    private fun getTodayStartMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getTodayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val end = calendar.timeInMillis
        return Pair(start, end)
    }
}
