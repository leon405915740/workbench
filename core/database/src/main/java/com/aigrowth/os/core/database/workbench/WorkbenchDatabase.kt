package com.aigrowth.os.core.database.workbench

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
        ReadingLog::class,
        ExerciseItem::class,
        Essay::class,
        Clipping::class,
        StatusTrendEntry::class,
        PomodoroState::class
    ],
    version = 3,
    exportSchema = false
)
abstract class WorkbenchDatabase : RoomDatabase() {
    abstract fun planItemDao(): PlanItemDao
    abstract fun habitDao(): HabitDao
    abstract fun habitLogDao(): HabitLogDao
    abstract fun readingItemDao(): ReadingItemDao
    abstract fun readingLogDao(): ReadingLogDao
    abstract fun exerciseItemDao(): ExerciseItemDao
    abstract fun essayDao(): EssayDao
    abstract fun clippingDao(): ClippingDao
    abstract fun statusTrendDao(): StatusTrendDao
    abstract fun pomodoroStateDao(): PomodoroStateDao

    companion object {
        /** v1→v2：仅 ALTER TABLE 新增列，不迁移 ExerciseItem 历史数据、不新建索引。 */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN category TEXT")
                db.execSQL("ALTER TABLE habits ADD COLUMN pinnedAt INTEGER")
                db.execSQL("ALTER TABLE habit_logs ADD COLUMN durationMinutes INTEGER")
                db.execSQL("ALTER TABLE habit_logs ADD COLUMN note TEXT")
                db.execSQL("ALTER TABLE habit_logs ADD COLUMN category TEXT")
            }
        }

        /** v2→v3：plan_items 新增 completedAt 列，新建 reading_logs 表。 */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE plan_items ADD COLUMN completedAt INTEGER")
                db.execSQL("CREATE TABLE IF NOT EXISTS reading_logs (id TEXT NOT NULL PRIMARY KEY, readingItemId TEXT NOT NULL, date TEXT NOT NULL, amount REAL NOT NULL, createdAt INTEGER NOT NULL, note TEXT)")
            }
        }
    }
}