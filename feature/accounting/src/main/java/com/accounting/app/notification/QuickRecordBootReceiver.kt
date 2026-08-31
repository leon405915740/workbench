package com.accounting.app.notification

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import com.accounting.app.log.AppLogger

/**
 * 开机自愈：系统重启后请求重绑通知监听服务。
 *
 * 部分 ROM（华为/三星省电策略）在重启后不会自动恢复 NotificationListenerService，
 * 导致 App 进程直到用户手动打开前都不存在；此接收器在 BOOT_COMPLETED 时显式调用
 * requestRebind 让系统重新绑定并拉起进程（随后 onListenerConnected 前台化保活）。
 */
class QuickRecordBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        AppLogger.i("", NODE, "开机自愈：BOOT_COMPLETED，请求重绑通知监听")
        try {
            NotificationListenerService.requestRebind(
                ComponentName(context, QuickRecordNotificationService::class.java)
            )
        } catch (e: Exception) {
            AppLogger.e("", NODE, "开机重绑异常: ${e.message}", e)
        }
    }

    private companion object {
        const val NODE = "开机重绑"
    }
}
