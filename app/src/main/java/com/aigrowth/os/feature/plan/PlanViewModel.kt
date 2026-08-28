package com.aigrowth.os.feature.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aigrowth.os.core.database.workbench.dao.PlanItemDao
import com.aigrowth.os.core.database.workbench.entity.PlanItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
        if (q.isBlank()) list
        else list.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.note.contains(q, ignoreCase = true) ||
                it.priority.contains(q, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<PlanStats> = allItems.map { list ->
        PlanStats(
            total = list.size,
            done = list.count { it.done },
            p0 = list.count { it.priority == "P0" },
            p1 = list.count { it.priority == "P1" },
            p2 = list.count { it.priority == "P2" }
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

    fun toggleDone(item: PlanItem) = update(item.copy(done = !item.done))

    fun togglePinned(item: PlanItem) = update(item.copy(pinned = !item.pinned))

    fun delete(item: PlanItem) {
        viewModelScope.launch { planDao.delete(item) }
    }
}