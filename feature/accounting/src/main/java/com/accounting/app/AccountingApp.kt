package com.accounting.app

import android.content.Context
import com.accounting.app.data.local.database.AppDatabase
import com.accounting.app.domain.classification.CategoryService
import com.accounting.app.log.AppLogger
import com.accounting.app.log.CrashHandler
import com.accounting.app.data.repository.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 记账模块运行时单例。
 * 整合进工作台后不再是 Application 子类，由宿主 Application.onCreate 调用 [init] 完成初始化。
 */
class AccountingApp private constructor(context: Context) {

    private val appContext: Context = context.applicationContext
    val appRepository: AppRepository by lazy { AppRepository(appContext) }

    companion object {
        @Volatile
        private var instance: AccountingApp? = null
        @Volatile
        private var initialized = false

        /** 宿主 Application.onCreate 调用，幂等。 */
        fun init(context: Context) {
            if (initialized) return
            synchronized(this) {
                if (initialized) return
                AppLogger.init(context.applicationContext)
                CrashHandler.init()
                AppLogger.i("", "应用启动", "AccountingApp init")
                val db = AppDatabase.getInstance(context.applicationContext)
                CategoryService.init(db.categoryDao())
                CoroutineScope(Dispatchers.IO).launch {
                    CategoryService.loadCache()
                    AppLogger.d("", "应用启动", "分类缓存加载完成")
                }
                instance = AccountingApp(context.applicationContext)
                initialized = true
            }
        }

        fun getInstance(): AccountingApp {
            return instance ?: throw IllegalStateException("AccountingApp not initialized")
        }

        @Volatile
        private var bridge: AccountingBridge? = null

        fun getBridge(): AccountingBridge {
            return bridge ?: synchronized(this) {
                bridge ?: AccountingBridgeImpl(
                    instance!!.appRepository,
                    instance!!.appContext
                ).also { bridge = it }
            }
        }
    }
}
