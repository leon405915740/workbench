package com.aigrowth.os.core.database.dao

import androidx.room.*
import com.aigrowth.os.core.database.entity.KnowledgeCard
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeCardDao {
    @Query("SELECT * FROM knowledge_cards WHERE learningLevelId = :learningLevelId ORDER BY createdAt DESC")
    fun getCardsByLevel(learningLevelId: String): Flow<List<KnowledgeCard>>
    
    @Query("SELECT * FROM knowledge_cards ORDER BY updatedAt DESC")
    fun getAllCards(): Flow<List<KnowledgeCard>>
    
    @Query("SELECT * FROM knowledge_cards WHERE id = :id")
    suspend fun getCardById(id: String): KnowledgeCard?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: KnowledgeCard)
    
    @Update
    suspend fun updateCard(card: KnowledgeCard)
    
    @Delete
    suspend fun deleteCard(card: KnowledgeCard)
}