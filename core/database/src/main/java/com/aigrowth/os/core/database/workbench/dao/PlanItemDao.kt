package com.aigrowth.os.core.database.workbench.dao

import androidx.room.*
import com.aigrowth.os.core.database.workbench.entity.PlanItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanItemDao {
    @Query("SELECT * FROM plan_items ORDER BY pinned DESC, createdAt DESC")
    fun getAll(): Flow<List<PlanItem>>

    @Query("SELECT * FROM plan_items WHERE planDate = :date ORDER BY pinned DESC, createdAt DESC")
    fun getByDate(date: String): Flow<List<PlanItem>>

    @Query("SELECT * FROM plan_items WHERE pinned = 1 ORDER BY createdAt DESC")
    fun getPinned(): Flow<List<PlanItem>>

    @Query("SELECT * FROM plan_items WHERE id = :id")
    suspend fun getById(id: String): PlanItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PlanItem)

    @Update
    suspend fun update(item: PlanItem)

    @Delete
    suspend fun delete(item: PlanItem)
}