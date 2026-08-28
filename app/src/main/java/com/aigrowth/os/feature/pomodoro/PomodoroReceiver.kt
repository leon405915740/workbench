package com.aigrowth.os.feature.pomodoro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PomodoroReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "番茄钟时间到"
        PomodoroAlarms.show(context, "番茄钟", message)
    }
}