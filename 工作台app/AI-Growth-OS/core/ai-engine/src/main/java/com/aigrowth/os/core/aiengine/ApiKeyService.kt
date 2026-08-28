package com.aigrowth.os.core.aiengine

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/**
 * API Key服务
 * 管理API Key的读取和保存
 */
@Singleton
class ApiKeyService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefsName = "AI_Growth_OS_Prefs"
    private val keyApiKey = "api_key"
    private val keyModel = "ai_model"
    private val encodedApiKeyPrefix = "b64:"
    
    fun getApiKey(): String {
        val stored = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .getString(keyApiKey, "") ?: ""
        if (!stored.startsWith(encodedApiKeyPrefix)) return stored

        return runCatching {
            val encoded = stored.removePrefix(encodedApiKeyPrefix)
            String(Base64.getDecoder().decode(encoded), Charsets.UTF_8)
        }.getOrDefault("")
    }
    
    fun getSelectedModel(): String {
        return context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .getString(keyModel, "zen") ?: "zen"
    }
    
    fun isApiKeySet(): Boolean {
        return getApiKey().isNotEmpty()
    }
}
