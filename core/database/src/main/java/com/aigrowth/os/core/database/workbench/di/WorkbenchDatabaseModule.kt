package com.aigrowth.os.core.database.workbench.di

import android.content.Context
import androidx.room.Room
import com.aigrowth.os.core.database.workbench.WorkbenchDatabase
import com.aigrowth.os.core.database.workbench.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WorkbenchDatabaseModule {

    @Provides
    @Singleton
    fun provideWorkbenchDatabase(
        @ApplicationContext context: Context
    ): WorkbenchDatabase {
        return Room.databaseBuilder(
            context,
            WorkbenchDatabase::class.java,
            "workbench.db"
        ).addMigrations(WorkbenchDatabase.MIGRATION_1_2, WorkbenchDatabase.MIGRATION_2_3)
            .build()
    }

    @Provides
    fun providePlanItemDao(database: WorkbenchDatabase): PlanItemDao = database.planItemDao()

    @Provides
    fun provideHabitDao(database: WorkbenchDatabase): HabitDao = database.habitDao()

    @Provides
    fun provideHabitLogDao(database: WorkbenchDatabase): HabitLogDao = database.habitLogDao()

    @Provides
    fun provideReadingItemDao(database: WorkbenchDatabase): ReadingItemDao = database.readingItemDao()

    @Provides
    fun provideReadingLogDao(database: WorkbenchDatabase): ReadingLogDao = database.readingLogDao()

    @Provides
    fun provideExerciseItemDao(database: WorkbenchDatabase): ExerciseItemDao = database.exerciseItemDao()

    @Provides
    fun provideEssayDao(database: WorkbenchDatabase): EssayDao = database.essayDao()

    @Provides
    fun provideClippingDao(database: WorkbenchDatabase): ClippingDao = database.clippingDao()

    @Provides
    fun provideStatusTrendDao(database: WorkbenchDatabase): StatusTrendDao = database.statusTrendDao()

    @Provides
    fun providePomodoroStateDao(database: WorkbenchDatabase): PomodoroStateDao = database.pomodoroStateDao()
}