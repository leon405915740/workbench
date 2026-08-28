package com.aigrowth.os.core.database.dao

import androidx.room.*
import com.aigrowth.os.core.database.entity.AIMemory
import com.aigrowth.os.core.database.entity.MemoryType
import kotlinx.coroutines.flow.Flow

@Dao
interface AIMemoryDao {
    @Query("SELECT * FROM ai_memories WHERE memoryType = :type ORDER BY importance DESC, createdAt DESC")
    fun getMemoriesByType(type: MemoryType): Flow<List<AIMemory>>
    
    @Query("SELECT * FROM ai_memories ORDER BY importance DESC, lastAccessedAt DESC")
    fun getAllMemories(): Flow<List<AIMemory>>
    
    @Query("SELECT * FROM ai_memories WHERE id = :id")
    suspend fun getMemoryById(id: String): AIMemory?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: AIMemory)
    
    @Update
    suspend fun updateMemory(memory: AIMemory)
    
    @Delete
    suspend fun deleteMemory(memory: AIMemory)
    
    @Query("UPDATE ai_memories SET lastAccessedAt = :timestamp WHERE id = :id")
    suspend fun updateLastAccessed(id: String, timestamp: Long)
}