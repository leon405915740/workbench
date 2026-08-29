package com.accounting.app.data.local.pref

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
private val KEY_OPENCODE_API_KEY = stringPreferencesKey("opencode_api_key_encoded")
private val KEY_PROVIDER = stringPreferencesKey("ai_provider")
private val KEY_MODEL = stringPreferencesKey("ai_model")
private val KEY_CHAT_HISTORY = stringPreferencesKey("chat_history")
private val KEY_AUTO_LEARN = booleanPreferencesKey("auto_learn_enabled")
private val KEY_QUICK_RECORD_ENABLED = booleanPreferencesKey("quick_record_enabled")

/** AI 提供商常量 */
object AiProviders {
    const val DEEPSEEK = "deepseek"
    const val OPENCODE_GO = "opencode_go"

    val ALL = listOf(DEEPSEEK, OPENCODE_GO)
}

/** 默认模型：DeepSeek v4 Flash Vision 实验版 */
private const val DEFAULT_MODEL = "deepseek-v4-flash-vision-exp"

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
                sanitizeKey(String(Base64.getDecoder().decode(stored), Charsets.UTF_8))
            } catch (_: Exception) {
                // 解码失败说明数据损坏，返回空字符串
                ""
            }
        }
    }

    suspend fun setApiKey(key: String) {
        val clean = sanitizeKey(key)
        val encoded = Base64.getEncoder().encodeToString(clean.toByteArray(Charsets.UTF_8))
        context.userDataStore.edit { prefs ->
            prefs[KEY_DEEPSEEK_API_KEY] = encoded
        }
    }

    /** 当前 AI 提供商（deepseek / opencode_go），默认 deepseek */
    fun getProvider(): Flow<String> = context.userDataStore.data.map { prefs ->
        prefs[KEY_PROVIDER] ?: AiProviders.DEEPSEEK
    }

    suspend fun setProvider(provider: String) {
        context.userDataStore.edit { prefs ->
            prefs[KEY_PROVIDER] = provider
        }
    }

    /** 当前 provider 对应的 API Key（deepseek 或 opencode_go） */
    fun getActiveApiKey(): Flow<String> = context.userDataStore.data.map { prefs ->
        when (prefs[KEY_PROVIDER] ?: AiProviders.DEEPSEEK) {
            AiProviders.OPENCODE_GO -> decodeKey(prefs[KEY_OPENCODE_API_KEY])
            else -> decodeKey(prefs[KEY_DEEPSEEK_API_KEY])
        }
    }

    /** OpenCode Go 专用 API Key */
    fun getOpenCodeApiKey(): Flow<String> = context.userDataStore.data.map { prefs ->
        decodeKey(prefs[KEY_OPENCODE_API_KEY])
    }

    suspend fun setOpenCodeApiKey(key: String) {
        val clean = sanitizeKey(key)
        context.userDataStore.edit { prefs ->
            prefs[KEY_OPENCODE_API_KEY] = Base64.getEncoder().encodeToString(clean.toByteArray(Charsets.UTF_8))
        }
    }

    /** 当前模型名，默认 deepseek-v4-flash-vision-exp */
    fun getModel(): Flow<String> = context.userDataStore.data.map { prefs ->
        prefs[KEY_MODEL] ?: DEFAULT_MODEL
    }

    suspend fun setModel(model: String) {
        context.userDataStore.edit { prefs ->
            prefs[KEY_MODEL] = model
        }
    }

    private fun decodeKey(encoded: String?): String {
        if (encoded.isNullOrBlank()) return ""
        return try {
            sanitizeKey(String(Base64.getDecoder().decode(encoded), Charsets.UTF_8))
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * 清洗 API Key：剔除 C0 控制字符（0x00-0x1F）与 DEL（0x7F）并去除首尾空白。
     *
     * 复制 API Key 时常会带入不可见控制字符（如 0x16 SYN），
     * 这些字符混入 Authorization 头会被 OkHttp 拒绝（Unexpected char 0x16 ...），
     * 因此在写入与读取时统一剥离，从源头规避该错误。
     */
    private fun sanitizeKey(raw: String): String =
        raw.filterNot { it.code < 0x20 || it.code == 0x7F }.trim()

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

    /** 记账后自动弹出学习确认开关（默认开启），工作台设置页与记账侧共用 */
    fun getAutoLearn(): Flow<Boolean> = context.userDataStore.data.map { prefs ->
        prefs[KEY_AUTO_LEARN] ?: true
    }

    suspend fun setAutoLearn(enabled: Boolean) {
        context.userDataStore.edit { prefs ->
            prefs[KEY_AUTO_LEARN] = enabled
        }
    }

    /** 付款后在通知栏弹出记账卡片开关（默认开启），工作台设置页与记账侧共用 */
    fun getQuickRecordEnabled(): Flow<Boolean> = context.userDataStore.data.map { prefs ->
        prefs[KEY_QUICK_RECORD_ENABLED] ?: true
    }

    suspend fun setQuickRecordEnabled(enabled: Boolean) {
        context.userDataStore.edit { prefs ->
            prefs[KEY_QUICK_RECORD_ENABLED] = enabled
        }
    }
}
