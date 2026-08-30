package com.aigrowth.os.feature.habit

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aigrowth.os.core.database.workbench.dao.HabitDao
import com.aigrowth.os.core.database.workbench.dao.HabitLogDao
import com.aigrowth.os.core.database.workbench.entity.ExerciseCategoryEnum
import com.aigrowth.os.core.database.workbench.entity.Habit
import com.aigrowth.os.core.database.workbench.entity.HabitLog
import com.aigrowth.os.ui.common.todayString
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ExerciseTimerState(
    val habitId: String,
    val running: Boolean,
    val startedAt: Long?
)

/**
 * 独立运动计时器 ViewModel：不依赖具体习惯卡片，首页「记运动」直接进入。
 * 计时结束自动累加时长到当日 HabitLog，并弹出编辑备注/类别/时长的面板。
 */
@HiltViewModel
class ExerciseTimerViewModel @Inject constructor(
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val allHabits = habitDao.getAllOrderedByPinnedAndRecent()
    val habits: StateFlow<List<Habit>> = allHabits
        .map { list -> list.filter { it.active } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedHabitId = MutableStateFlow<String?>(null)
    val selectedHabitId: StateFlow<String?> = _selectedHabitId.asStateFlow()

    private val _timerState = MutableStateFlow<ExerciseTimerState?>(null)
    private val _tick = MutableStateFlow(System.currentTimeMillis())

    val elapsedSeconds: StateFlow<Long> = combine(_timerState, _tick) { state, now ->
        val s = state ?: return@combine 0L
        if (!s.running || s.startedAt == null) return@combine 0L
        ((now - s.startedAt) / 1000).coerceAtLeast(0L)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val isRunning: StateFlow<Boolean> = _timerState.map { it?.running == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _editRequest = MutableStateFlow<LogEditRequest?>(null)
    val editRequest: StateFlow<LogEditRequest?> = _editRequest.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    init {
        val running = ExerciseTimerStore.runningTimers(context)
        val first = running.entries.firstOrNull()
        if (first != null) {
            val (id, startedAt) = first
            _selectedHabitId.value = id
            _timerState.value = ExerciseTimerState(id, true, startedAt)
        }

        viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val now = System.currentTimeMillis()
                _tick.value = now
                val state = _timerState.value
                if (state?.running == true && state.startedAt != null) {
                    val title = habits.value.firstOrNull { it.id == state.habitId }?.title
                    ExerciseTimerNotifier.showRunning(
                        context,
                        state.habitId,
                        title,
                        ((now - state.startedAt) / 1000).coerceAtLeast(0L)
                    )
                }
            }
        }
    }

    fun selectHabit(habitId: String) {
        if (_timerState.value?.running == true) return
        _selectedHabitId.value = habitId
    }

    fun start() {
        if (_timerState.value?.running == true) return
        viewModelScope.launch {
            val habit = ensureExerciseHabit()
            if (habit == null) {
                _toast.value = "请先创建一个习惯"
                return@launch
            }
            _selectedHabitId.value = habit.id
            val now = System.currentTimeMillis()
            ExerciseTimerStore.setRunning(context, habit.id, true, now)
            _timerState.value = ExerciseTimerState(habit.id, true, now)
            _tick.value = now
            ExerciseTimerNotifier.showRunning(context, habit.id, habit.title, 0L)
        }
    }

    fun end() {
        val state = _timerState.value ?: return
        if (!state.running) return
        val startedAt = state.startedAt ?: return
        ExerciseTimerStore.clear(context, state.habitId)
        _timerState.value = ExerciseTimerState(state.habitId, false, null)
        ExerciseTimerNotifier.cancel(context, state.habitId)
        val minutes = (((System.currentTimeMillis() - startedAt) / 1000) / 60).toInt()
        if (minutes <= 0) {
            _toast.value = "计时不足 1 分钟，未计入"
            return
        }
        val date = todayString()
        viewModelScope.launch {
            accumulateMinutes(state.habitId, date, minutes)
            val log = habitLogDao.getByHabitAndDate(state.habitId, date)
            _editRequest.value = LogEditRequest(
                state.habitId,
                date,
                log?.durationMinutes,
                log?.note,
                log?.category
            )
        }
    }

    fun cancel() {
        val state = _timerState.value ?: return
        ExerciseTimerStore.clear(context, state.habitId)
        _timerState.value = null
        ExerciseTimerNotifier.cancel(context, state.habitId)
        _toast.value = "已取消计时，本次时长未保存"
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

    fun consumeToast() {
        _toast.value = null
    }

    /** 首页「记运动」的数据固定归到标题为「运动」的习惯，避免散落到其它生活习惯。 */
    private suspend fun ensureExerciseHabit(): Habit? {
        habits.value.firstOrNull { it.title == "运动" }?.let { return it }

        val now = System.currentTimeMillis()
        val habit = Habit(
            id = UUID.randomUUID().toString(),
            title = "运动",
            active = true,
            pinned = false,
            imageUri = null,
            createdAt = now,
            updatedAt = now,
            category = ExerciseCategoryEnum.OTHER,
            pinnedAt = null
        )
        habitDao.insert(habit)
        return habit
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
}
