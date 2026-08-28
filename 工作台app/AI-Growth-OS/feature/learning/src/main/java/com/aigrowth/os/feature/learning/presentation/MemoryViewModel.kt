package com.aigrowth.os.feature.learning.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aigrowth.os.core.aiengine.ApiKeyService
import com.aigrowth.os.core.aiengine.MemoryManager
import com.aigrowth.os.core.database.entity.AIMemory
import com.aigrowth.os.core.database.entity.MemoryType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AI记忆ViewModel
 * 管理记忆相关的UI状态和业务逻辑
 */
@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val memoryManager: MemoryManager,
    private val apiKeyService: ApiKeyService
) : ViewModel() {

    private val _memories = MutableStateFlow<List<AIMemory>>(emptyList())
    val memories: StateFlow<List<AIMemory>> = _memories

    private val _selectedType = MutableStateFlow<MemoryType?>(null)
    val selectedType: StateFlow<MemoryType?> = _selectedType

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isExtracting = MutableStateFlow(false)
    val isExtracting: StateFlow<Boolean> = _isExtracting

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog

    init {
        loadMemories()
    }

    fun loadMemories() {
        viewModelScope.launch {
            memoryManager.getAllMemories()
                .catch { e ->
                    _error.value = "加载记忆失败: ${e.message}"
                }
                .collect { memories ->
                    _memories.value = memories
                }
        }
    }

    fun filterByType(type: MemoryType?) {
        _selectedType.value = type
        viewModelScope.launch {
            val flow = if (type != null) {
                memoryManager.getMemoriesByType(type)
            } else {
                memoryManager.getAllMemories()
            }
            flow.collect { memories ->
                _memories.value = memories
            }
        }
    }

    fun addMemory(type: MemoryType, content: String, importance: Int) {
        viewModelScope.launch {
            try {
                memoryManager.saveMemory(
                    type = type,
                    content = content,
                    importance = importance
                )
                _showAddDialog.value = false
            } catch (e: Exception) {
                _error.value = "保存记忆失败: ${e.message}"
            }
        }
    }

    fun deleteMemory(memory: AIMemory) {
        viewModelScope.launch {
            try {
                memoryManager.deleteMemory(memory)
            } catch (e: Exception) {
                _error.value = "删除记忆失败: ${e.message}"
            }
        }
    }

    fun extractMemoriesFromConversation(conversationText: String) {
        val apiKey = apiKeyService.getApiKey()
        if (apiKey.isBlank()) {
            _error.value = "请先在设置中配置API Key"
            return
        }

        viewModelScope.launch {
            _isExtracting.value = true
            _error.value = null

            val result = memoryManager.extractMemoriesFromConversation(conversationText, apiKey)

            result.fold(
                onSuccess = { extractedMemories ->
                    if (extractedMemories.isEmpty()) {
                        _error.value = "未从对话中提取到新的记忆"
                    } else {
                        memoryManager.saveExtractedMemories(extractedMemories)
                    }
                },
                onFailure = { e ->
                    _error.value = e.message ?: "提取记忆失败"
                }
            )

            _isExtracting.value = false
        }
    }

    fun showAddDialog() {
        _showAddDialog.value = true
    }

    fun dismissAddDialog() {
        _showAddDialog.value = false
    }

    fun clearError() {
        _error.value = null
    }

    fun getMemoryTypeLabel(type: MemoryType): String {
        return when (type) {
            MemoryType.WEAKNESS -> "薄弱点"
            MemoryType.PREFERENCE -> "偏好"
            MemoryType.HABIT -> "习惯"
            MemoryType.ACHIEVEMENT -> "成就"
        }
    }

    fun getMemoryTypeColor(type: MemoryType): androidx.compose.ui.graphics.Color {
        return when (type) {
            MemoryType.WEAKNESS -> androidx.compose.ui.graphics.Color(0xFFE57373)
            MemoryType.PREFERENCE -> androidx.compose.ui.graphics.Color(0xFF64B5F6)
            MemoryType.HABIT -> androidx.compose.ui.graphics.Color(0xFF81C784)
            MemoryType.ACHIEVEMENT -> androidx.compose.ui.graphics.Color(0xFFFFB74D)
        }
    }
}
