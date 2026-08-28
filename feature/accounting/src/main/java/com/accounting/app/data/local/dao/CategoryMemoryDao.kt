package com.accounting.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.accounting.app.data.local.entity.CategoryMemoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * 分类记忆数据访问对象。
 *
 * 提供两种写入策略：
 * - upsert（REPLACE）：用户修改分类时覆盖旧记忆
 * - insertAll（IGNORE）：种子数据初始化时只新增不覆盖
 */
@Dao
interface CategoryMemoryDao {
    // 用户修改时用：冲突策略 REPLACE，覆盖旧记忆
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(memory: CategoryMemoryEntity): Long

    // 种子数据初始化用：冲突策略 IGNORE，只新增不覆盖
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(memories: List<CategoryMemoryEntity>)

    @Query("SELECT * FROM category_memory WHERE triggerWord = :triggerWord AND type = :type LIMIT 1")
    suspend fun matchByTriggerWord(type: String, triggerWord: String): CategoryMemoryEntity?

    @Query("SELECT * FROM category_memory WHERE type = :type")
    fun getAllByType(type: String): Flow<List<CategoryMemoryEntity>>

    @Query("SELECT * FROM category_memory WHERE type = :type ORDER BY hitCount DESC LIMIT :limit")
    suspend fun getTopByType(type: String, limit: Int): List<CategoryMemoryEntity>

    @Query("DELETE FROM category_memory WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM category_memory WHERE type = :type")
    suspend fun deleteAllByType(type: String)

    @Query("DELETE FROM category_memory")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM category_memory")
    suspend fun count(): Int

    @Query("UPDATE category_memory SET hitCount = hitCount + 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun incrementHitCount(id: Long, updatedAt: Long)

    @Query("SELECT * FROM category_memory WHERE type = :type ORDER BY hitCount DESC LIMIT 20")
    suspend fun getTop20ByType(type: String): List<CategoryMemoryEntity>

    @Query("SELECT * FROM category_memory WHERE type = :type AND source = :source")
    suspend fun getByTypeAndSource(type: String, source: String): List<CategoryMemoryEntity>

    @Query("SELECT * FROM category_memory WHERE type = :type ORDER BY hitCount DESC")
    suspend fun getAllByTypeOnce(type: String): List<CategoryMemoryEntity>

    @Query("DELETE FROM category_memory WHERE source = :source")
    suspend fun deleteBySource(source: String)
}
