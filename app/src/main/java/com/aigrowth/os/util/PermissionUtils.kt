package com.aigrowth.os.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.accounting.app.log.AppLogger

fun Context.isNotificationAccessEnabled(): Boolean {
    val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
    val pkg = packageName
    return flat.split(':').any { it.startsWith("$pkg/") || it == pkg }
}

fun Context.isOverlayGranted(): Boolean = Settings.canDrawOverlays(this)

fun Context.openNotificationAccessSettings() {
    val requestId = AppLogger.generateRequestId()
    AppLogger.i(requestId, "PermissionUtils", "openNotificationAccessSettings 入口")
    runCatching {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }.onSuccess {
        AppLogger.d(requestId, "PermissionUtils", "openNotificationAccessSettings 成功")
    }.onFailure {
        AppLogger.e(requestId, "PermissionUtils", "openNotificationAccessSettings 失败", it)
    }
}

fun Context.openOverlaySettings() {
    val requestId = AppLogger.generateRequestId()
    AppLogger.i(requestId, "PermissionUtils", "openOverlaySettings 入口")
    runCatching {
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
    }.onSuccess {
        AppLogger.d(requestId, "PermissionUtils", "openOverlaySettings 成功")
    }.onFailure {
        AppLogger.e(requestId, "PermissionUtils", "openOverlaySettings 失败", it)
    }
}
