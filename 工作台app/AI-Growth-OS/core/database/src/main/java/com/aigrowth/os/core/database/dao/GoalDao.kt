package com.aigrowth.os.core.database.dao

import androidx.room.*
import com.aigrowth.os.core.database.entity.Goal
import com.aigrowth.os.core.database.entity.GoalStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE status = :status ORDER BY createdAt DESC")
    fun getGoalsByStatus(status: GoalStatus): Flow<List<Goal>>
    
    @Query("SELECT * FROM goals ORDER BY createdAt DESC")
    fun getAllGoals(): Flow<List<Goal>>
    
    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getGoalById(id: String): Goal?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal)
    
    @Update
    suspend fun updateGoal(goal: Goal)
    
    @Delete
    suspend fun deleteGoal(goal: Goal)
    
    @Query("UPDATE goals SET status = :status WHERE id = :id")
    suspend fun updateGoalStatus(id: String, status: GoalStatus)
}