package com.aigrowth.os.feature.simple

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.accounting.app.log.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.simpleDataStore by preferencesDataStore("simple_workbench")
data class SimpleItem(val id: String, val text: String, val done: Boolean = false)
class SimpleDataStore(private val context: Context) {
    private val appContext = context.applicationContext

    private fun key(kind: String) = stringPreferencesKey("items_$kind")
    fun items(kind: String): Flow<List<SimpleItem>> = appContext.simpleDataStore.data.map { prefs ->
        try {
            val array = JSONArray(prefs[key(kind)] ?: "[]")
            List(array.length()) { index ->
                array.getJSONObject(index).let {
                    SimpleItem(it.getString("id"), it.getString("text"), it.optBoolean("done"))
                }
            }
        } catch (error: Exception) {
            val requestId = AppLogger.generateRequestId()
            AppLogger.e(requestId, "SimpleDataStore", "读取异常，已回退空列表: ${error.javaClass.simpleName}", error)
            emptyList()
        }
    }

    suspend fun save(
        kind: String,
        items: List<SimpleItem>,
        node: String,
        requestId: String,
    ) {
        AppLogger.i(requestId, node, "save 入口: kind=$kind, count=${items.size}")
        try {
            val array = JSONArray().apply {
                items.forEach {
                    put(JSONObject().put("id", it.id).put("text", it.text).put("done", it.done))
                }
            }
            appContext.simpleDataStore.edit { preferences -> preferences[key(kind)] = array.toString() }
            AppLogger.d(requestId, node, "save 成功: kind=$kind, count=${items.size}")
        } catch (error: Exception) {
            AppLogger.e(requestId, node, "save 异常: kind=$kind, error=${error.javaClass.simpleName}", error)
            throw error
        }
    }

    fun newId() = UUID.randomUUID().toString()
}
