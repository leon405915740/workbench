package com.aigrowth.os.core.database.workbench.dao

import androidx.room.*
import com.aigrowth.os.core.database.workbench.entity.ExerciseItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseItemDao {
    @Query("SELECT * FROM exercise_items ORDER BY pinned DESC, createdAt DESC")
    fun getAll(): Flow<List<ExerciseItem>>

    @Query("SELECT * FROM exercise_items WHERE pinned = 1 ORDER BY createdAt DESC")
    fun getPinned(): Flow<List<ExerciseItem>>

    @Query("SELECT * FROM exercise_items WHERE id = :id")
    suspend fun getById(id: String): ExerciseItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ExerciseItem)

    @Update
    suspend fun update(item: ExerciseItem)

    @Delete
    suspend fun delete(item: ExerciseItem)
}