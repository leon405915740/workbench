package com.aigrowth.os.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.accounting.app.AccountingApp
import com.accounting.app.AccountingBridge
import com.accounting.app.log.AppLogger
import com.accounting.app.ui.model.MappingItemUi
import com.accounting.app.ui.model.MemoryItemUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MemoryMappingViewModel : ViewModel() {

    private val bridge: AccountingBridge = AccountingApp.getBridge()
    private val node = "MemoryMappingVM"

    private val _memories = MutableStateFlow<List<MemoryItemUi>>(emptyList())
    val memories: StateFlow<List<MemoryItemUi>> = _memories.asStateFlow()

    private val _mappings = MutableStateFlow<List<MappingItemUi>>(emptyList())
    val mappings: StateFlow<List<MappingItemUi>> = _mappings.asStateFlow()

    private val _memorySourceFilter = MutableStateFlow("")
    val memorySourceFilter: StateFlow<String> = _memorySourceFilter.asStateFlow()

    private val _expandedCategories = MutableStateFlow<Set<String>>(emptySet())
    val expandedCategories: StateFlow<Set<String>> = _expandedCategories.asStateFlow()

    val expenseCategories: List<Pair<String, Long>> = bridge.getExpenseCategories()
    val incomeCategories: List<Pair<String, Long>> = bridge.getIncomeCategories()

    private var currentMemoryType = "expense"
    private var currentMappingSource = "MANUAL"

    init {
        val requestId = AppLogger.generateRequestId()
        AppLogger.i(requestId, node, "init 开始加载记忆和映射数据")
        loadMemories(requestId)
        loadMappings(requestId)
    }

    private fun loadMemories(requestId: String) {
        viewModelScope.launch {
            try {
                bridge.getMemories(currentMemoryType).collect { items ->
                    _memories.value = items
                    AppLogger.d(requestId, node, "loadMemories 数据更新: ${items.size} 条")
                }
            } catch (e: Exception) {
                AppLogger.e(requestId, node, "loadMemories 异常", e)
            }
        }
    }

    private fun loadMappings(requestId: String) {
        viewModelScope.launch {
            try {
                bridge.getMappingsBySource(currentMappingSource).collect { items ->
                    _mappings.value = items
                    AppLogger.d(requestId, node, "loadMappings 数据更新: ${items.size} 条")
                }
            } catch (e: Exception) {
                AppLogger.e(requestId, node, "loadMappings 异常", e)
            }
        }
    }

    fun searchMemories(query: String) {
        val requestId = AppLogger.generateRequestId()
        AppLogger.i(requestId, node, "searchMemories 入口: query=$query")
        viewModelScope.launch {
            val all = _memories.value
            _memories.value = if (query.isBlank()) all else all.filter {
                it.triggerWord.contains(query, ignoreCase = true)
            }
            AppLogger.d(requestId, node, "searchMemories 出口: ${_memories.value.size} 条结果")
        }
    }

    fun toggleExpand(category: String) {
        _expandedCategories.value = _expandedCategories.value.let { set ->
            if (category in set) set - category else set + category
        }
    }

    fun setSourceFilter(filter: String) {
        _memorySourceFilter.value = filter
    }

    fun addMemory(triggerWord: String, type: String, category: String) {
        val requestId = AppLogger.generateRequestId()
        AppLogger.i(requestId, node, "addMemory 入口: triggerWord=$triggerWord, type=$type, category=$category")
        viewModelScope.launch {
            try {
                bridge.addMemory(triggerWord, type, category, requestId)
                AppLogger.d(requestId, node, "addMemory 出口: 成功")
            } catch (e: Exception) {
                AppLogger.e(requestId, node, "addMemory 异常", e)
            }
        }
    }

    fun deleteMemory(id: Long) {
        val requestId = AppLogger.generateRequestId()
        AppLogger.i(requestId, node, "deleteMemory 入口: id=$id")
        viewModelScope.launch {
            try {
                bridge.deleteMemory(id, requestId)
                AppLogger.d(requestId, node, "deleteMemory 出口: 成功")
            } catch (e: Exception) {
                AppLogger.e(requestId, node, "deleteMemory 异常", e)
            }
        }
    }

    fun clearAllMemories() {
        val requestId = AppLogger.generateRequestId()
        AppLogger.i(requestId, node, "clearAllMemories 入口")
        viewModelScope.launch {
            try {
                bridge.clearAllMemories(requestId)
                AppLogger.d(requestId, node, "clearAllMemories 出口: 成功")
            } catch (e: Exception) {
                AppLogger.e(requestId, node, "clearAllMemories 异常", e)
            }
        }
    }

    fun restoreDefaultMemories() {
        val requestId = AppLogger.generateRequestId()
        AppLogger.i(requestId, node, "restoreDefaultMemories 入口")
        viewModelScope.launch {
            try {
                bridge.restoreDefaultMemories(requestId)
                AppLogger.d(requestId, node, "restoreDefaultMemories 出口: 成功")
            } catch (e: Exception) {
                AppLogger.e(requestId, node, "restoreDefaultMemories 异常", e)
            }
        }
    }

    fun addMapping(keyword: String, type: String, categoryId: Long, subcategoryId: Long?) {
        val requestId = AppLogger.generateRequestId()
        AppLogger.i(requestId, node, "addMapping 入口: keyword=$keyword, type=$type, categoryId=$categoryId, subcategoryId=$subcategoryId")
        viewModelScope.launch {
            try {
                bridge.addMapping(keyword, type, categoryId, subcategoryId, requestId)
                AppLogger.d(requestId, node, "addMapping 出口: 成功")
            } catch (e: Exception) {
                AppLogger.e(requestId, node, "addMapping 异常", e)
            }
        }
    }

    fun deleteMapping(id: Long) {
        val requestId = AppLogger.generateRequestId()
        AppLogger.i(requestId, node, "deleteMapping 入口: id=$id")
        viewModelScope.launch {
            try {
                bridge.deleteMapping(id, requestId)
                AppLogger.d(requestId, node, "deleteMapping 出口: 成功")
            } catch (e: Exception) {
                AppLogger.e(requestId, node, "deleteMapping 异常", e)
            }
        }
    }

    fun toggleMappingEnabled(id: Long, enabled: Boolean) {
        val requestId = AppLogger.generateRequestId()
        AppLogger.i(requestId, node, "toggleMappingEnabled 入口: id=$id, enabled=$enabled")
        viewModelScope.launch {
            try {
                bridge.toggleMappingEnabled(id, enabled, requestId)
                AppLogger.d(requestId, node, "toggleMappingEnabled 出口: 成功")
            } catch (e: Exception) {
                AppLogger.e(requestId, node, "toggleMappingEnabled 异常", e)
            }
        }
    }

    fun promoteMappingToManual(id: Long) {
        val requestId = AppLogger.generateRequestId()
        AppLogger.i(requestId, node, "promoteMappingToManual 入口: id=$id")
        viewModelScope.launch {
            try {
                bridge.promoteMappingToManual(id, requestId)
                AppLogger.d(requestId, node, "promoteMappingToManual 出口: 成功")
            } catch (e: Exception) {
                AppLogger.e(requestId, node, "promoteMappingToManual 异常", e)
            }
        }
    }

    fun cleanStaleAutoMappings() {
        val requestId = AppLogger.generateRequestId()
        AppLogger.i(requestId, node, "cleanStaleAutoMappings 入口")
        viewModelScope.launch {
            try {
                bridge.cleanStaleAutoMappings(requestId)
                AppLogger.d(requestId, node, "cleanStaleAutoMappings 出口: 成功")
            } catch (e: Exception) {
                AppLogger.e(requestId, node, "cleanStaleAutoMappings 异常", e)
            }
        }
    }
}
