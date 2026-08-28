package com.aigrowth.os.core.database.workbench.dao

import androidx.room.*
import com.aigrowth.os.core.database.workbench.entity.PomodoroState
import kotlinx.coroutines.flow.Flow

@Dao
interface PomodoroStateDao {
    @Query("SELECT * FROM pomodoro_state WHERE id = :id")
    fun observe(id: String): Flow<PomodoroState?>

    @Query("SELECT * FROM pomodoro_state WHERE id = :id")
    suspend fun get(id: String): PomodoroState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: PomodoroState)
}