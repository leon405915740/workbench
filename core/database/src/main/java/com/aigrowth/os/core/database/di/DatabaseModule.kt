package com.aigrowth.os.core.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "ai_growth_os.db"
        )
            .build()
    }
    
    @Provides
    fun provideGoalDao(database: AppDatabase) = database.goalDao()
    
    @Provides
    fun provideLearningPathDao(database: AppDatabase) = database.learningPathDao()
    
    @Provides
    fun provideLearningLevelDao(database: AppDatabase) = database.learningLevelDao()
    
    @Provides
    fun provideDailyTaskDao(database: AppDatabase) = database.dailyTaskDao()
    
    @Provides
    fun provideKnowledgeCardDao(database: AppDatabase) = database.knowledgeCardDao()
    
    @Provides
    fun provideAIMemoryDao(database: AppDatabase) = database.aiMemoryDao()

    @Provides
    fun provideAIConversationDao(database: AppDatabase) = database.aiConversationDao()

    @Provides
    fun provideFeynmanSessionDao(database: AppDatabase) = database.feynmanSessionDao()

    @Provides
    fun provideGrowthRecordDao(database: AppDatabase) = database.growthRecordDao()

    @Provides
    fun provideContentDao(database: AppDatabase) = database.contentDao()
}
