package com.aigrowth.os

import android.app.Application
import com.accounting.app.AccountingApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AIGrowthOSApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AccountingApp.init(this)
    }
}
