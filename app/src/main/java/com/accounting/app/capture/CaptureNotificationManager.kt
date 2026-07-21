package com.accounting.app.capture

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import android.annotation.SuppressLint
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.accounting.app.MainActivity
import com.accounting.app.R
import com.accounting.app.data.model.BillExecutePlan
import com.accounting.app.log.AppLogger

object CaptureNotificationManager {
    private const val CHANNEL_ID = "auto_capture_notifications"
    private const val CHANNEL_NAME = "自动采集通知"
    private const val CHANNEL_ID_FOREGROUND = "auto_capture_foreground"
    private const val CHANNEL_NAME_FOREGROUND = "自动采集后台服务"
    private const val NOTIFICATION_ID = 1001
    private const val FOREGROUND_NOTIFICATION_ID = 1002

    fun initChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "自动采集支付记录的通知"
            }
            val foregroundChannel = NotificationChannel(
                CHANNEL_ID_FOREGROUND,
                CHANNEL_NAME_FOREGROUND,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持自动采集服务后台运行"
                setShowBadge(false)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(foregroundChannel)
        }
    }

    fun createForegroundNotification(context: Context): android.app.Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(context, CHANNEL_ID_FOREGROUND)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("记账服务运行中")
            .setContentText("自动采集功能已开启")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setShowWhen(false)
            .build()
    }

    fun getForegroundNotificationId(): Int = FOREGROUND_NOTIFICATION_ID

    @SuppressLint("MissingPermission")
    fun showCaptureSuccess(context: Context, plan: BillExecutePlan, billIds: List<Long>) {
        if (!hasNotificationPermission(context)) {
            AppLogger.w(plan.requestId, "AutoCapture_Notification", "未获取通知权限，跳过通知")
            return
        }
        val item = plan.items.firstOrNull() ?: return
        val title = "记账成功"
        val content = "${item.merchant} ${item.amount}分"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            AppLogger.i(plan.requestId, "AutoCapture_Notification", "发送成功通知，merchant=${item.merchant}, amount=${item.amount}")
        } catch (e: SecurityException) {
            AppLogger.e(plan.requestId, "CaptureNotificationManager", "通知权限被拒绝: ${e.message}", null)
        }
    }

    @SuppressLint("MissingPermission")
    fun showCaptureFailed(context: Context, requestId: String, reason: String) {
        if (!hasNotificationPermission(context)) {
            AppLogger.w(requestId, "AutoCapture_Notification", "未获取通知权限，跳过通知")
            return
        }
        val title = "自动记账失败"
        val content = reason

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            AppLogger.e(requestId, "AutoCapture_Notification", "发送失败通知，reason=$reason", null)
        } catch (e: SecurityException) {
            AppLogger.e(requestId, "CaptureNotificationManager", "通知权限被拒绝: ${e.message}", null)
        }
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
}