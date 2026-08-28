package com.aigrowth.os.core.aiengine.di

import android.content.Context
import com.aigrowth.os.core.aiengine.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AIEngineModule {

    // ApiKeyService 使用 @Inject constructor + @ApplicationContext 自动注入
    // 无需在此重复 @Provides，避免 Hilt 重复绑定
}