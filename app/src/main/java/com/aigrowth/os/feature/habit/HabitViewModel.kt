package com.aigrowth.os.feature.habit

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aigrowth.os.core.database.workbench.dao.HabitDao
import com.aigrowth.os.core.database.workbench.dao.HabitLogDao
import com.aigrowth.os.core.database.workbench.entity.ExerciseCategoryEnum
import com.aigrowth.os.core.database.workbench.entity.Habit
import com.aigrowth.os.core.database.workbench.entity.HabitLog
import com.aigrowth.os.ui.common.todayString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class HabitUi(
    val habit: Habit,
    val checkedToday: Boolean,
    val streak: Int,
    val total: Int,
    val todayMinutes: Int? = null
)

data class HabitStats(
    val total: Int = 0,
    val active: Int = 0,
    val doneToday: Int = 0
) {
    val progress: Float get() = if (active == 0) 0f else doneToday.toFloat() / active
}

/** 运动计时运行中的 UI 状态（按 habitId 一张）。 */
data class HabitTimerUi(
    val habitId: String,
    val running: Boolean,
    val startedAt: Long?,
    val elapsedSeconds: Long
)

/** 运动计时内部状态。 */
data class HabitTimerState(
    val habitId: String,
    val running: Boolean,
    val startedAt: Long?
)

/** 重启后待恢复的计时。 */
data class RecoverableTimer(
    val habitId: String,
    val startedAt: Long,
    val elapsedSeconds: Long
)

/** 补录/记录编辑弹窗的数据载体；habitId 为空表示需要先选习惯（路由带日期自动补录）。 */
data class LogEditRequest(
    val habitId: String?,
    val date: String,
    val durationMinutes: Int?,
    val note: String?,
    val category: ExerciseCategoryEnum?
)

@HiltViewModel
class HabitViewModel @Inject constructor(
    private val app: Application,
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao
) : ViewModel() {

    private val allHabits = habitDao.getAllOrderedByPinnedAndRecent()
    private val allLogs = habitLogDao.getLogsInRange("0000-01-01", "9999-12-31")
    private val _search = MutableStateFlow("")
    val search: StateFlow<String> = _search.asStateFlow()

    private val _editRequest = MutableStateFlow<LogEditRequest?>(null)
    val editRequest: StateFlow<LogEditRequest?> = _editRequest.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    private val _timerStates = MutableStateFlow<Map<String, HabitTimerState>>(emptyMap())
    private val _tick = MutableStateFlow(0L)
    private val _recovery = MutableStateFlow<List<RecoverableTimer>>(emptyList())
    val recovery: StateFlow<List<RecoverableTimer>> = _recovery.asStateFlow()

    private var cachedHabits: List<Habit> = emptyList()

    val timelines: StateFlow<List<HabitUi>> = combine(allHabits, allLogs, _search) { list, logs, q ->
        cachedHabits = list
        val today = todayString()
        val filtered = if (q.isBlank()) list else list.filter { it.title.contains(q, ignoreCase = true) }
        filtered.map { habit ->
            val mine = logs.filter { it.habitId == habit.id }
            val dates = mine.map { it.date }.toSet()
            HabitUi(
                habit = habit,
                checkedToday = today in dates,
                streak = streakOf(dates, today),
                total = dates.size,
                todayMinutes = mine.firstOrNull { it.date == today }?.durationMinutes
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habits: StateFlow<List<HabitUi>> = timelines

    val activeHabits: StateFlow<List<Habit>> = allHabits
        .map { list -> list.filter { it.active } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<HabitStats> = combine(allHabits, allLogs) { list, logs ->
        val today = todayString()
        val activeIds = list.filter { it.active }.map { it.id }.toSet()
        HabitStats(
            total = list.size,
            active = activeIds.size,
            doneToday = logs.count { it.date == today && it.habitId in activeIds }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HabitStats())

    val logs: StateFlow<List<HabitLog>> = allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val timers: StateFlow<Map<String, HabitTimerUi>> = combine(_timerStates, _tick) { states, _ ->
        val now = System.currentTimeMillis()
        states.mapValues { (id, s) ->
            HabitTimerUi(
                habitId = id,
                running = s.running,
                startedAt = s.startedAt,
                elapsedSeconds = if (s.running && s.startedAt != null) (now - s.startedAt) / 1000 else 0L
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        viewModelScope.launch {
            val timings = ExerciseTimerStore.runningTimers(app)
            if (timings.isNotEmpty()) {
                _recovery.value = timings.map { (id, startedAt) ->
                    RecoverableTimer(id, startedAt, (System.currentTimeMillis() - startedAt) / 1000)
                }
            }
        }
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                _tick.value = System.currentTimeMillis()
            }
        }
    }

    fun setSearch(query: String) {
        _search.value = query
    }

    fun consumeToast() {
        _toast.value = null
    }

    fun add(title: String, imageUri: String?, category: ExerciseCategoryEnum?) {
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            habitDao.insert(
                Habit(
                    id = UUID.randomUUID().toString(),
                    title = title.trim(),
                    active = true,
                    pinned = false,
                    imageUri = imageUri,
                    createdAt = now,
                    updatedAt = now,
                    category = category,
                    pinnedAt = null
                )
            )
        }
    }

    fun edit(habit: Habit, newTitle: String, imageUri: String?, category: ExerciseCategoryEnum?) {
        viewModelScope.launch {
            habitDao.update(habit.copy(title = newTitle.trim(), imageUri = imageUri, category = category, updatedAt = System.currentTimeMillis()))
        }
    }

    fun setActive(habit: Habit, active: Boolean) {
        viewModelScope.launch {
            habitDao.update(habit.copy(active = active, updatedAt = System.currentTimeMillis()))
        }
    }

    fun togglePinned(habit: Habit) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val pinned = habit.pinnedAt == null
            habitDao.update(habit.copy(pinned = pinned, pinnedAt = if (pinned) now else null, updatedAt = now))
        }
    }

    /**
     * 打卡框点击：无记录 -> 打卡（类别继承 Habit.category）；
     * 无时长记录再点 -> 取消打卡；已有时长 -> 进编辑确认，不直接取消。
     */
    fun onCheckClick(habitId: String, date: String) {
        viewModelScope.launch {
            val existing = habitLogDao.getByHabitAndDate(habitId, date)
            when {
                existing == null -> {
                    habitLogDao.insert(
                        HabitLog(habitId, date, System.currentTimeMillis(), null, null, habitDao.getById(habitId)?.category)
                    )
                }
                (existing.durationMinutes ?: 0) <= 0 -> {
                    habitLogDao.deleteByHabitAndDate(habitId, date)
                }
                else -> {
                    _editRequest.value = LogEditRequest(habitId, date, existing.durationMinutes, existing.note, existing.category)
                }
            }
        }
    }

    fun delete(habit: Habit) {
        viewModelScope.launch { habitDao.delete(habit) }
    }

    // ---- 运动计时（正计时，按 habitId 独立持久化）----

    fun startTimer(habitId: String) {
        val now = System.currentTimeMillis()
        ExerciseTimerStore.setRunning(app, habitId, true, now)
        _timerStates.update { it + (habitId to HabitTimerState(habitId, true, now)) }
        _tick.value = now
        ExerciseTimerNotifier.showRunning(app, habitId, cachedHabits.firstOrNull { it.id == habitId }?.title, 0L)
    }

    fun endTimer(habitId: String) {
        val state = _timerStates.value[habitId] ?: return
        if (!state.running) return
        val startedAt = state.startedAt ?: return
        ExerciseTimerStore.clear(app, habitId)
        _timerStates.update { it - habitId }
        ExerciseTimerNotifier.cancel(app, habitId)
        val minutes = (((System.currentTimeMillis() - startedAt) / 1000) / 60).toInt()
        if (minutes <= 0) {
            _toast.value = "计时不足 1 分钟，未计入"
            return
        }
        val date = todayString()
        viewModelScope.launch {
            accumulateMinutes(habitId, date, minutes)
            val log = habitLogDao.getByHabitAndDate(habitId, date)
            _editRequest.value = LogEditRequest(habitId, date, log?.durationMinutes, log?.note, log?.category)
        }
    }

    fun cancelTimer(habitId: String) {
        ExerciseTimerStore.clear(app, habitId)
        _timerStates.update { it - habitId }
        ExerciseTimerNotifier.cancel(app, habitId)
        _toast.value = "已取消计时，本次时长未保存"
    }

    fun openManual(habitId: String, date: String = todayString()) {
        viewModelScope.launch {
            val log = habitLogDao.getByHabitAndDate(habitId, date)
            _editRequest.value = LogEditRequest(
                habitId, date,
                log?.durationMinutes, log?.note,
                log?.category ?: habitDao.getById(habitId)?.category
            )
        }
    }

    /** 路由 ?date= 自动带出该日期的补录弹窗；未来日期拒绝。 */
    fun openMakeup(date: String) {
        val parsed = runCatching { LocalDate.parse(date) }.getOrNull()
        if (parsed == null || parsed.isAfter(LocalDate.now())) {
            _toast.value = "补录日期无效"
            return
        }
        _editRequest.value = LogEditRequest(null, date, null, null, null)
    }

    fun dismissEdit() {
        _editRequest.value = null
    }

    fun saveLog(habitId: String, date: String, durationMinutes: Int?, note: String?, category: ExerciseCategoryEnum?) {
        viewModelScope.launch {
            val existing = habitLogDao.getByHabitAndDate(habitId, date)
            val resolved = category ?: habitDao.getById(habitId)?.category ?: existing?.category
            habitLogDao.insert(
                HabitLog(
                    habitId = habitId,
                    date = date,
                    checkedAt = existing?.checkedAt ?: System.currentTimeMillis(),
                    durationMinutes = durationMinutes,
                    note = note?.trim()?.takeIf { it.isNotEmpty() },
                    category = resolved
                )
            )
            dismissEdit()
        }
    }

    fun deleteLog(habitId: String, date: String) {
        viewModelScope.launch {
            habitLogDao.deleteByHabitAndDate(habitId, date)
            dismissEdit()
        }
    }

    // ---- 重启恢复提示 ----

    fun dismissRecovery() {
        _recovery.value = emptyList()
    }

    fun abandonRecovery(habitId: String) {
        cancelTimer(habitId)
        _recovery.value = _recovery.value.filter { it.habitId != habitId }
    }

    fun abandonAllRecovery() {
        _recovery.value.forEach { t ->
            ExerciseTimerStore.clear(app, t.habitId)
            ExerciseTimerNotifier.cancel(app, t.habitId)
        }
        _timerStates.value = emptyMap()
        _recovery.value = emptyList()
    }

    private suspend fun accumulateMinutes(habitId: String, date: String, minutes: Int) {
        if (habitLogDao.getByHabitAndDate(habitId, date) == null) {
            habitLogDao.insert(
                HabitLog(habitId, date, System.currentTimeMillis(), minutes, null, habitDao.getById(habitId)?.category)
            )
        } else {
            habitLogDao.addDuration(habitId, date, minutes)
        }
    }

    private fun streakOf(dates: Set<String>, today: String): Int {
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
