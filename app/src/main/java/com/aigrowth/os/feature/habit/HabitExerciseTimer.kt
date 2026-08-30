package com.aigrowth.os.feature.habit

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.aigrowth.os.R

/**
 * 运动计时状态持久化：按 habitId 维度把 running + startedAt 存进 SharedPreferences，
 * 进程被杀后按 startedAt 恢复计时。与番茄钟（Room 单例状态）相互独立、可并行。
 */
object ExerciseTimerStore {
    private const val PREFS = "habit_exercise_timer"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun runningKey(habitId: String) = "running_$habitId"
    private fun startedKey(habitId: String) = "startedAt_$habitId"

    fun setRunning(context: Context, habitId: String, running: Boolean, startedAt: Long?) {
        prefs(context).edit().apply {
            putBoolean(runningKey(habitId), running)
            if (startedAt != null) putLong(startedKey(habitId), startedAt) else remove(startedKey(habitId))
        }.apply()
    }

    /** 返回所有进行中的计时：habitId -> startedAt（毫秒）。 */
    fun runningTimers(context: Context): Map<String, Long> {
        val p = prefs(context)
        return p.all.keys
            .filter { it.startsWith("running_") && p.getBoolean(it, false) }
            .mapNotNull { key ->
                val id = key.removePrefix("running_")
                p.getLong(startedKey(id), 0L).takeIf { it > 0L }?.let { id to it }
            }
            .toMap()
    }

    fun clear(context: Context, habitId: String) {
        prefs(context).edit().apply {
            remove(runningKey(habitId))
            remove(startedKey(habitId))
        }.apply()
    }
}

/**
 * 运动计时前台通知：独立渠道 + 独立样式，通知 ID 按 habitId 唯一，
 * 与番茄钟通知并排显示、互不覆盖。
 */
object ExerciseTimerNotifier {
    private const val CHANNEL_ID = "habit_exercise_timer"

    fun notificationId(habitId: String): Int = habitId.hashCode() and 0x7fffffff

    private fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "运动计时", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    fun showRunning(context: Context, habitId: String, title: String?, elapsedSeconds: Long) {
        ensureChannel(context)
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title ?: "运动计时")
            .setContentText("已计时 ${formatExerciseElapsed(elapsedSeconds)}")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .build()
        try {
            context.getSystemService(NotificationManager::class.java)
                ?.notify(notificationId(habitId), notification)
        } catch (_: SecurityException) {
            // 通知权限未授予时静默失败，不打断计时逻辑
        }
    }

    fun cancel(context: Context, habitId: String) {
        try {
            context.getSystemService(NotificationManager::class.java)
                ?.cancel(notificationId(habitId))
        } catch (_: Exception) {
            // 忽略取消失败
        }
    }
}

/** 秒 -> "Xh Ym Zs" / "Ym Zs" / "Zs"，用于卡片与通知的时长展示。 */
fun formatExerciseElapsed(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return when {
        h > 0 -> "${h}时${m}分${s}秒"
        m > 0 -> "${m}分${s}秒"
        else -> "${s}秒"
    }
}