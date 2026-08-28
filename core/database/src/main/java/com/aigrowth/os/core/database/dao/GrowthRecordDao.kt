package com.aigrowth.os.core.database.dao

import androidx.room.*
import com.aigrowth.os.core.database.entity.GrowthRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface GrowthRecordDao {
    @Query("SELECT * FROM growth_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<GrowthRecord>>

    @Query("SELECT * FROM growth_records WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getRecordsInRange(startDate: Long, endDate: Long): Flow<List<GrowthRecord>>

    @Query("SELECT * FROM growth_records WHERE date = :date")
    suspend fun getRecordByDate(date: Long): GrowthRecord?

    @Query("SELECT * FROM growth_records WHERE date = :date")
    fun observeRecordByDate(date: Long): Flow<GrowthRecord?>

    @Query("SELECT * FROM growth_records ORDER BY date DESC LIMIT 7")
    fun getRecentRecords(): Flow<List<GrowthRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: GrowthRecord)

    @Update
    suspend fun updateRecord(record: GrowthRecord)

    @Delete
    suspend fun deleteRecord(record: GrowthRecord)
}
