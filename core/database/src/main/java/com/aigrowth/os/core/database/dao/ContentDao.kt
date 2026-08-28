package com.aigrowth.os.core.database.dao

import androidx.room.*
import com.aigrowth.os.core.database.entity.Content
import com.aigrowth.os.core.database.entity.ContentStatus
import com.aigrowth.os.core.database.entity.ContentType
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentDao {

    @Query("SELECT * FROM contents ORDER BY updatedAt DESC")
    fun getAllContents(): Flow<List<Content>>

    @Query("SELECT * FROM contents WHERE contentType = :contentType ORDER BY updatedAt DESC")
    fun getContentsByType(contentType: ContentType): Flow<List<Content>>

    @Query("SELECT * FROM contents WHERE status = :status ORDER BY updatedAt DESC")
    fun getContentsByStatus(status: ContentStatus): Flow<List<Content>>

    @Query("SELECT * FROM contents WHERE id = :id")
    suspend fun getContentById(id: String): Content?

    @Query("SELECT * FROM contents WHERE contentType = :contentType AND status = :status ORDER BY updatedAt DESC")
    fun getContentsByTypeAndStatus(contentType: ContentType, status: ContentStatus): Flow<List<Content>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContent(content: Content)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContents(contents: List<Content>)

    @Update
    suspend fun updateContent(content: Content)

    @Delete
    suspend fun deleteContent(content: Content)

    @Query("DELETE FROM contents WHERE id = :id")
    suspend fun deleteContentById(id: String)

    @Query("SELECT COUNT(*) FROM contents WHERE contentType = :contentType")
    suspend fun countByType(contentType: ContentType): Int
}
