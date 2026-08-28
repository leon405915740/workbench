package com.aigrowth.os.feature.essay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aigrowth.os.core.database.workbench.dao.EssayDao
import com.aigrowth.os.core.database.workbench.entity.Essay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EssayViewModel @Inject constructor(
    private val dao: EssayDao
) : ViewModel() {

    private val all = dao.getAll()
    private val _search = MutableStateFlow("")
    val search: StateFlow<String> = _search.asStateFlow()

    val items: StateFlow<List<Essay>> = combine(all, _search) { list, q ->
        if (q.isBlank()) list
        else list.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.content.contains(q, ignoreCase = true) ||
                it.tags.contains(q, ignoreCase = true) ||
                (it.mood?.contains(q, ignoreCase = true) == true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearch(query: String) {
        _search.value = query
    }

    fun add(title: String, content: String, mood: String?, type: String, tags: String, layout: String, date: String, imageUri: String? = null) {
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            dao.insert(
                Essay(
                    id = UUID.randomUUID().toString(),
                    title = title.trim(),
                    content = content.trim(),
                    mood = mood,
                    type = type,
                    tags = tags.trim(),
                    layout = layout,
                    date = date,
                    pinned = false,
                    imageUri = imageUri,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    fun update(item: Essay) {
        viewModelScope.launch { dao.update(item.copy(updatedAt = System.currentTimeMillis())) }
    }

    fun togglePinned(item: Essay) = update(item.copy(pinned = !item.pinned))

    fun delete(item: Essay) {
        viewModelScope.launch { dao.delete(item) }
    }
}