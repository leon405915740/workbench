package com.accounting.app.data.local.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * 记账数据库 v6 → v7 迁移自动化测试。
 *
 * v7 相对于 v6 唯一变更：为 expense / income 表新增允许为空的 attachmentPath 列，
 * 用于存储附加凭证图片的应用私有路径。
 *
 * 覆盖场景：
 * 1. 有数据升级（含无附件账单）——既有记录完整保留，新列默认 null
 * 2. 附件增 / 删 / 换——updateAttachment 三个方向行为正确，不存在的 id 返回 0
 * 3. 统计一致——迁移后金额汇总与分类统计不受影响
 * 4. 失败回滚——迁移函数抛异常时不留下半迁移状态，数据无损、可再次正确升级
 */
class AccountingMigrationTest {

    private lateinit var context: Context

    // 每个测试使用独立数据库名，避免相互干扰，并在前后删除
    private val dbName = "v6-to-v7-test-${System.nanoTime()}.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    // region 迁移后应保持一致的样本统计值
    // expense: 1000(餐饮美食) + 2500(交通出行) + 5000(餐饮美食) + 1200(其他支出) = 9700
    // income:  800000(工资薪水) + 200000(兼职副业) = 1000000
    // endregion

    @Test
    fun migrate6to7_preservesExistingDataAndNullAttachmentForNoAttachmentBills() = runBlocking {
        createV6Database(dbName)
        insertV6SampleData(dbName)

        val db = openMigratedDb(AppDatabase.MIGRATION_6_7)
        try {
            val expenseDao = db.expenseDao()
            val incomeDao = db.incomeDao()

            // 行数完整保留
            assertEquals(4, expenseDao.count())
            assertEquals(2, incomeDao.count())

            // 原字段完整保留
            val e1 = expenseDao.getById(1)
            assertNotNull(e1)
            assertEquals(1000L, e1!!.amount)
            assertEquals("餐饮美食", e1.category)
            assertEquals("奶茶店", e1.merchant)
            assertNull(e1.subcategory)
            assertEquals(0.9f, e1.confidence)

            val e2 = expenseDao.getById(2)
            assertNotNull(e2)
            assertEquals("交通出行", e2!!.category)
            assertEquals("打车", e2.note)

            // 无附件账单：升级后 attachmentPath 应为 null
            assertNull(expenseDao.getById(1)!!.attachmentPath)
            assertNull(expenseDao.getById(2)!!.attachmentPath)
            assertNull(expenseDao.getById(3)!!.attachmentPath)
            assertNull(expenseDao.getById(4)!!.attachmentPath)

            val i1 = incomeDao.getById(1)
            assertNotNull(i1)
            assertEquals(800000L, i1!!.amount)
            assertEquals("工资薪水", i1.category)
            assertNull(i1.attachmentPath)

            val i2 = incomeDao.getById(2)
            assertNotNull(i2)
            assertEquals("兼职副业", i2!!.category)
            assertNull(i2.attachmentPath)
        } finally {
            db.close()
        }
    }

    @Test
    fun migrate6to7_attachmentAddReplaceDelete() = runBlocking {
        createV6Database(dbName)
        insertV6SampleData(dbName)

        val db = openMigratedDb(AppDatabase.MIGRATION_6_7)
        try {
            val expenseDao = db.expenseDao()
            val incomeDao = db.incomeDao()

            // 初始无附件
            assertNull(expenseDao.getById(1)!!.attachmentPath)

            // 添加附件
            assertEquals(1, expenseDao.updateAttachment(1, "/data/expense_1.png"))
            assertEquals("/data/expense_1.png", expenseDao.getById(1)!!.attachmentPath)

            // 更换附件
            assertEquals(1, expenseDao.updateAttachment(1, "/data/expense_1_v2.png"))
            assertEquals("/data/expense_1_v2.png", expenseDao.getById(1)!!.attachmentPath)

            // 删除附件（置空）
            assertEquals(1, expenseDao.updateAttachment(1, null))
            assertNull(expenseDao.getById(1)!!.attachmentPath)

            // income 附件同样支持增 / 删
            assertEquals(1, incomeDao.updateAttachment(1, "/data/income_1.png"))
            assertEquals("/data/income_1.png", incomeDao.getById(1)!!.attachmentPath)
            assertEquals(1, incomeDao.updateAttachment(1, null))
            assertNull(incomeDao.getById(1)!!.attachmentPath)

            // 更新不存在的 id 应返回 0，且不影响其他记录
            assertEquals(0, expenseDao.updateAttachment(999, "/data/x.png"))
            assertNull(expenseDao.getById(1)!!.attachmentPath)
        } finally {
            db.close()
        }
    }

    @Test
    fun migrate6to7_statisticsConsistent() = runBlocking {
        createV6Database(dbName)
        insertV6SampleData(dbName)

        val db = openMigratedDb(AppDatabase.MIGRATION_6_7)
        try {
            val expenseDao = db.expenseDao()
            val incomeDao = db.incomeDao()

            // 全量时间范围汇总保持一致
            val expenseSum = expenseDao.getSumByTimeRange(0, 4000).first()
            assertNotNull(expenseSum)
            assertEquals(9700L, expenseSum)

            val incomeSum = incomeDao.getSumByTimeRange(0, 4000).first()
            assertNotNull(incomeSum)
            assertEquals(1000000L, incomeSum)

            // 分类统计保持一致（3 个支出分类）
            val expenseStats = expenseDao.getCategoryStats(0, 4000).first()
            assertEquals(3, expenseStats.size)

            // 收入分类统计（2 个收入分类）
            val incomeStats = incomeDao.getCategoryStats(0, 4000).first()
            assertEquals(2, incomeStats.size)
        } finally {
            db.close()
        }
    }

    @Test
    fun migrate6to7_failureRollsBackAndLeavesDataIntact() = runBlocking {
        createV6Database(dbName)
        insertV6SampleData(dbName)

        // 一个执行到一半便抛异常的迁移：先加列再抛错，模拟迁移中途失败
        val brokenMigration = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE expense ADD COLUMN attachmentPath TEXT")
                throw IllegalStateException("boom")
            }
        }

        val brokenDb = openMigratedDb(brokenMigration)
        try {
            // 触发数据库打开，执行迁移
            brokenDb.expenseDao().count()
            fail("迁移抛异常时，打开数据库应当抛出异常")
        } catch (_: Exception) {
            // 预期抛出异常：迁移失败
        }

        // 失败回滚：原数据无损，且可再次以正确迁移升级到 v7
        val db = openMigratedDb(AppDatabase.MIGRATION_6_7)
        try {
            val expenseDao = db.expenseDao()
            assertEquals(4, expenseDao.count())

            // 半迁移未生效：attachmentPath 仍为 null（列被回滚后由正确迁移重新添加）
            assertNull(expenseDao.getById(1)!!.attachmentPath)
            assertNull(expenseDao.getById(2)!!.attachmentPath)

            // 原数据保留
            assertEquals(2500L, expenseDao.getById(2)!!.amount)
            assertEquals("奶茶店", expenseDao.getById(1)!!.merchant)
        } finally {
            db.close()
        }
    }

    // region Helpers

    private fun openMigratedDb(migration: Migration): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(migration)
            .build()
    }

    /**
     * 用 Room 生成的 v6 实体 schema 直接构造一个 v6 数据库文件（不含 attachmentPath 列）。
     * 构造遵循 Room 的 DDL 约定，保证迁移后 Room 校验通过。
     */
    private fun createV6Database(name: String) {
        val db = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null)
        try {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `expense` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`amount` INTEGER NOT NULL, `category` TEXT NOT NULL, `subcategory` TEXT, " +
                    "`merchant` TEXT, `time` INTEGER NOT NULL, `note` TEXT, `confidence` REAL NOT NULL, " +
                    "`rawInput` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_expense_time` ON `expense` (`time`)")

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `income` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`amount` INTEGER NOT NULL, `category` TEXT NOT NULL, `subcategory` TEXT, " +
                    "`merchant` TEXT, `time` INTEGER NOT NULL, `note` TEXT, `confidence` REAL NOT NULL, " +
                    "`rawInput` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_income_time` ON `income` (`time`)")

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `category_memory` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`triggerWord` TEXT NOT NULL, `type` TEXT NOT NULL, `category` TEXT NOT NULL, " +
                    "`subcategory` TEXT, `hitCount` INTEGER NOT NULL, `source` TEXT NOT NULL, " +
                    "`confidence` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)"
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_category_memory_triggerWord_type` ON `category_memory` (`triggerWord`, `type`)")

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`type` TEXT NOT NULL, `name` TEXT NOT NULL, `parentId` INTEGER, `sortOrder` INTEGER NOT NULL, " +
                    "`isSystem` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)"
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_name_type_parentId` ON `categories` (`name`, `type`, `parentId`)")

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `category_mappings` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`keyword` TEXT NOT NULL, `type` TEXT NOT NULL, `categoryId` INTEGER NOT NULL, `subcategoryId` INTEGER, " +
                    "`source` TEXT NOT NULL, `enabled` INTEGER NOT NULL, `hitCount` INTEGER NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `lastHitAt` INTEGER)"
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_category_mappings_keyword_type` ON `category_mappings` (`keyword`, `type`)")

            db.version = 6
        } finally {
            db.close()
        }
    }

    private fun insertV6SampleData(name: String) {
        val db = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null)
        try {
            // 支出：id0-3
            db.execSQL(
                "INSERT INTO expense (amount, category, subcategory, merchant, time, note, confidence, rawInput, createdAt) " +
                    "VALUES (1000, '餐饮美食', NULL, '奶茶店', 1000, NULL, 0.9, '奶茶', 1000)"
            )
            db.execSQL(
                "INSERT INTO expense (amount, category, subcategory, merchant, time, note, confidence, rawInput, createdAt) " +
                    "VALUES (2500, '交通出行', NULL, '滴滴', 2000, '打车', 0.8, '滴滴', 2000)"
            )
            db.execSQL(
                "INSERT INTO expense (amount, category, subcategory, merchant, time, note, confidence, rawInput, createdAt) " +
                    "VALUES (5000, '餐饮美食', NULL, NULL, 3000, NULL, 0.7, '火锅', 3000)"
            )
            db.execSQL(
                "INSERT INTO expense (amount, category, subcategory, merchant, time, note, confidence, rawInput, createdAt) " +
                    "VALUES (1200, '其他支出', NULL, '超市', 1500, '无附件', 0.6, '超市', 1500)"
            )

            // 收入：id0-1
            db.execSQL(
                "INSERT INTO income (amount, category, subcategory, merchant, time, note, confidence, rawInput, createdAt) " +
                    "VALUES (800000, '工资薪水', NULL, '公司', 1000, NULL, 0.95, '工资', 1000)"
            )
            db.execSQL(
                "INSERT INTO income (amount, category, subcategory, merchant, time, note, confidence, rawInput, createdAt) " +
                    "VALUES (200000, '兼职副业', NULL, NULL, 2000, NULL, 0.8, '兼职', 2000)"
            )
        } finally {
            db.close()
        }
    }

    // endregion
}
