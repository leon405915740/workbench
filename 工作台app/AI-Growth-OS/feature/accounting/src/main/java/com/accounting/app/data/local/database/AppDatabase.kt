package com.accounting.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.accounting.app.data.local.dao.CategoryMemoryDao
import com.accounting.app.data.local.dao.CategoryMappingDao
import com.accounting.app.data.local.dao.CategoryDao
import com.accounting.app.data.local.dao.ExpenseDao
import com.accounting.app.data.local.dao.IncomeDao
import com.accounting.app.data.local.entity.CategoryMemoryEntity
import com.accounting.app.data.local.entity.CategoryMappingEntity
import com.accounting.app.data.local.entity.CategoryEntity
import com.accounting.app.data.local.entity.ExpenseEntity
import com.accounting.app.data.local.entity.IncomeEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [ExpenseEntity::class, IncomeEntity::class, CategoryMemoryEntity::class, CategoryEntity::class, CategoryMappingEntity::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun incomeDao(): IncomeDao
    abstract fun categoryMemoryDao(): CategoryMemoryDao
    abstract fun categoryDao(): CategoryDao
    abstract fun categoryMappingDao(): CategoryMappingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE category_memory ADD COLUMN source TEXT NOT NULL DEFAULT 'user'")
                database.execSQL("ALTER TABLE category_memory ADD COLUMN confidence INTEGER NOT NULL DEFAULT 100")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `name` TEXT NOT NULL, `parentId` INTEGER, `sortOrder` INTEGER NOT NULL, `isSystem` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_name_type_parentId` ON `categories` (`name`, `type`, `parentId`)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `category_mappings` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `keyword` TEXT NOT NULL, `type` TEXT NOT NULL, `categoryId` INTEGER NOT NULL, `subcategoryId` INTEGER, `source` TEXT NOT NULL, `enabled` INTEGER NOT NULL, `hitCount` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `lastHitAt` INTEGER)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_category_mappings_keyword_type` ON `category_mappings` (`keyword`, `type`)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 删除所有二级分类（parentId 不为 NULL 的记录）
                database.execSQL("DELETE FROM categories WHERE parentId IS NOT NULL")
                // 删除旧唯一索引（包含 parentId）
                database.execSQL("DROP INDEX IF EXISTS index_categories_name_type_parentId")
                // 重建不含 parentId 的唯一索引
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_categories_name_type ON categories (name, type)")
                // 清空 category_mappings 中的 subcategoryId
                database.execSQL("UPDATE category_mappings SET subcategoryId = NULL WHERE subcategoryId IS NOT NULL")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. expense 表：旧分类名 → 新分类名（按 expense → income → category_memory 顺序）
                database.execSQL("UPDATE expense SET category = '餐饮美食' WHERE category = '餐饮'")
                database.execSQL("UPDATE expense SET category = '餐饮美食' WHERE category = '零食饮料'")
                database.execSQL("UPDATE expense SET category = '交通出行' WHERE category = '交通'")
                database.execSQL("UPDATE expense SET category = '日用家居' WHERE category = '大件购物'")
                database.execSQL("UPDATE expense SET category = '日用家居' WHERE category = '居家'")
                database.execSQL("UPDATE expense SET category = '日用家居' WHERE category = '日用'")
                database.execSQL("UPDATE expense SET category = '通讯资费' WHERE category = '通讯'")
                database.execSQL("UPDATE expense SET category = '娱乐休闲' WHERE category = '娱乐'")
                database.execSQL("UPDATE expense SET category = '医疗健康' WHERE category = '医疗'")
                database.execSQL("UPDATE expense SET category = '教育学习' WHERE category = '教育'")
                database.execSQL("UPDATE expense SET category = '人情往来' WHERE category = '人情'")
                database.execSQL("UPDATE expense SET category = '宠物生活' WHERE category = '宠物'")
                database.execSQL("UPDATE expense SET category = '数码电器' WHERE category = '数码'")
                database.execSQL("UPDATE expense SET category = '其他支出' WHERE category = '其他'")
                // 服饰美容 不变，无需 UPDATE

                // 2. income 表：旧分类名 → 新分类名
                database.execSQL("UPDATE income SET category = '工资薪水' WHERE category = '工资'")
                database.execSQL("UPDATE income SET category = '工资薪水' WHERE category = '奖金'")
                database.execSQL("UPDATE income SET category = '兼职副业' WHERE category = '兼职'")
                database.execSQL("UPDATE income SET category = '其他收入' WHERE category = '退款'")
                database.execSQL("UPDATE income SET category = '其他收入' WHERE category = '报销'")
                database.execSQL("UPDATE income SET category = '人情礼金' WHERE category = '红包'")
                // 理财收益、其他收入 不变，无需 UPDATE

                // 3. category_memory 表：旧分类名 → 新分类名（保留用户学习数据，仅 UPDATE 分类名）
                database.execSQL("UPDATE category_memory SET category = '餐饮美食' WHERE category = '餐饮'")
                database.execSQL("UPDATE category_memory SET category = '餐饮美食' WHERE category = '零食饮料'")
                database.execSQL("UPDATE category_memory SET category = '交通出行' WHERE category = '交通'")
                database.execSQL("UPDATE category_memory SET category = '日用家居' WHERE category = '大件购物'")
                database.execSQL("UPDATE category_memory SET category = '日用家居' WHERE category = '居家'")
                database.execSQL("UPDATE category_memory SET category = '日用家居' WHERE category = '日用'")
                database.execSQL("UPDATE category_memory SET category = '通讯资费' WHERE category = '通讯'")
                database.execSQL("UPDATE category_memory SET category = '娱乐休闲' WHERE category = '娱乐'")
                database.execSQL("UPDATE category_memory SET category = '医疗健康' WHERE category = '医疗'")
                database.execSQL("UPDATE category_memory SET category = '教育学习' WHERE category = '教育'")
                database.execSQL("UPDATE category_memory SET category = '人情往来' WHERE category = '人情'")
                database.execSQL("UPDATE category_memory SET category = '宠物生活' WHERE category = '宠物'")
                database.execSQL("UPDATE category_memory SET category = '数码电器' WHERE category = '数码'")
                database.execSQL("UPDATE category_memory SET category = '其他支出' WHERE category = '其他'")
                database.execSQL("UPDATE category_memory SET category = '工资薪水' WHERE category = '工资'")
                database.execSQL("UPDATE category_memory SET category = '工资薪水' WHERE category = '奖金'")
                database.execSQL("UPDATE category_memory SET category = '兼职副业' WHERE category = '兼职'")
                database.execSQL("UPDATE category_memory SET category = '其他收入' WHERE category = '退款'")
                database.execSQL("UPDATE category_memory SET category = '其他收入' WHERE category = '报销'")
                database.execSQL("UPDATE category_memory SET category = '人情礼金' WHERE category = '红包'")

                // 4. 清空 categories 和 category_mappings 表（生产路径未播种未使用）
                database.execSQL("DELETE FROM categories")
                database.execSQL("DELETE FROM category_mappings")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. expense 表：旧分类名 → 新分类名
                database.execSQL("UPDATE expense SET category = '餐饮美食' WHERE category = '餐饮'")
                database.execSQL("UPDATE expense SET category = '餐饮美食' WHERE category = '零食饮料'")
                database.execSQL("UPDATE expense SET category = '交通出行' WHERE category = '交通'")
                database.execSQL("UPDATE expense SET category = '日用家居' WHERE category = '大件购物'")
                database.execSQL("UPDATE expense SET category = '日用家居' WHERE category = '居家'")
                database.execSQL("UPDATE expense SET category = '日用家居' WHERE category = '日用'")
                database.execSQL("UPDATE expense SET category = '通讯资费' WHERE category = '通讯'")
                database.execSQL("UPDATE expense SET category = '娱乐休闲' WHERE category = '娱乐'")
                database.execSQL("UPDATE expense SET category = '医疗健康' WHERE category = '医疗'")
                database.execSQL("UPDATE expense SET category = '教育学习' WHERE category = '教育'")
                database.execSQL("UPDATE expense SET category = '人情往来' WHERE category = '人情'")
                database.execSQL("UPDATE expense SET category = '宠物生活' WHERE category = '宠物'")
                database.execSQL("UPDATE expense SET category = '数码电器' WHERE category = '数码'")
                database.execSQL("UPDATE expense SET category = '其他支出' WHERE category = '其他'")

                // 2. income 表：旧分类名 → 新分类名
                database.execSQL("UPDATE income SET category = '工资薪水' WHERE category = '工资'")
                database.execSQL("UPDATE income SET category = '工资薪水' WHERE category = '奖金'")
                database.execSQL("UPDATE income SET category = '兼职副业' WHERE category = '兼职'")
                database.execSQL("UPDATE income SET category = '其他收入' WHERE category = '退款'")
                database.execSQL("UPDATE income SET category = '其他收入' WHERE category = '报销'")
                database.execSQL("UPDATE income SET category = '人情礼金' WHERE category = '红包'")

                // 3. category_memory 表：旧分类名 → 新分类名
                database.execSQL("UPDATE category_memory SET category = '餐饮美食' WHERE category = '餐饮'")
                database.execSQL("UPDATE category_memory SET category = '餐饮美食' WHERE category = '零食饮料'")
                database.execSQL("UPDATE category_memory SET category = '交通出行' WHERE category = '交通'")
                database.execSQL("UPDATE category_memory SET category = '日用家居' WHERE category = '大件购物'")
                database.execSQL("UPDATE category_memory SET category = '日用家居' WHERE category = '居家'")
                database.execSQL("UPDATE category_memory SET category = '日用家居' WHERE category = '日用'")
                database.execSQL("UPDATE category_memory SET category = '通讯资费' WHERE category = '通讯'")
                database.execSQL("UPDATE category_memory SET category = '娱乐休闲' WHERE category = '娱乐'")
                database.execSQL("UPDATE category_memory SET category = '医疗健康' WHERE category = '医疗'")
                database.execSQL("UPDATE category_memory SET category = '教育学习' WHERE category = '教育'")
                database.execSQL("UPDATE category_memory SET category = '人情往来' WHERE category = '人情'")
                database.execSQL("UPDATE category_memory SET category = '宠物生活' WHERE category = '宠物'")
                database.execSQL("UPDATE category_memory SET category = '数码电器' WHERE category = '数码'")
                database.execSQL("UPDATE category_memory SET category = '其他支出' WHERE category = '其他'")
                database.execSQL("UPDATE category_memory SET category = '工资薪水' WHERE category = '工资'")
                database.execSQL("UPDATE category_memory SET category = '工资薪水' WHERE category = '奖金'")
                database.execSQL("UPDATE category_memory SET category = '兼职副业' WHERE category = '兼职'")
                database.execSQL("UPDATE category_memory SET category = '其他收入' WHERE category = '退款'")
                database.execSQL("UPDATE category_memory SET category = '其他收入' WHERE category = '报销'")
                database.execSQL("UPDATE category_memory SET category = '人情礼金' WHERE category = '红包'")

                // 4. 重建 categories 表
                database.execSQL("DELETE FROM categories")
                val now = "strftime('%s','now')*1000"
                val expenseCategories = listOf(
                    "餐饮美食", "交通出行", "日用家居", "娱乐休闲",
                    "服饰美容", "住房房租", "通讯资费", "医疗健康",
                    "教育学习", "人情往来", "数码电器", "爱车养车",
                    "宠物生活", "旅行度假", "育儿长辈", "其他支出"
                )
                expenseCategories.forEachIndexed { index, name ->
                    database.execSQL(
                        "INSERT INTO categories (type, name, parentId, sortOrder, isSystem, createdAt, updatedAt) VALUES ('expense', '$name', NULL, $index, 1, $now, $now)"
                    )
                }
                val incomeCategories = listOf(
                    "工资薪水", "兼职副业", "理财收益", "人情礼金", "其他收入"
                )
                incomeCategories.forEachIndexed { index, name ->
                    database.execSQL(
                        "INSERT INTO categories (type, name, parentId, sortOrder, isSystem, createdAt, updatedAt) VALUES ('income', '$name', NULL, $index, 1, $now, $now)"
                    )
                }

                // 5. 清空 category_mappings 表（由应用层重建）
                database.execSQL("DELETE FROM category_mappings")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "accounting.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.categoryMemoryDao()?.insertAll(SeedData.seedMemories)
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
