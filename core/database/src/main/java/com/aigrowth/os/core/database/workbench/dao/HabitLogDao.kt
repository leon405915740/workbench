package com.aigrowth.os.core.database.workbench.dao

import androidx.room.*
import com.aigrowth.os.core.database.workbench.entity.HabitLog
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitLogDao {
    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY date DESC")
    fun getLogsForHabit(habitId: String): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_logs WHERE date = :date")
    fun getLogsByDate(date: String): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_logs WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getLogsInRange(startDate: String, endDate: String): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND date = :date")
    suspend fun getByHabitAndDate(habitId: String, date: String): HabitLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: HabitLog)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND date = :date")
    suspend fun deleteByHabitAndDate(habitId: String, date: String)
}