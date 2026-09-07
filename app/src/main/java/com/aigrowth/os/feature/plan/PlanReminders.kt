package com.aigrowth.os.feature.plan

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.aigrowth.os.MainActivity
import com.aigrowth.os.R
import com.aigrowth.os.core.database.workbench.dao.PlanItemDao
import com.aigrowth.os.core.database.workbench.entity.PlanItem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

private const val ACTION_PLAN_REMINDER = "com.aigrowth.os.action.PLAN_REMINDER"
private const val ACTION_EXACT_ALARM_PERMISSION_STATE_CHANGED =
    "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
private const val EXTRA_PLAN_ID = "plan_id"

internal fun reminderTriggerAt(
    planDate: String,
    planTime: String,
    zoneId: ZoneId = ZoneId.systemDefault()
): Long? = runCatching {
    LocalDate.parse(planDate)
        .atTime(LocalTime.parse(planTime))
        .atZone(zoneId)
        .toInstant()
        .toEpochMilli()
}.getOrNull()

object PlanReminders {
    private const val CHANNEL_ID = "plan_reminders"

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    fun requestExactAlarmAccess(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || canScheduleExact(context)) return
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun hasFutureTrigger(planDate: String, planTime: String, now: Long = System.currentTimeMillis()): Boolean =
        reminderTriggerAt(planDate, planTime)?.let { it > now } == true

    fun schedule(context: Context, item: PlanItem) {
        cancelAlarm(context, item.id)
        if (item.done) return
        val planTime = item.planTime ?: return
        val triggerAt = reminderTriggerAt(item.planDate, planTime) ?: return
        if (triggerAt <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val operation = reminderPendingIntent(context, item.id)
        if (canScheduleExact(context)) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
            } catch (_: SecurityException) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
            }
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
        }
    }

    fun cancel(context: Context, planId: String) {
        cancelAlarm(context, planId)
        context.getSystemService(NotificationManager::class.java)?.cancel(planId, 0)
    }

    private fun cancelAlarm(context: Context, planId: String) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val operation = reminderPendingIntent(context, planId)
        alarmManager?.cancel(operation)
        operation.cancel()
    }

    fun show(context: Context, planId: String, title: String) {
        val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return
        ensureChannel(notificationManager)
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("待办事项提醒")
            .setContentText(title)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        try {
            notificationManager.notify(planId, 0, notification)
        } catch (_: SecurityException) {
            // 用户未授予通知权限时不让广播接收器崩溃。
        }
    }

    private fun ensureChannel(notificationManager: NotificationManager) {
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            notificationManager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "待办事项提醒", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    private fun reminderPendingIntent(context: Context, planId: String): PendingIntent {
        val intent = Intent(context, PlanReminderReceiver::class.java).apply {
            action = ACTION_PLAN_REMINDER
            data = Uri.parse("workbench://plan-reminder/${Uri.encode(planId)}")
            putExtra(EXTRA_PLAN_ID, planId)
        }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

@AndroidEntryPoint
class PlanReminderReceiver : BroadcastReceiver() {
    @Inject
    lateinit var planItemDao: PlanItemDao

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_PLAN_REMINDER) return
        val planId = intent.getStringExtra(EXTRA_PLAN_ID) ?: return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val item = planItemDao.getById(planId)
                val triggerAt = item?.planTime?.let { reminderTriggerAt(item.planDate, it) }
                if (item != null && !item.done && triggerAt != null && triggerAt <= System.currentTimeMillis()) {
                    PlanReminders.show(context, planId, item.title)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

@AndroidEntryPoint
class PlanReminderRestoreReceiver : BroadcastReceiver() {
    @Inject
    lateinit var planItemDao: PlanItemDao

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in RESTORE_ACTIONS) return
        if (intent.action == ACTION_EXACT_ALARM_PERMISSION_STATE_CHANGED &&
            !PlanReminders.canScheduleExact(context)
        ) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                planItemDao.getReminderCandidates().forEach { PlanReminders.schedule(context, it) }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        val RESTORE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            ACTION_EXACT_ALARM_PERMISSION_STATE_CHANGED
        )
    }
}
