package com.aigrowth.os.core.database.workbench.dao

import androidx.room.*
import com.aigrowth.os.core.database.workbench.entity.Habit
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY pinned DESC, createdAt ASC")
    fun getAll(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE active = 1 ORDER BY pinned DESC, createdAt ASC")
    fun getActive(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE pinned = 1 ORDER BY createdAt ASC")
    fun getPinned(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getById(id: String): Habit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(habit: Habit)

    @Update
    suspend fun update(habit: Habit)

    @Delete
    suspend fun delete(habit: Habit)
}