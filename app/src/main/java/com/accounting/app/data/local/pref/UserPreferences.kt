package com.accounting.app.data.local.pref

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Base64

private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)

private val KEY_DEEPSEEK_API_KEY = stringPreferencesKey("deepseek_api_key_encoded")
private val KEY_CHAT_HISTORY = stringPreferencesKey("chat_history")

/** 用于 DataStore 序列化的轻量消息记录 */
data class PersistedMessage(
    val type: String,       // "user" | "ai" | "aiText"
    val text: String,
    val rawInput: String,   // 仅 ErrorMessage 用，其他类型为空
    val timestamp: Long
)

/**
 * 用户偏好封装：负责 DataStore 读写。
 */
class UserPreferences(private val context: Context) {

    private val gson = Gson()

    fun getApiKey(): Flow<String> = context.userDataStore.data.map { prefs ->
        val stored = prefs[KEY_DEEPSEEK_API_KEY] ?: ""
        if (stored.isBlank()) "" else {
            try {
                String(Base64.getDecoder().decode(stored), Charsets.UTF_8)
            } catch (_: Exception) {
                // 解码失败说明数据损坏，返回空字符串
                ""
            }
        }
    }

    suspend fun setApiKey(key: String) {
        val encoded = Base64.getEncoder().encodeToString(key.toByteArray(Charsets.UTF_8))
        context.userDataStore.edit { prefs ->
            prefs[KEY_DEEPSEEK_API_KEY] = encoded
        }
    }

    /** 读取持久化的聊天记录 */
    fun getChatHistory(): Flow<List<PersistedMessage>> = context.userDataStore.data.map { prefs ->
        val json = prefs[KEY_CHAT_HISTORY] ?: ""
        if (json.isBlank()) emptyList()
        else try {
            gson.fromJson(json, object : TypeToken<List<PersistedMessage>>() {}.type)
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** 保存聊天记录（全量覆盖） */
    suspend fun saveChatHistory(messages: List<PersistedMessage>) {
        val json = gson.toJson(messages)
        context.userDataStore.edit { prefs ->
            prefs[KEY_CHAT_HISTORY] = json
        }
    }
}
