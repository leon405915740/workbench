package com.aigrowth.os.core.database.workbench.dao

import androidx.room.*
import com.aigrowth.os.core.database.workbench.entity.ReadingLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingLogDao {
    @Query("SELECT * FROM reading_logs WHERE readingItemId = :readingItemId ORDER BY date DESC, createdAt DESC")
    fun getAllByItem(readingItemId: String): Flow<List<ReadingLog>>

    @Query("SELECT * FROM reading_logs WHERE readingItemId = :readingItemId AND date = :date")
    suspend fun getByItemAndDate(readingItemId: String, date: String): ReadingLog?

    @Query("SELECT COALESCE(SUM(amount), 0) FROM reading_logs WHERE date = :date")
    suspend fun sumAmountOn(date: String): Float

    @Query("SELECT COALESCE(SUM(amount), 0) FROM reading_logs WHERE date >= :start AND date <= :end")
    suspend fun sumAmountInRange(start: String, end: String): Float

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: ReadingLog)

    @Delete
    suspend fun delete(log: ReadingLog)

    @Query("SELECT * FROM reading_logs WHERE id = :id")
    suspend fun getById(id: String): ReadingLog?
}
