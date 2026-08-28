package com.aigrowth.os.core.database.workbench

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aigrowth.os.core.database.workbench.dao.*
import com.aigrowth.os.core.database.workbench.entity.*

/**
 * v3.0 工作台数据层独立数据库（workbench.db）。
 * 与旧 ai_growth_os.db（AppDatabase）及记账模块 accounting.db 相互独立。
 */
@Database(
    entities = [
        PlanItem::class,
        Habit::class,
        HabitLog::class,
        ReadingItem::class,
        ExerciseItem::class,
        Essay::class,
        Clipping::class,
        StatusTrendEntry::class,
        PomodoroState::class
    ],
    version = 1,
    exportSchema = false
)
abstract class WorkbenchDatabase : RoomDatabase() {
    abstract fun planItemDao(): PlanItemDao
    abstract fun habitDao(): HabitDao
    abstract fun habitLogDao(): HabitLogDao
    abstract fun readingItemDao(): ReadingItemDao
    abstract fun exerciseItemDao(): ExerciseItemDao
    abstract fun essayDao(): EssayDao
    abstract fun clippingDao(): ClippingDao
    abstract fun statusTrendDao(): StatusTrendDao
    abstract fun pomodoroStateDao(): PomodoroStateDao
}