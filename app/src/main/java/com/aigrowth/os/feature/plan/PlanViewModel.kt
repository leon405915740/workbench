package com.aigrowth.os.feature.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aigrowth.os.core.database.workbench.dao.PlanItemDao
import com.aigrowth.os.core.database.workbench.entity.PlanItem
import com.aigrowth.os.ui.common.todayString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

data class PlanStats(
    val total: Int = 0,
    val done: Int = 0,
    val p0: Int = 0,
    val p1: Int = 0,
    val p2: Int = 0
) {
    val progress: Float get() = if (total == 0) 0f else done.toFloat() / total
}

@HiltViewModel
class PlanViewModel @Inject constructor(
    private val planDao: PlanItemDao
) : ViewModel() {

    private val allItems = planDao.getAll()
    private val _search = MutableStateFlow("")
    val search: StateFlow<String> = _search.asStateFlow()

    val items: StateFlow<List<PlanItem>> = combine(allItems, _search) { list, q ->
        val today = todayString()
        // 今日计划：planDate == 今天（含今天已完成的，仍显示勾选状态；不消失）
        val visible = list.filter { it.planDate == today }
        if (q.isBlank()) visible
        else visible.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.note.contains(q, ignoreCase = true) ||
                it.priority.contains(q, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 历史归档：已经完成且完成时间不在今天的（昨天/更早完成的），按完成时间倒序。 */
    val archived: StateFlow<List<PlanItem>> = allItems.map { list ->
        val today = todayString()
        list.filter { item ->
            item.done && item.completedAt != null &&
                !isCompletedOn(item, today)
        }.sortedByDescending { it.completedAt ?: 0L }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<PlanStats> = allItems.map { list ->
        val today = todayString()
        val todays = list.filter { it.planDate == today }
        PlanStats(
            total = todays.size,
            done = todays.count { isCompletedOn(it, today) },
            p0 = todays.count { it.priority == "P0" },
            p1 = todays.count { it.priority == "P1" },
            p2 = todays.count { it.priority == "P2" }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlanStats())

    fun setSearch(query: String) {
        _search.value = query
    }

    fun add(title: String, priority: String, note: String, planDate: String, imageUri: String? = null) {
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            planDao.insert(
                PlanItem(
                    id = UUID.randomUUID().toString(),
                    title = title.trim(),
                    priority = priority,
                    note = note.trim(),
                    done = false,
                    pinned = false,
                    planDate = planDate,
                    imageUri = imageUri,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    fun update(item: PlanItem) {
        viewModelScope.launch { planDao.update(item.copy(updatedAt = System.currentTimeMillis())) }
    }

    fun toggleDone(item: PlanItem) = update(
        item.copy(done = !item.done, completedAt = if (!item.done) System.currentTimeMillis() else null)
    )

    fun togglePinned(item: PlanItem) = update(item.copy(pinned = !item.pinned))

    private fun isCompletedOn(item: PlanItem, date: String): Boolean {
        val ts = item.completedAt ?: return false
        return Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate().toString() == date
    }

    fun delete(item: PlanItem) {
        viewModelScope.launch { planDao.delete(item) }
    }
}