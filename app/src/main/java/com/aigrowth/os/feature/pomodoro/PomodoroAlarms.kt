package com.aigrowth.os.feature.pomodoro

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

const val EXTRA_MESSAGE = "pomodoro_message"

object PomodoroAlarms {
    const val CHANNEL_ID = "pomodoro"
    private const val REQUEST_CODE = 1001

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "番茄钟", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }

    fun show(context: Context, title: String, text: String) {
        ensureChannel(context)
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(com.aigrowth.os.R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .build()
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        try {
            nm.notify(1, notification)
        } catch (_: SecurityException) {
            // 通知权限未授予或渠道缺失时静默失败，不打断计时逻辑
        }
    }

    fun schedule(context: Context, delaySeconds: Int, message: String) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val triggerAt = System.currentTimeMillis() + delaySeconds * 1000L
        val pi = pendingIntent(context, message)
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } catch (_: SecurityException) {
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(pendingIntent(context, ""))
    }

    private fun pendingIntent(context: Context, message: String): PendingIntent {
        val intent = Intent(context, PomodoroReceiver::class.java).apply {
            putExtra(EXTRA_MESSAGE, message)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}