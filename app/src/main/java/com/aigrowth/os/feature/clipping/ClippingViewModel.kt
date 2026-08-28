package com.aigrowth.os.feature.clipping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aigrowth.os.core.database.workbench.dao.ClippingDao
import com.aigrowth.os.core.database.workbench.entity.Clipping
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ClippingViewModel @Inject constructor(
    private val dao: ClippingDao
) : ViewModel() {

    private val all = dao.getAll()
    private val _search = MutableStateFlow("")
    val search: StateFlow<String> = _search.asStateFlow()

    val items: StateFlow<List<Clipping>> = combine(all, _search) { list, q ->
        if (q.isBlank()) list
        else list.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.content.contains(q, ignoreCase = true) ||
                it.tags.contains(q, ignoreCase = true) ||
                (it.source?.contains(q, ignoreCase = true) == true) ||
                (it.status?.contains(q, ignoreCase = true) == true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearch(query: String) {
        _search.value = query
    }

    fun add(title: String, content: String, status: String?, source: String?, tags: String, layout: String, date: String, imageUri: String? = null) {
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            dao.insert(
                Clipping(
                    id = UUID.randomUUID().toString(),
                    title = title.trim(),
                    content = content.trim(),
                    status = status,
                    source = source?.trim(),
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

    fun update(item: Clipping) {
        viewModelScope.launch { dao.update(item.copy(updatedAt = System.currentTimeMillis())) }
    }

    fun togglePinned(item: Clipping) = update(item.copy(pinned = !item.pinned))

    fun delete(item: Clipping) {
        viewModelScope.launch { dao.delete(item) }
    }
}