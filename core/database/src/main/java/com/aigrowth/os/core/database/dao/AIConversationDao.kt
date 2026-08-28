package com.aigrowth.os.core.database.dao

import androidx.room.*
import com.aigrowth.os.core.database.entity.AIConversation
import kotlinx.coroutines.flow.Flow

@Dao
interface AIConversationDao {
    @Query("SELECT * FROM ai_conversations WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    fun getConversationsBySession(sessionId: String): Flow<List<AIConversation>>
    
    @Query("SELECT * FROM ai_conversations WHERE relatedTaskId = :taskId ORDER BY createdAt ASC")
    fun getConversationsByTask(taskId: String): Flow<List<AIConversation>>
    
    @Query("SELECT * FROM ai_conversations ORDER BY createdAt DESC")
    fun getAllConversations(): Flow<List<AIConversation>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: AIConversation)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(conversations: List<AIConversation>)
    
    @Delete
    suspend fun deleteConversation(conversation: AIConversation)
    
    @Query("DELETE FROM ai_conversations WHERE sessionId = :sessionId")
    suspend fun deleteConversationsBySession(sessionId: String)
}