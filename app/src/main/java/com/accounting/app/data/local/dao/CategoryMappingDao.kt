package com.accounting.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.accounting.app.data.local.entity.CategoryMappingEntity

@Dao
interface CategoryMappingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(mapping: CategoryMappingEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(mappings: List<CategoryMappingEntity>)

    @Query("SELECT * FROM category_mappings WHERE type = :type AND enabled = 1 AND :text LIKE '%' || keyword || '%' ORDER BY CASE source WHEN 'MANUAL' THEN 0 WHEN 'AUTO' THEN 1 ELSE 2 END, LENGTH(keyword) DESC, hitCount DESC LIMIT 1")
    suspend fun match(type: String, text: String): CategoryMappingEntity?

    @Query("SELECT * FROM category_mappings WHERE source = :source ORDER BY hitCount DESC")
    suspend fun getBySource(source: String): List<CategoryMappingEntity>

    @Query("SELECT * FROM category_mappings ORDER BY CASE source WHEN 'MANUAL' THEN 0 WHEN 'AUTO' THEN 1 ELSE 2 END, hitCount DESC")
    suspend fun getAll(): List<CategoryMappingEntity>

    @Query("DELETE FROM category_mappings WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE category_mappings SET enabled = :enabled WHERE id = :id")
    suspend fun updateEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE category_mappings SET source = 'MANUAL', hitCount = 1, updatedAt = :now WHERE id = :id")
    suspend fun promoteToManual(id: Long, now: Long)

    @Query("DELETE FROM category_mappings WHERE source = 'AUTO' AND (lastHitAt IS NULL OR lastHitAt < :beforeTime) AND hitCount < 3")
    suspend fun cleanStaleAutoMappings(beforeTime: Long): Int

    @Query("SELECT * FROM category_mappings WHERE keyword = :keyword AND type = :type LIMIT 1")
    suspend fun findByKeywordAndType(keyword: String, type: String): CategoryMappingEntity?

    @Query("UPDATE category_mappings SET hitCount = hitCount + 1, lastHitAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun incrementHitCount(id: Long, now: Long)
}
