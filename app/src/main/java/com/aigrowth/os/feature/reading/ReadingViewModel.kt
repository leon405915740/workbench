package com.aigrowth.os.feature.reading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aigrowth.os.core.database.workbench.dao.ReadingItemDao
import com.aigrowth.os.core.database.workbench.dao.ReadingLogDao
import com.aigrowth.os.core.database.workbench.entity.ReadingItem
import com.aigrowth.os.core.database.workbench.entity.ReadingLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class ReadingStats(
    val total: Int = 0,
    val completed: Int = 0,
    val currentSum: Float = 0f
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReadingViewModel @Inject constructor(
    private val dao: ReadingItemDao,
    private val readingLogDao: ReadingLogDao
) : ViewModel() {

    private val all = dao.getAll()
    private val _search = MutableStateFlow("")
    val search: StateFlow<String> = _search.asStateFlow()

    val items: StateFlow<List<ReadingItem>> = combine(all, _search) { list, q ->
        // 进行中：未读完（current < target）
        val visible = list.filter { it.current < it.target }
        if (q.isBlank()) visible
        else visible.filter {
            it.title.contains(q, ignoreCase = true) || it.note.contains(q, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 历史归档：已读完（current >= target）的书籍，按创建时间倒序显示历史。 */
    val archived: StateFlow<List<ReadingItem>> = all.map { list ->
        list.filter { it.current >= it.target }
            .sortedByDescending { it.updatedAt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<ReadingStats> = all.map { list ->
        ReadingStats(
            total = list.size,
            completed = list.count { it.current >= it.target },
            currentSum = list.sumOf { it.current.toDouble() }.toFloat()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReadingStats())

    val readingToday: StateFlow<Float> = all.mapLatest {
        readingLogDao.sumAmountOn(todayString())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

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

    fun increment(item: ReadingItem, step: Float) {
        if (item.current >= item.target) return // 已完成，不再累加
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            val next = minOf(item.target, item.current + step) // 封顶不超目标
            val added = next - item.current
            if (added > 0f) {
                readingLogDao.insert(
                    ReadingLog(
                        id = UUID.randomUUID().toString(),
                        readingItemId = item.id,
                        date = todayString(),
                        amount = added,
                        createdAt = now
                    )
                )
                dao.update(item.copy(current = next, updatedAt = now))
            }
        }
    }

    fun deleteLog(log: ReadingLog) {
        viewModelScope.launch {
            readingLogDao.delete(log)
            val item = dao.getById(log.readingItemId)
            if (item != null) {
                dao.update(item.copy(current = maxOf(0f, item.current - log.amount), updatedAt = System.currentTimeMillis()))
            }
        }
    }

    fun togglePinned(item: ReadingItem) = update(item.copy(pinned = !item.pinned))

    fun delete(item: ReadingItem) {
        viewModelScope.launch { dao.delete(item) }
    }

    private fun todayString(): String = LocalDate.now().toString()
}
