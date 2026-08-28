package com.aigrowth.os.core.database.workbench.dao

import androidx.room.*
import com.aigrowth.os.core.database.workbench.entity.StatusTrendEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface StatusTrendDao {
    @Query("SELECT * FROM status_trend_entries ORDER BY date DESC")
    fun getAll(): Flow<List<StatusTrendEntry>>

    @Query("SELECT * FROM status_trend_entries WHERE date = :date")
    suspend fun getByDate(date: String): StatusTrendEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: StatusTrendEntry)
}