package com.aigrowth.os

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aigrowth.os.core.database.workbench.WorkbenchDatabase
import com.aigrowth.os.core.database.workbench.entity.ExerciseCategoryEnum
import com.aigrowth.os.core.database.workbench.entity.HabitLog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * 工作台数据库 v1 → v2 迁移自动化测试。
 *
 * v2 相对于 v1 的变更：habits 新增 category / pinnedAt，habit_logs 新增
 * durationMinutes / note / category（均为可空列）。
 *
 * 覆盖场景：
 * 1. 有数据升级——habits/habit_logs 既有记录完整保留，新列默认 null
 * 2. HabitLogDao.addDuration 原子累加——5+3=8，无记录时返回 0 且不新增行
 * 3. 带类别 / 备注 / 时长的打卡日志读写往返
 */
class WorkbenchMigrationTest {

    private lateinit var context: Context

    // 每个测试使用独立数据库名，避免相互干扰，并在前后删除
    private val dbName = "workbench-v1-to-v2-test-${System.nanoTime()}.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migrate1to2_preservesExistingDataAndAddsNullColumns() = runBlocking {
        createV1Database(dbName)
        insertV1SampleData(dbName)

        val db = openMigratedDb()
        try {
            val habitDao = db.habitDao()
            val habitLogDao = db.habitLogDao()

            // 旧数据完好：习惯数量保留
            val habits = habitDao.getAll().first()
            assertEquals(2, habits.size)

            val h1 = habitDao.getById("habit-1")
            assertEquals("早起", h1?.title)
            assertEquals(true, h1?.active)
            assertEquals(false, h1?.pinned)
            assertNull(h1?.imageUri)
            // 新增列默认 null
            assertNull(h1?.category)
            assertNull(h1?.pinnedAt)

            val h2 = habitDao.getById("habit-2")
            assertEquals("跑步", h2?.title)
            assertEquals(true, h2?.pinned)
            assertEquals("image/run.png", h2?.imageUri)
            assertNull(h2?.category)
            assertNull(h2?.pinnedAt)

            // 打卡记录旧数据完好：habit-1 两天、habit-2 一天
            assertEquals(2, habitLogDao.getLogsForHabit("habit-1").first().size)
            assertEquals(1, habitLogDao.getLogsForHabit("habit-2").first().size)

            val log1 = habitLogDao.getByHabitAndDate("habit-1", "2026-06-01")
            assertEquals(100000L, log1?.checkedAt)
            assertNull(log1?.durationMinutes)
            assertNull(log1?.note)
            assertNull(log1?.category)

            val log2 = habitLogDao.getByHabitAndDate("habit-2", "2026-06-01")
            assertEquals(300000L, log2?.checkedAt)
            assertNull(log2?.durationMinutes)
            assertNull(log2?.note)
            assertNull(log2?.category)
        } finally {
            db.close()
        }
    }

    @Test
    fun habitLogDao_addDuration_accumulatesAtomicallyAndNoRowWhenMissing() = runBlocking {
        createV1Database(dbName)
        insertV1SampleData(dbName)

        val db = openMigratedDb()
        try {
            val habitLogDao = db.habitLogDao()

            // 预置一条时长 5 的打卡记录（迁移后新增列可写）
            habitLogDao.insert(
                HabitLog(habitId = "habit-1", date = "2026-06-05", checkedAt = 500000L, durationMinutes = 5)
            )

            // 原子累加：5 + 3 = 8
            assertEquals(1, habitLogDao.addDuration("habit-1", "2026-06-05", 3))
            assertEquals(8, habitLogDao.getByHabitAndDate("habit-1", "2026-06-05")?.durationMinutes)

            // 无记录时不新增行，返回受影响行数 0
            assertEquals(0, habitLogDao.addDuration("habit-1", "2099-01-01", 10))
            assertNull(habitLogDao.getByHabitAndDate("habit-1", "2099-01-01"))
        } finally {
            db.close()
        }
    }

    @Test
    fun habitLogDao_categoryNoteDuration_writeAndReadBack() = runBlocking {
        createV1Database(dbName)
        insertV1SampleData(dbName)

        val db = openMigratedDb()
        try {
            val habitLogDao = db.habitLogDao()

            habitLogDao.insert(
                HabitLog(
                    habitId = "habit-1",
                    date = "2026-06-06",
                    checkedAt = 600000L,
                    durationMinutes = 45,
                    note = "晨跑 30 分钟",
                    category = ExerciseCategoryEnum.CARDIO
                )
            )

            val log = habitLogDao.getByHabitAndDate("habit-1", "2026-06-06")
            assertEquals(45, log?.durationMinutes)
            assertEquals("晨跑 30 分钟", log?.note)
            assertEquals(ExerciseCategoryEnum.CARDIO, log?.category)
        } finally {
            db.close()
        }
    }

    @Test
    fun migrate2to3_addsCompletedAtAndReadingLogs() = runBlocking {
        createV1Database(dbName)
        insertV1SampleData(dbName)

        val db = openMigratedDb()
        try {
            // plan_items.completedAt 可写读（v3 新增列，默认 null → 写入时间戳）
            val planItemDao = db.planItemDao()
            planItemDao.insert(
                com.aigrowth.os.core.database.workbench.entity.PlanItem(
                    id = "plan-1",
                    title = "写周报",
                    priority = "P1",
                    note = "",
                    done = true,
                    pinned = false,
                    planDate = "2026-06-01",
                    imageUri = null,
                    createdAt = 1000L,
                    updatedAt = 1000L,
                    completedAt = 500000L
                )
            )
            val savedPlan = planItemDao.getById("plan-1")
            assertEquals(500000L, savedPlan?.completedAt)

            // reading_logs 表可用（v3 CREATE TABLE）
            val readingLogDao = db.readingLogDao()
            readingLogDao.insert(
                com.aigrowth.os.core.database.workbench.entity.ReadingLog(
                    id = "log-1",
                    readingItemId = "book-1",
                    date = "2026-06-01",
                    amount = 20f,
                    createdAt = 700000L
                )
            )
            val logs = readingLogDao.getAllByItem("book-1").first()
            assertEquals(1, logs.size)
            assertEquals(20f, logs[0].amount)
        } finally {
            db.close()
        }
    }

    // region Helpers

    private fun openMigratedDb(): WorkbenchDatabase {
        return Room.databaseBuilder(context, WorkbenchDatabase::class.java, dbName)
            .addMigrations(WorkbenchDatabase.MIGRATION_1_2, WorkbenchDatabase.MIGRATION_2_3)
            .build()
    }

    /**
     * 用 Room 生成的 v1 实体 schema 直接构造一个 v1 数据库文件。
     * 除 habits / habit_logs 缺少 v2 新增列外，其余 7 张表与 v2 完全一致，保证迁移后 Room 校验通过。
     */
    private fun createV1Database(name: String) {
        val db = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null)
        try {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `plan_items` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                    "`priority` TEXT NOT NULL, `note` TEXT NOT NULL, `done` INTEGER NOT NULL, `pinned` INTEGER NOT NULL, " +
                    "`planDate` TEXT NOT NULL, `imageUri` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`id`))"
            )

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `habits` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                    "`active` INTEGER NOT NULL, `pinned` INTEGER NOT NULL, `imageUri` TEXT, " +
                    "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
            )

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `habit_logs` (`habitId` TEXT NOT NULL, `date` TEXT NOT NULL, " +
                    "`checkedAt` INTEGER NOT NULL, PRIMARY KEY(`habitId`, `date`), " +
                    "FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_logs_habitId` ON `habit_logs` (`habitId`)")

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `reading_items` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                    "`current` REAL NOT NULL, `target` REAL NOT NULL, `unit` TEXT NOT NULL, `date` TEXT NOT NULL, " +
                    "`note` TEXT NOT NULL, `pinned` INTEGER NOT NULL, `imageUri` TEXT, " +
                    "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
            )

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `exercise_items` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                    "`current` REAL NOT NULL, `target` REAL NOT NULL, `unit` TEXT NOT NULL, `date` TEXT NOT NULL, " +
                    "`note` TEXT NOT NULL, `pinned` INTEGER NOT NULL, `imageUri` TEXT, " +
                    "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
            )

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `essays` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, " +
                    "`mood` TEXT, `type` TEXT NOT NULL, `tags` TEXT NOT NULL, `layout` TEXT NOT NULL, `date` TEXT NOT NULL, " +
                    "`pinned` INTEGER NOT NULL, `imageUri` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`id`))"
            )

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `clippings` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, " +
                    "`status` TEXT, `source` TEXT, `tags` TEXT NOT NULL, `layout` TEXT NOT NULL, `date` TEXT NOT NULL, " +
                    "`pinned` INTEGER NOT NULL, `imageUri` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`id`))"
            )

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `status_trend_entries` (`id` TEXT NOT NULL, `date` TEXT NOT NULL, " +
                    "`score` INTEGER NOT NULL, `note` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
            )

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `pomodoro_state` (`id` TEXT NOT NULL, `running` INTEGER NOT NULL, " +
                    "`remainSeconds` INTEGER NOT NULL, `totalSeconds` INTEGER NOT NULL, `startedAt` INTEGER, " +
                    "`focusCount` INTEGER NOT NULL, `totalFocusMinutes` INTEGER NOT NULL, PRIMARY KEY(`id`))"
            )

            db.version = 1
        } finally {
            db.close()
        }
    }

    private fun insertV1SampleData(name: String) {
        val db = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null)
        try {
            db.execSQL(
                "INSERT INTO habits (id, title, active, pinned, imageUri, createdAt, updatedAt) " +
                    "VALUES ('habit-1', '早起', 1, 0, NULL, 1000, 1000)"
            )
            db.execSQL(
                "INSERT INTO habits (id, title, active, pinned, imageUri, createdAt, updatedAt) " +
                    "VALUES ('habit-2', '跑步', 1, 1, 'image/run.png', 2000, 2000)"
            )

            db.execSQL(
                "INSERT INTO habit_logs (habitId, date, checkedAt) VALUES ('habit-1', '2026-06-01', 100000)"
            )
            db.execSQL(
                "INSERT INTO habit_logs (habitId, date, checkedAt) VALUES ('habit-1', '2026-06-02', 200000)"
            )
            db.execSQL(
                "INSERT INTO habit_logs (habitId, date, checkedAt) VALUES ('habit-2', '2026-06-01', 300000)"
            )
        } finally {
            db.close()
        }
    }

    // endregion
}