package com.aigrowth.os.core.database.dao

import androidx.room.*
import com.aigrowth.os.core.database.entity.LearningPath
import kotlinx.coroutines.flow.Flow

@Dao
interface LearningPathDao {
    @Query("SELECT * FROM learning_paths WHERE goalId = :goalId")
    fun getLearningPathByGoal(goalId: String): Flow<LearningPath?>
    
    @Query("SELECT * FROM learning_paths WHERE id = :id")
    suspend fun getLearningPathById(id: String): LearningPath?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLearningPath(learningPath: LearningPath)
    
    @Update
    suspend fun updateLearningPath(learningPath: LearningPath)
    
    @Delete
    suspend fun deleteLearningPath(learningPath: LearningPath)
}