package com.aigrowth.os.core.database

import android.content.Context
import com.aigrowth.os.core.database.entity.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据库初始化器
 * 首次启动时预填充示例数据
 */
@Singleton
class DatabaseInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase
) {
    private val prefsName = "AI_Growth_OS_Init"
    private val keyInitialized = "database_initialized"

    /**
     * 初始化数据库（如果需要）
     */
    suspend fun initializeIfNeeded() {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val isInitialized = prefs.getBoolean(keyInitialized, false)

        if (!isInitialized) {
            withContext(Dispatchers.IO) {
                populateSampleData()
            }
            prefs.edit().putBoolean(keyInitialized, true).apply()
        }
    }

    /**
     * 填充示例数据
     */
    private suspend fun populateSampleData() {
        val now = System.currentTimeMillis()

        // 1. 创建学习目标（先不关联学习路径，避免循环依赖）
        val goalId = UUID.randomUUID().toString()
        database.goalDao().insertGoal(
            Goal(
                id = goalId,
                title = "掌握AI驱动开发",
                description = "学习如何使用AI辅助编程，提升开发效率10倍",
                status = GoalStatus.ACTIVE,
                learningPathId = null,
                createdAt = now,
                updatedAt = now
            )
        )

        // 2. 创建学习路径
        val learningPathId = UUID.randomUUID().toString()
        database.learningPathDao().insertLearningPath(
            LearningPath(
                id = learningPathId,
                goalId = goalId,
                title = "AI驱动开发学习路径",
                description = "从基础到实战的完整学习计划",
                totalLevels = 4,
                currentLevel = 1,
                createdAt = now,
                updatedAt = now
            )
        )

        // 3. 更新目标关联学习路径
        val goal = database.goalDao().getGoalById(goalId)
        goal?.let {
            database.goalDao().updateGoal(
                it.copy(learningPathId = learningPathId, updatedAt = now)
            )
        }

        // 4. 创建学习阶段
        val levelIds = mutableListOf<String>()
        val levels = listOf(
            Quadruple("Level 1: AI编程基础", "了解AI编程工具和基本概念", 1, LevelStatus.IN_PROGRESS),
            Quadruple("Level 2: Prompt Engineering", "学习如何编写高质量的提示词", 2, LevelStatus.LOCKED),
            Quadruple("Level 3: 实战项目", "通过实际项目巩固所学知识", 3, LevelStatus.LOCKED),
            Quadruple("Level 4: 进阶技巧", "掌握高级AI编程技巧", 4, LevelStatus.LOCKED)
        )

        levels.forEach { (title, objective, levelNumber, status) ->
            val levelId = UUID.randomUUID().toString()
            levelIds.add(levelId)

            database.learningLevelDao().insertLevels(
                listOf(
                    LearningLevel(
                        id = levelId,
                        learningPathId = learningPathId,
                        levelNumber = levelNumber,
                        title = title,
                        objective = objective,
                        knowledgePoints = "[\"要点1\",\"要点2\"]",
                        commonMistakes = "[\"常见错误1\"]",
                        successCriteria = "完成所有练习任务",
                        status = status,
                        startedAt = if (status == LevelStatus.IN_PROGRESS) now else null,
                        completedAt = null,
                        createdAt = now
                    )
                )
            )

            // 为第一个阶段创建示例任务
            if (levelNumber == 1) {
                createSampleTasksForLevel(levelId, now)
            }
        }

        // 5. 创建示例知识卡片（关联第一个阶段）
        createSampleKnowledgeCards(now, levelIds.firstOrNull() ?: "")

        // 6. 创建示例成长记录
        createSampleGrowthRecords(now)
    }

    private suspend fun createSampleTasksForLevel(levelId: String, now: Long) {
        val tasks = listOf(
            Triple("了解Claude/Cursor等AI编程工具", "学习主流AI编程工具的基本用法", TaskType.LEARNING),
            Triple("学习基本的Prompt结构", "掌握提示词的基本组成和编写技巧", TaskType.LEARNING),
            Triple("实践：用AI生成一个简单函数", "通过实际操作加深理解", TaskType.PRACTICE)
        )

        tasks.forEachIndexed { index, (title, description, type) ->
            database.dailyTaskDao().insertTask(
                DailyTask(
                    id = UUID.randomUUID().toString(),
                    learningLevelId = levelId,
                    taskType = type,
                    title = title,
                    description = description,
                    estimatedTime = 30 + index * 10,
                    scheduledDate = now + index * 86400000L,
                    status = TaskStatus.PENDING,
                    userResponse = null,
                    aiFeedback = null,
                    score = null,
                    completedAt = null,
                    createdAt = now
                )
            )
        }
    }

    private suspend fun createSampleKnowledgeCards(now: Long, levelId: String) {
        val cards = listOf(
            Triple(
                "什么是AI驱动开发？",
                "AI驱动开发是指利用大语言模型辅助编程的开发模式。开发者通过自然语言描述需求，AI生成代码、解释概念、优化逻辑，大幅提升开发效率。",
                "[\"LLM\",\"代码生成\",\"效率提升\"]"
            ),
            Triple(
                "Prompt Engineering的核心原则",
                "1. 清晰明确：具体说明你想要什么\n2. 提供上下文：给AI足够的背景信息\n3. 分步引导：复杂任务拆分成小步骤\n4. 迭代优化：根据结果调整prompt",
                "[\"提示词\",\"上下文\",\"迭代优化\"]"
            ),
            Triple(
                "Claude vs GPT：如何选择？",
                "Claude擅长长文本理解、代码审查、细致解释。GPT擅长快速原型、创意生成、工具集成。建议根据任务特点选择，或组合使用。",
                "[\"Claude\",\"GPT\",\"对比\"]"
            )
        )

        cards.forEach { (topic, coreDefinition, keyConcepts) ->
            database.knowledgeCardDao().insertCard(
                KnowledgeCard(
                    id = UUID.randomUUID().toString(),
                    learningLevelId = levelId,
                    topic = topic,
                    coreDefinition = coreDefinition,
                    keyConcepts = keyConcepts,
                    useCases = "[\"编程辅助\",\"学习提升\"]",
                    commonMistakes = "[\"过度依赖AI\",\"不验证结果\"]",
                    checklist = "[\"理解原理\",\"实践验证\"]",
                    selfTestQuestions = "[\"什么是Prompt Engineering？\"]",
                    masteryScore = 0,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    private suspend fun createSampleGrowthRecords(now: Long) {
        val records = listOf(
            Triple(60, 3, 85),
            Triple(75, 4, 92),
            Triple(45, 2, 78)
        )

        records.forEachIndexed { index, (minutes, tasks, score) ->
            database.growthRecordDao().insertRecord(
                GrowthRecord(
                    id = UUID.randomUUID().toString(),
                    date = now - index * 86400000L,
                    learningMinutes = minutes,
                    tasksCompleted = tasks,
                    knowledgeCardsCreated = 1,
                    masteryScore = score,
                    aiSummary = "第${index + 1}天的学习总结",
                    createdAt = now
                )
            )
        }
    }
}

// 辅助数据类
private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
