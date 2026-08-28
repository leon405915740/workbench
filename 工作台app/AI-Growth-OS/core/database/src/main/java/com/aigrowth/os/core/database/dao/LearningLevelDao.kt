package com.aigrowth.os.core.database.dao

import androidx.room.*
import com.aigrowth.os.core.database.entity.LearningLevel
import com.aigrowth.os.core.database.entity.LevelStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface LearningLevelDao {
    @Query("SELECT * FROM learning_levels WHERE learningPathId = :learningPathId ORDER BY levelNumber ASC")
    fun getLevelsByLearningPath(learningPathId: String): Flow<List<LearningLevel>>
    
    @Query("SELECT * FROM learning_levels WHERE id = :id")
    suspend fun getLevelById(id: String): LearningLevel?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLevels(levels: List<LearningLevel>)
    
    @Update
    suspend fun updateLevel(level: LearningLevel)
    
    @Query("UPDATE learning_levels SET status = :status WHERE id = :id")
    suspend fun updateLevelStatus(id: String, status: LevelStatus)
}