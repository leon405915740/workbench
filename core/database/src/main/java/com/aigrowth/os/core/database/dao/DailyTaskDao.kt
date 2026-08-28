package com.aigrowth.os.core.database.dao

import androidx.room.*
import com.aigrowth.os.core.database.entity.DailyTask
import com.aigrowth.os.core.database.entity.TaskStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyTaskDao {
    @Query("SELECT * FROM daily_tasks WHERE scheduledDate >= :startOfDay AND scheduledDate < :endOfDay ORDER BY scheduledDate ASC")
    fun getTasksForDay(startOfDay: Long, endOfDay: Long): Flow<List<DailyTask>>
    
    @Query("SELECT * FROM daily_tasks WHERE status = :status ORDER BY scheduledDate ASC")
    fun getTasksByStatus(status: TaskStatus): Flow<List<DailyTask>>
    
    @Query("SELECT * FROM daily_tasks WHERE id = :id")
    suspend fun getTaskById(id: String): DailyTask?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: DailyTask)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<DailyTask>)
    
    @Update
    suspend fun updateTask(task: DailyTask)
    
    @Query("UPDATE daily_tasks SET status = :status, completedAt = :completedAt WHERE id = :id")
    suspend fun updateTaskStatus(id: String, status: TaskStatus, completedAt: Long?)
}