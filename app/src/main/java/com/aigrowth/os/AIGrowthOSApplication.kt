package com.aigrowth.os

import android.app.Application
import com.accounting.app.AccountingApp
import com.aigrowth.os.core.database.workbench.WorkbenchDatabaseInitializer
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AIGrowthOSApplication : Application() {
    @Inject
    lateinit var workbenchInitializer: WorkbenchDatabaseInitializer

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        AccountingApp.init(this)
        applicationScope.launch {
            workbenchInitializer.initializeIfNeeded()
        }
    }
}
