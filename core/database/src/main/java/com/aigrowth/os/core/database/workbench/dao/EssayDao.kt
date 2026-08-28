package com.aigrowth.os.core.database.workbench.dao

import androidx.room.*
import com.aigrowth.os.core.database.workbench.entity.Essay
import kotlinx.coroutines.flow.Flow

@Dao
interface EssayDao {
    @Query("SELECT * FROM essays ORDER BY pinned DESC, date DESC, createdAt DESC")
    fun getAll(): Flow<List<Essay>>

    @Query("SELECT * FROM essays WHERE pinned = 1 ORDER BY date DESC")
    fun getPinned(): Flow<List<Essay>>

    @Query("SELECT * FROM essays WHERE id = :id")
    suspend fun getById(id: String): Essay?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(essay: Essay)

    @Update
    suspend fun update(essay: Essay)

    @Delete
    suspend fun delete(essay: Essay)
}