package com.aigrowth.os.core.database.workbench.dao

import androidx.room.*
import com.aigrowth.os.core.database.workbench.entity.ReadingItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingItemDao {
    @Query("SELECT * FROM reading_items ORDER BY pinned DESC, createdAt DESC")
    fun getAll(): Flow<List<ReadingItem>>

    @Query("SELECT * FROM reading_items WHERE pinned = 1 ORDER BY createdAt DESC")
    fun getPinned(): Flow<List<ReadingItem>>

    @Query("SELECT * FROM reading_items WHERE id = :id")
    suspend fun getById(id: String): ReadingItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ReadingItem)

    @Update
    suspend fun update(item: ReadingItem)

    @Delete
    suspend fun delete(item: ReadingItem)
}