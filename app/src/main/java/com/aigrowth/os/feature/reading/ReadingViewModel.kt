package com.aigrowth.os.feature.reading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aigrowth.os.core.database.workbench.dao.ReadingItemDao
import com.aigrowth.os.core.database.workbench.entity.ReadingItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ReadingStats(
    val total: Int = 0,
    val completed: Int = 0,
    val currentSum: Float = 0f
)

@HiltViewModel
class ReadingViewModel @Inject constructor(
    private val dao: ReadingItemDao
) : ViewModel() {

    private val all = dao.getAll()
    private val _search = MutableStateFlow("")
    val search: StateFlow<String> = _search.asStateFlow()

    val items: StateFlow<List<ReadingItem>> = combine(all, _search) { list, q ->
        if (q.isBlank()) list
        else list.filter {
            it.title.contains(q, ignoreCase = true) || it.note.contains(q, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<ReadingStats> = all.map { list ->
        ReadingStats(
            total = list.size,
            completed = list.count { it.current >= it.target },
            currentSum = list.sumOf { it.current.toDouble() }.toFloat()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReadingStats())

    fun setSearch(query: String) {
        _search.value = query
    }

    fun add(title: String, current: Float, target: Float, unit: String, note: String, date: String, imageUri: String? = null) {
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            dao.insert(
                ReadingItem(
                    id = UUID.randomUUID().toString(),
                    title = title.trim(),
                    current = current,
                    target = target,
                    unit = unit.trim(),
                    date = date,
                    note = note.trim(),
                    pinned = false,
                    imageUri = imageUri,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    fun update(item: ReadingItem) {
        viewModelScope.launch { dao.update(item.copy(updatedAt = System.currentTimeMillis())) }
    }

    fun increment(item: ReadingItem, step: Float) = update(item.copy(current = maxOf(0f, item.current + step)))

    fun togglePinned(item: ReadingItem) = update(item.copy(pinned = !item.pinned))

    fun delete(item: ReadingItem) {
        viewModelScope.launch { dao.delete(item) }
    }
}