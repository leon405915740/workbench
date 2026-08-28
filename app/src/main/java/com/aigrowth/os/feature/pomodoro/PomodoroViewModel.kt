package com.aigrowth.os.feature.pomodoro

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aigrowth.os.core.database.workbench.dao.PomodoroStateDao
import com.aigrowth.os.core.database.workbench.entity.PomodoroState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

const val POMODORO_ID = "default"
const val FOCUS_SECONDS = 25 * 60
const val BREAK_SECONDS = 5 * 60

enum class PomodoroPhase { IDLE, FOCUS, BREAK }

data class PomodoroUi(
    val phase: PomodoroPhase = PomodoroPhase.IDLE,
    val running: Boolean = false,
    val remainSeconds: Int = FOCUS_SECONDS,
    val totalSeconds: Int = FOCUS_SECONDS,
    val focusCount: Int = 0,
    val totalFocusMinutes: Int = 0
) {
    val progress: Float get() = if (totalSeconds == 0) 0f else 1f - remainSeconds.toFloat() / totalSeconds
}

@HiltViewModel
class PomodoroViewModel @Inject constructor(
    private val dao: PomodoroStateDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _tick = MutableStateFlow(0L)
    private val dbState = dao.observe(POMODORO_ID)

    val ui: StateFlow<PomodoroUi> = combine(dbState, _tick) { db, now ->
        buildUi(db, now)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PomodoroUi())

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val now = System.currentTimeMillis()
                _tick.value = now
                completeIfDue(now)
            }
        }
    }

    private fun buildUi(db: PomodoroState?, now: Long): PomodoroUi {
        if (db == null) return PomodoroUi()
        val phase = phaseOf(db.totalSeconds)
        if (!db.running) {
            return PomodoroUi(phase, false, db.remainSeconds, db.totalSeconds, db.focusCount, db.totalFocusMinutes)
        }
        val started = db.startedAt ?: now
        val remain = (db.remainSeconds - ((now - started) / 1000).toInt()).coerceAtLeast(0)
        return PomodoroUi(phase, true, remain, db.totalSeconds, db.focusCount, db.totalFocusMinutes)
    }

    private fun phaseOf(totalSeconds: Int): PomodoroPhase = when {
        totalSeconds >= FOCUS_SECONDS -> PomodoroPhase.FOCUS
        totalSeconds == BREAK_SECONDS -> PomodoroPhase.BREAK
        else -> PomodoroPhase.IDLE
    }

    fun startFocus() {
        viewModelScope.launch {
            val cur = dao.get(POMODORO_ID) ?: PomodoroState(POMODORO_ID, false, 0, FOCUS_SECONDS, null, 0, 0)
            val now = System.currentTimeMillis()
            dao.upsert(cur.copy(running = true, remainSeconds = FOCUS_SECONDS, totalSeconds = FOCUS_SECONDS, startedAt = now))
            PomodoroAlarms.schedule(context, FOCUS_SECONDS, "专注完成，休息一下吧")
        }
    }

    fun pause() {
        viewModelScope.launch {
            val cur = dao.get(POMODORO_ID) ?: return@launch
            val remain = remainAt(cur, System.currentTimeMillis())
            dao.upsert(cur.copy(running = false, remainSeconds = remain, startedAt = null))
            PomodoroAlarms.cancel(context)
        }
    }

    fun resume() {
        viewModelScope.launch {
            val cur = dao.get(POMODORO_ID) ?: return@launch
            val now = System.currentTimeMillis()
            dao.upsert(cur.copy(running = true, startedAt = now))
            val message = if (phaseOf(cur.totalSeconds) == PomodoroPhase.BREAK) "休息结束，开始下一轮吧" else "专注完成，休息一下吧"
            PomodoroAlarms.schedule(context, cur.remainSeconds, message)
        }
    }

    fun reset() {
        viewModelScope.launch {
            val cur = dao.get(POMODORO_ID) ?: PomodoroState(POMODORO_ID, false, 0, FOCUS_SECONDS, null, 0, 0)
            dao.upsert(cur.copy(running = false, remainSeconds = FOCUS_SECONDS, totalSeconds = FOCUS_SECONDS, startedAt = null))
            PomodoroAlarms.cancel(context)
        }
    }

    private suspend fun completeIfDue(now: Long) {
        val db = dao.get(POMODORO_ID) ?: return
        if (!db.running) return
        val started = db.startedAt ?: return
        val elapsed = ((now - started) / 1000).toInt()
        if (elapsed < db.remainSeconds) return
        onComplete(db, now)
    }

    private suspend fun onComplete(db: PomodoroState, now: Long) {
        val isFocus = db.totalSeconds >= FOCUS_SECONDS
        if (isFocus) {
            dao.upsert(
                db.copy(
                    running = true,
                    totalSeconds = BREAK_SECONDS,
                    remainSeconds = BREAK_SECONDS,
                    startedAt = now,
                    focusCount = db.focusCount + 1,
                    totalFocusMinutes = db.totalFocusMinutes + FOCUS_SECONDS / 60
                )
            )
            PomodoroAlarms.schedule(context, BREAK_SECONDS, "休息结束，开始下一轮吧")
            PomodoroAlarms.show(context, "专注完成", "休息 5 分钟吧")
        } else {
            dao.upsert(
                db.copy(
                    running = false,
                    totalSeconds = FOCUS_SECONDS,
                    remainSeconds = FOCUS_SECONDS,
                    startedAt = null
                )
            )
            PomodoroAlarms.cancel(context)
            PomodoroAlarms.show(context, "休息结束", "开始下一轮专注吧")
        }
    }

    private fun remainAt(db: PomodoroState, now: Long): Int {
        val started = db.startedAt ?: return db.remainSeconds
        return (db.remainSeconds - ((now - started) / 1000).toInt()).coerceAtLeast(0)
    }
}