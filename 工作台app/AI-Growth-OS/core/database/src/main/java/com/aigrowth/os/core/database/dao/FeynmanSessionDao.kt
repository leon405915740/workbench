package com.aigrowth.os.core.database.dao

import androidx.room.*
import com.aigrowth.os.core.database.entity.FeynmanSession
import com.aigrowth.os.core.database.entity.FeynmanStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface FeynmanSessionDao {
    @Query("SELECT * FROM feynman_sessions WHERE knowledgeCardId = :knowledgeCardId ORDER BY createdAt DESC")
    fun getSessionsByCard(knowledgeCardId: String): Flow<List<FeynmanSession>>

    @Query("SELECT * FROM feynman_sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<FeynmanSession>>

    @Query("SELECT * FROM feynman_sessions WHERE id = :id")
    suspend fun getSessionById(id: String): FeynmanSession?

    @Query("SELECT * FROM feynman_sessions WHERE status = :status ORDER BY createdAt DESC")
    fun getSessionsByStatus(status: FeynmanStatus): Flow<List<FeynmanSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FeynmanSession)

    @Update
    suspend fun updateSession(session: FeynmanSession)

    @Delete
    suspend fun deleteSession(session: FeynmanSession)
}
