package com.accounting.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.accounting.app.data.local.database.AppDatabase
import com.accounting.app.data.model.BillExecutePlan
import com.accounting.app.domain.classification.CategoryService
import com.accounting.app.log.AppLogger
import com.accounting.app.log.CrashHandler
import com.accounting.app.capture.CaptureNotificationManager
import com.accounting.app.data.repository.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

class AccountingApp : Application() {

    // ===== AppRepository 单例（避免重复创建） =====
    val appRepository: AppRepository by lazy { AppRepository(applicationContext) }

    // ===== Task 2: 前台状态管理（基于 resumed 计数，避免配置变更误判） =====
    private val resumedCount = AtomicInteger(0)

    // ===== Task 3: 防堆叠单例缓存 =====
    @Volatile
    private var pendingCapturePlan: BillExecutePlan? = null
    private val planLock = Any()

    companion object {
        @Volatile
        private var instance: AccountingApp? = null

        fun getInstance(): AccountingApp {
            return instance ?: throw IllegalStateException("AccountingApp not initialized")
        }

        /**
         * 判断应用是否在前台
         * @return true 表示前台，false 表示后台
         */
        fun isAppInForeground(): Boolean {
            return getInstance().resumedCount.get() > 0
        }

        /**
         * 设置待执行的采集计划（覆盖旧数据）
         */
        fun setPendingCapturePlan(plan: BillExecutePlan?) {
            getInstance().setPlanInternal(plan)
        }

        /**
         * 获取并清空待执行的采集计划
         * @return 缓存的计划，如果没有则返回 null
         */
        fun getAndClearPendingCapturePlan(): BillExecutePlan? {
            return getInstance().getAndClearPlanInternal()
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        AppLogger.init(this)
        CrashHandler.init()
        AppLogger.i("", "应用启动", "Application onCreate")
        CaptureNotificationManager.initChannel(this)
        val db = AppDatabase.getInstance(this)
        CategoryService.init(db.categoryDao())
        CoroutineScope(Dispatchers.IO).launch {
            CategoryService.loadCache()
            AppLogger.d("", "应用启动", "分类缓存加载完成")
        }
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                AppLogger.d("", "应用生命周期", "Activity启动: ${activity.javaClass.simpleName}")
            }

            override fun onActivityResumed(activity: Activity) {
                val wasInBackground = resumedCount.get() == 0
                resumedCount.incrementAndGet()
                if (wasInBackground) {
                    AppLogger.d("", "应用生命周期", "应用进入前台")
                }
            }

            override fun onActivityPaused(activity: Activity) {
                val wasInForeground = resumedCount.get() > 0
                resumedCount.decrementAndGet()
                if (wasInForeground && resumedCount.get() == 0) {
                    AppLogger.d("", "应用生命周期", "应用进入后台")
                }
            }

            override fun onActivityStopped(activity: Activity) {
                AppLogger.d("", "应用生命周期", "Activity停止: ${activity.javaClass.simpleName}")
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                AppLogger.d("", "应用生命周期", "Activity创建: ${activity.javaClass.simpleName}")
            }
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {
                AppLogger.d("", "应用生命周期", "Activity销毁: ${activity.javaClass.simpleName}")
            }
        })
    }

    // ===== Task 3: 内部方法实现 =====
    private fun setPlanInternal(plan: BillExecutePlan?) {
        synchronized(planLock) {
            val previousPlan = pendingCapturePlan
            val overwritePrevious = previousPlan != null
            pendingCapturePlan = plan
            AppLogger.d(
                plan?.requestId ?: "null",
                "Popup_Cache_Set",
                "overwritePrevious=$overwritePrevious"
            )
        }
    }

    private fun getAndClearPlanInternal(): BillExecutePlan? {
        synchronized(planLock) {
            val plan = pendingCapturePlan
            val found = plan != null
            AppLogger.d(
                plan?.requestId ?: "null",
                "Popup_Cache_Get",
                "found=$found"
            )
            pendingCapturePlan = null
            return plan
        }
    }
}