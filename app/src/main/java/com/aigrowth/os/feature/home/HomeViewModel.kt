package com.aigrowth.os.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.accounting.app.AccountingApp
import com.accounting.app.AccountingBridge
import com.aigrowth.os.core.database.workbench.dao.*
import com.aigrowth.os.core.database.workbench.entity.*
import com.aigrowth.os.ui.common.formatProgress
import com.aigrowth.os.ui.common.todayString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class FocusItem(
    val key: String,
    val module: String,
    val title: String,
    val subtitle: String
)

data class OverviewUi(
    val planTotal: Int = 0,
    val planDone: Int = 0,
    val habitActive: Int = 0,
    val habitDoneToday: Int = 0,
    val readingTotal: Int = 0,
    val readingCompleted: Int = 0,
    val exerciseTotal: Int = 0,
    val exerciseCompleted: Int = 0,
    val keyTodos: List<PlanItem> = emptyList(),
    val activeHabits: List<Habit> = emptyList(),
    val habitChecked: Set<String> = emptySet()
) {
    val planProgress: Float get() = if (planTotal == 0) 0f else planDone.toFloat() / planTotal
    val habitProgress: Float get() = if (habitActive == 0) 0f else habitDoneToday.toFloat() / habitActive
    val readingProgress: Float get() = if (readingTotal == 0) 0f else readingCompleted.toFloat() / readingTotal
    val exerciseProgress: Float get() = if (exerciseTotal == 0) 0f else exerciseCompleted.toFloat() / exerciseTotal
    val overall: Float get() = (planProgress + habitProgress + readingProgress + exerciseProgress) / 4f
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val planDao: PlanItemDao,
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao,
    private val readingDao: ReadingItemDao,
    private val exerciseDao: ExerciseItemDao,
    private val essayDao: EssayDao,
    private val clippingDao: ClippingDao,
    private val statusTrendDao: StatusTrendDao
) : ViewModel() {

    private val bridge: AccountingBridge = AccountingApp.getBridge()
    private val today = todayString()

    private val plans = planDao.getAll()
    private val habits = habitDao.getAll()
    private val logs = habitLogDao.getLogsInRange("0000-01-01", "9999-12-31")
    private val reading = readingDao.getAll()
    private val exercise = exerciseDao.getAll()
    private val essays = essayDao.getAll()
    private val clippings = clippingDao.getAll()

    val overview: StateFlow<OverviewUi> = combine(plans, habits, logs, reading, exercise) { p, h, l, r, e ->
        val activeIds = h.filter { it.active }.map { it.id }.toSet()
        val todayPlans = p.filter { it.planDate == today }
        val priorityOrder = mapOf("P0" to 0, "P1" to 1, "P2" to 2)
        OverviewUi(
            planTotal = todayPlans.size,
            planDone = todayPlans.count { it.done },
            habitActive = activeIds.size,
            habitDoneToday = l.count { it.date == today && it.habitId in activeIds },
            readingTotal = r.size,
            readingCompleted = r.count { it.current >= it.target },
            exerciseTotal = e.size,
            exerciseCompleted = e.count { it.current >= it.target },
            keyTodos = todayPlans.filter { !it.done }.sortedBy { priorityOrder[it.priority] ?: 3 }.take(3),
            activeHabits = h.filter { it.active },
            habitChecked = l.map { "${it.habitId}|${it.date}" }.toSet()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OverviewUi())

    val focus: StateFlow<List<FocusItem>> = combine(plans, habits, reading, exercise, combine(essays, clippings) { e, c -> e to c }) { p, h, r, e, ec ->
        buildFocus(p, h, r, e, ec.first, ec.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val statusEntries: StateFlow<List<StatusTrendEntry>> = statusTrendDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthExpense: StateFlow<Double?> = bridge.getMonthlyExpense()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun upsertStatus(score: Int, note: String?) {
        viewModelScope.launch {
            val existing = statusTrendDao.getByDate(today)
            if (existing == null) {
                statusTrendDao.upsert(StatusTrendEntry(UUID.randomUUID().toString(), today, score, note, System.currentTimeMillis()))
            } else {
                statusTrendDao.upsert(existing.copy(score = score, note = note, createdAt = System.currentTimeMillis()))
            }
        }
    }

    fun toggleCheck(habitId: String, date: String) {
        viewModelScope.launch {
            if (habitLogDao.getByHabitAndDate(habitId, date) == null) {
                habitLogDao.insert(HabitLog(habitId, date, System.currentTimeMillis()))
            } else {
                habitLogDao.deleteByHabitAndDate(habitId, date)
            }
        }
    }

    private fun buildFocus(
        plans: List<PlanItem>,
        habits: List<Habit>,
        reading: List<ReadingItem>,
        exercise: List<ExerciseItem>,
        essays: List<Essay>,
        clippings: List<Clipping>
    ): List<FocusItem> {
        val result = mutableListOf<FocusItem>()
        plans.filter { it.pinned }.forEach {
            result += FocusItem("plan:${it.id}", "今日计划", it.title, it.priority)
        }
        habits.filter { it.pinned && it.active }.forEach {
            result += FocusItem("habit:${it.id}", "习惯打卡", it.title, "今日打卡")
        }
        reading.filter { it.pinned }.forEach {
            result += FocusItem("reading:${it.id}", "阅读", it.title, "进度 ${formatProgress(it.current)} ${it.unit}")
        }
        exercise.filter { it.pinned }.forEach {
            result += FocusItem("exercise:${it.id}", "运动", it.title, "进度 ${formatProgress(it.current)} ${it.unit}")
        }
        essays.filter { it.pinned }.forEach {
            result += FocusItem("essay:${it.id}", "随笔", it.title, it.type)
        }
        clippings.filter { it.pinned }.forEach {
            result += FocusItem("clipping:${it.id}", "剪报", it.title, it.status ?: "收藏")
        }
        return result
    }
}