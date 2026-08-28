package com.aigrowth.os.core.database.workbench

import android.content.Context
import androidx.room.withTransaction
import com.aigrowth.os.core.database.workbench.entity.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * workbench.db 初始化器：首次启动播种非财务演示数据，仅播种一次。
 * 记账模块、状态趋势（真实填写）与番茄钟统计不在播种范围。
 */
@Singleton
class WorkbenchDatabaseInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: WorkbenchDatabase
) {
    private val prefsName = "workbench_db_init"
    private val keySeeded = "seeded"

    suspend fun initializeIfNeeded() {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        if (prefs.getBoolean(keySeeded, false)) return
        withContext(Dispatchers.IO) {
            database.withTransaction { seed() }
        }
        prefs.edit().putBoolean(keySeeded, true).apply()
    }

    private suspend fun seed() {
        val now = System.currentTimeMillis()
        val today = LocalDate.now().toString()

        listOf(
            PlanItem(UUID.randomUUID().toString(), "确定今日最重要的一件事", "P0", "先做最重要的事", false, false, today, null, now, now),
            PlanItem(UUID.randomUUID().toString(), "整理桌面并给房间通风", "P1", "", false, false, today, null, now, now),
            PlanItem(UUID.randomUUID().toString(), "晚饭后整理今日小票并记账", "P2", "", true, false, today, null, now, now)
        ).forEach { database.planItemDao().insert(it) }

        listOf("喝够 8 杯水", "23:30 前睡觉", "散步 10 分钟").forEach { title ->
            database.habitDao().insert(
                Habit(UUID.randomUUID().toString(), title, true, false, null, now, now)
            )
        }

        listOf(
            ReadingItem(UUID.randomUUID().toString(), "《认知觉醒》", 168f, 300f, "页", today, "第 7 章：习惯的复利，早晚各读 30 分钟", false, null, now, now),
            ReadingItem(UUID.randomUUID().toString(), "《原子习惯》", 90f, 260f, "页", today, "聚焦身份认同的养成，做好读书笔记", false, null, now, now)
        ).forEach { database.readingItemDao().insert(it) }

        listOf(
            ExerciseItem(UUID.randomUUID().toString(), "力量训练", 12f, 20f, "分钟", today, "核心 + 上肢，组间休息 60 秒", false, null, now, now),
            ExerciseItem(UUID.randomUUID().toString(), "跑步", 30f, 40f, "分钟", today, "慢跑热身，配速 6 分半保持心率", false, null, now, now)
        ).forEach { database.exerciseItemDao().insert(it) }

        database.essayDao().insert(
            Essay(UUID.randomUUID().toString(), "今天的小确幸", "傍晚的阳光落在窗边，暖暖的，安静得刚好。", "开心", "note", "", "default", today, false, null, now, now)
        )

        database.clippingDao().insert(
            Clipping(UUID.randomUUID().toString(), "AI 提示词技巧合集", "整理常用提示词模板，方便复用。", "收藏", null, "", "default", today, false, null, now, now)
        )
    }
}