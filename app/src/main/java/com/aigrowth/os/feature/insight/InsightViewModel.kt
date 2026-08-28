package com.aigrowth.os.feature.insight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.accounting.app.AccountingApp
import com.accounting.app.AccountingBridge
import com.aigrowth.os.core.database.workbench.dao.*
import com.aigrowth.os.core.database.workbench.entity.*
import com.aigrowth.os.feature.pomodoro.POMODORO_ID
import com.aigrowth.os.ui.common.todayString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class InsightUi(
    val planTotal: Int = 0,
    val planDone: Int = 0,
    val habitActive: Int = 0,
    val habitTotalLogs: Int = 0,
    val avgStreak: Int = 0,
    val readingTotal: Int = 0,
    val readingCompleted: Int = 0,
    val exerciseTotal: Int = 0,
    val exerciseCompleted: Int = 0
) {
    val planProgress: Float get() = if (planTotal == 0) 0f else planDone.toFloat() / planTotal
    val readingProgress: Float get() = if (readingTotal == 0) 0f else readingCompleted.toFloat() / readingTotal
    val exerciseProgress: Float get() = if (exerciseTotal == 0) 0f else exerciseCompleted.toFloat() / exerciseTotal
}

@HiltViewModel
class InsightViewModel @Inject constructor(
    private val planDao: PlanItemDao,
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao,
    private val readingDao: ReadingItemDao,
    private val exerciseDao: ExerciseItemDao,
    private val statusTrendDao: StatusTrendDao,
    private val pomodoroStateDao: PomodoroStateDao
) : ViewModel() {

    private val bridge: AccountingBridge = AccountingApp.getBridge()
    private val today = todayString()

    val insight: StateFlow<InsightUi> = combine(
        planDao.getAll(),
        habitDao.getAll(),
        habitLogDao.getLogsInRange("0000-01-01", "9999-12-31"),
        readingDao.getAll(),
        exerciseDao.getAll()
    ) { plans, habits, logs, reading, exercise ->
        InsightUi(
            planTotal = plans.size,
            planDone = plans.count { it.done },
            habitActive = habits.count { it.active },
            habitTotalLogs = logs.size,
            avgStreak = avgStreak(habits, logs),
            readingTotal = reading.size,
            readingCompleted = reading.count { it.current >= it.target },
            exerciseTotal = exercise.size,
            exerciseCompleted = exercise.count { it.current >= it.target }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InsightUi())

    val pomodoro: StateFlow<PomodoroState?> = pomodoroStateDao.observe(POMODORO_ID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val monthExpense: StateFlow<Double?> = bridge.getMonthlyExpense()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val statusEntries: StateFlow<List<StatusTrendEntry>> = statusTrendDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun upsertStatus(score: Int, note: String?) {
        viewModelScope.launch {
            val existing = statusTrendDao.getByDate(today)
            if (existing == null) {
                statusTrendDao.upsert(
                    StatusTrendEntry(
                        java.util.UUID.randomUUID().toString(),
                        today,
                        score,
                        note,
                        System.currentTimeMillis()
                    )
                )
            } else {
                statusTrendDao.upsert(existing.copy(score = score, note = note, createdAt = System.currentTimeMillis()))
            }
        }
    }

    private fun avgStreak(habits: List<Habit>, logs: List<HabitLog>): Int {
        val active = habits.filter { it.active }
        if (active.isEmpty()) return 0
        val byHabit = logs.groupBy { it.habitId }
        val streaks = active.map { habit ->
            val dates = byHabit[habit.id].orEmpty().map { it.date }.toSet()
            streakOf(dates)
        }
        return (streaks.sum().toDouble() / streaks.size).toInt()
    }

    private fun streakOf(dates: Set<String>): Int {
        var cursor = LocalDate.parse(today)
        if (cursor.toString() !in dates) cursor = cursor.minusDays(1)
        var count = 0
        while (cursor.toString() in dates) {
            count++
            cursor = cursor.minusDays(1)
        }
        return count
    }
}