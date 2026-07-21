package com.accounting.app.capture

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.accounting.app.log.AppLogger

private val Context.captureDataStore: DataStore<Preferences> by preferencesDataStore(name = "capture_settings")

object CapturePreferences {
    private val AUTO_CAPTURE_ENABLED = booleanPreferencesKey("auto_capture_enabled")
    private val NOTIFICATION_LISTENER_ENABLED = booleanPreferencesKey("auto_capture_notification_enabled")
    private val SHOW_PREVIEW = booleanPreferencesKey("show_preview")
    private val AUTO_SAVE = booleanPreferencesKey("auto_save")

    suspend fun saveAutoCaptureEnabled(context: Context, enabled: Boolean) {
        AppLogger.i("", "CapturePreferences", "更新自动采集: $enabled")
        context.captureDataStore.edit { preferences ->
            preferences[AUTO_CAPTURE_ENABLED] = enabled
        }
    }

    suspend fun getAutoCaptureEnabled(context: Context): Boolean {
        val value = context.captureDataStore.data.map { it[AUTO_CAPTURE_ENABLED] ?: false }.first()
        AppLogger.d("", "CapturePreferences", "读取自动采集: $value")
        return value
    }

    fun getAutoCaptureEnabledFlow(context: Context): Flow<Boolean> {
        return context.captureDataStore.data.map {
            val value = it[AUTO_CAPTURE_ENABLED] ?: false
            AppLogger.d("", "CapturePreferences", "读取自动采集: $value")
            value
        }
    }

    suspend fun saveNotificationListenerEnabled(context: Context, enabled: Boolean) {
        AppLogger.i("", "CapturePreferences", "更新通知监听: $enabled")
        context.captureDataStore.edit { preferences ->
            preferences[NOTIFICATION_LISTENER_ENABLED] = enabled
        }
    }

    suspend fun getNotificationListenerEnabled(context: Context): Boolean {
        val value = context.captureDataStore.data.map { it[NOTIFICATION_LISTENER_ENABLED] ?: false }.first()
        AppLogger.d("", "CapturePreferences", "读取通知监听: $value")
        return value
    }

    fun getNotificationListenerEnabledFlow(context: Context): Flow<Boolean> {
        return context.captureDataStore.data.map {
            val value = it[NOTIFICATION_LISTENER_ENABLED] ?: false
            AppLogger.d("", "CapturePreferences", "读取通知监听: $value")
            value
        }
    }

    suspend fun saveShowPreview(context: Context, enabled: Boolean) {
        AppLogger.i("", "CapturePreferences", "更新显示预览: $enabled")
        context.captureDataStore.edit { preferences ->
            preferences[SHOW_PREVIEW] = enabled
        }
    }

    suspend fun getShowPreview(context: Context): Boolean {
        val value = context.captureDataStore.data.map { it[SHOW_PREVIEW] ?: true }.first()
        AppLogger.d("", "CapturePreferences", "读取显示预览: $value")
        return value
    }

    suspend fun saveAutoSave(context: Context, enabled: Boolean) {
        AppLogger.i("", "CapturePreferences", "更新自动保存: $enabled")
        context.captureDataStore.edit { preferences ->
            preferences[AUTO_SAVE] = enabled
        }
    }

    suspend fun getAutoSave(context: Context): Boolean {
        val value = context.captureDataStore.data.map { it[AUTO_SAVE] ?: false }.first()
        AppLogger.d("", "CapturePreferences", "读取自动保存: $value")
        return value
    }
}