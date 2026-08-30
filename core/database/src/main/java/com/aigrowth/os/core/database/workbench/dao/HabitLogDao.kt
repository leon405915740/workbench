package com.aigrowth.os.core.database.workbench.dao

import androidx.room.*
import com.aigrowth.os.core.database.workbench.entity.ExerciseCategoryEnum
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

    /** 原子累加当日时长（单条 UPDATE，无记录不新增）。返回受影响行数。 */
    @Query("UPDATE habit_logs SET durationMinutes = COALESCE(durationMinutes, 0) + :deltaMinutes WHERE habitId = :habitId AND date = :date")
    suspend fun addDuration(habitId: String, date: String, deltaMinutes: Int): Int

    /** 按周/月/年（即起止日期范围）聚合时长。 */
    @Query("SELECT COALESCE(SUM(durationMinutes), 0) FROM habit_logs WHERE date >= :startDate AND date <= :endDate")
    suspend fun sumDurationInRange(startDate: String, endDate: String): Long

    /** 按分类聚合时长（日期范围可选过滤）。 */
    @Query("SELECT COALESCE(SUM(durationMinutes), 0) FROM habit_logs WHERE category = :category AND date >= :startDate AND date <= :endDate")
    suspend fun sumDurationForCategory(category: ExerciseCategoryEnum, startDate: String, endDate: String): Long
}