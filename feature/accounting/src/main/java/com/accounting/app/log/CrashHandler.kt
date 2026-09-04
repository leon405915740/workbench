package com.accounting.app.log

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat

/**
 * 全局未捕获异常处理器。
 *
 * 在 Application.onCreate 中调用 [init] 安装。捕获到未处理异常时：
 * 1. 收集设备/应用环境信息（机型、系统版本、App 版本、构建类型）
 * 2. 通过 [AppLogger.logCrash] 写入日志文件（绕过日志开关，始终落盘）
 * 3. 委派给系统默认处理器，确保进程仍按原有方式终止/上报
 *
 * 设计原则：只"顺手记录"，不吞异常、不阻止系统崩溃流程。
 */
object CrashHandler {

    @Volatile
    private var installed = false

    fun init(context: Context) {
        if (installed) return
        installed = true
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val env = collectEnv(context, thread)
                AppLogger.logCrash(throwable, env)
            } catch (e: Throwable) {
                // 记录崩溃本身失败时不应影响后续流程
            }
            // 委派给系统默认处理器，保持原有崩溃行为
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun collectEnv(context: Context, thread: Thread): String {
        val packageInfo = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }.getOrNull()
        val versionName = packageInfo?.versionName ?: "unknown"
        val versionCode = packageInfo?.let(PackageInfoCompat::getLongVersionCode) ?: -1L
        val debug = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        return buildString {
            append("线程: ${thread.name}")
            append(" | 机型: ${Build.MANUFACTURER} ${Build.MODEL}")
            append(" | Android: ${Build.VERSION.SDK_INT}(${Build.VERSION.RELEASE})")
            append(" | App: v$versionName($versionCode)")
            append(" | 构建类型: ${if (debug) "debug" else "release"}")
        }
    }
}
