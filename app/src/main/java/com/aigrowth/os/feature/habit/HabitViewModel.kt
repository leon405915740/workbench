package com.aigrowth.os.feature.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aigrowth.os.core.database.workbench.dao.HabitDao
import com.aigrowth.os.core.database.workbench.dao.HabitLogDao
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
    val total: Int
)

data class HabitStats(
    val total: Int = 0,
    val active: Int = 0,
    val doneToday: Int = 0
) {
    val progress: Float get() = if (active == 0) 0f else doneToday.toFloat() / active
}

@HiltViewModel
class HabitViewModel @Inject constructor(
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao
) : ViewModel() {

    private val allHabits = habitDao.getAll()
    private val allLogs = habitLogDao.getLogsInRange("0000-01-01", "9999-12-31")
    private val _search = MutableStateFlow("")
    val search: StateFlow<String> = _search.asStateFlow()

    private val today = todayString()

    val habits: StateFlow<List<HabitUi>> = combine(allHabits, allLogs, _search) { list, logs, q ->
        val filtered = if (q.isBlank()) list else list.filter { it.title.contains(q, ignoreCase = true) }
        filtered.map { habit ->
            val dates = logs.asSequence().filter { it.habitId == habit.id }.map { it.date }.toSet()
            HabitUi(
                habit = habit,
                checkedToday = today in dates,
                streak = streakOf(dates),
                total = dates.size
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeHabits: StateFlow<List<Habit>> = allHabits
        .map { list -> list.filter { it.active } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<HabitStats> = combine(allHabits, allLogs) { list, logs ->
        val activeIds = list.filter { it.active }.map { it.id }.toSet()
        HabitStats(
            total = list.size,
            active = activeIds.size,
            doneToday = logs.count { it.date == today && it.habitId in activeIds }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HabitStats())

    val logs: StateFlow<List<HabitLog>> = allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearch(query: String) {
        _search.value = query
    }

    fun add(title: String, imageUri: String? = null) {
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
                    updatedAt = now
                )
            )
        }
    }

    fun edit(habit: Habit, newTitle: String, imageUri: String?) {
        viewModelScope.launch {
            habitDao.update(habit.copy(title = newTitle.trim(), imageUri = imageUri, updatedAt = System.currentTimeMillis()))
        }
    }

    fun setActive(habit: Habit, active: Boolean) {
        viewModelScope.launch {
            habitDao.update(habit.copy(active = active, updatedAt = System.currentTimeMillis()))
        }
    }

    fun togglePinned(habit: Habit) {
        viewModelScope.launch {
            habitDao.update(habit.copy(pinned = !habit.pinned, updatedAt = System.currentTimeMillis()))
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

    fun delete(habit: Habit) {
        viewModelScope.launch { habitDao.delete(habit) }
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