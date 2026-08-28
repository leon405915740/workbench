package com.aigrowth.os.core.database.workbench.dao

import androidx.room.*
import com.aigrowth.os.core.database.workbench.entity.Clipping
import kotlinx.coroutines.flow.Flow

@Dao
interface ClippingDao {
    @Query("SELECT * FROM clippings ORDER BY pinned DESC, date DESC, createdAt DESC")
    fun getAll(): Flow<List<Clipping>>

    @Query("SELECT * FROM clippings WHERE pinned = 1 ORDER BY date DESC")
    fun getPinned(): Flow<List<Clipping>>

    @Query("SELECT * FROM clippings WHERE id = :id")
    suspend fun getById(id: String): Clipping?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(clipping: Clipping)

    @Update
    suspend fun update(clipping: Clipping)

    @Delete
    suspend fun delete(clipping: Clipping)
}