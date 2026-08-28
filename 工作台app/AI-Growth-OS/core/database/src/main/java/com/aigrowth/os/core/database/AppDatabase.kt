package com.aigrowth.os.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aigrowth.os.core.database.dao.*
import com.aigrowth.os.core.database.entity.*

@Database(
    entities = [
        Goal::class,
        LearningPath::class,
        LearningLevel::class,
        DailyTask::class,
        KnowledgeCard::class,
        AIMemory::class,
        GrowthRecord::class,
        Content::class,
        AIConversation::class,
        FeynmanSession::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun goalDao(): GoalDao
    abstract fun learningPathDao(): LearningPathDao
    abstract fun learningLevelDao(): LearningLevelDao
    abstract fun dailyTaskDao(): DailyTaskDao
    abstract fun knowledgeCardDao(): KnowledgeCardDao
    abstract fun aiMemoryDao(): AIMemoryDao
    abstract fun aiConversationDao(): AIConversationDao
    abstract fun feynmanSessionDao(): FeynmanSessionDao
    abstract fun growthRecordDao(): GrowthRecordDao
    abstract fun contentDao(): ContentDao
}