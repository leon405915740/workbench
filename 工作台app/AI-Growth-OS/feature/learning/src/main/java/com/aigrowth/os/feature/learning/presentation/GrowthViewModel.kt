package com.aigrowth.os.feature.learning.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aigrowth.os.core.aiengine.ApiKeyService
import com.aigrowth.os.core.aiengine.GrowthReviewResponse
import com.aigrowth.os.core.database.entity.GrowthRecord
import com.aigrowth.os.feature.learning.domain.GrowthRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TaskCategory(val label: String) {
    ALL("全部"),
    STUDY("学习"),
    CREATION("创作"),
    ENGLISH("英语"),
    FITNESS("健身")
}

/**
 * 成长数据ViewModel
 */
@HiltViewModel
class GrowthViewModel @Inject constructor(
    private val growthRecordRepository: GrowthRecordRepository,
    private val apiKeyService: ApiKeyService
) : ViewModel() {

    private val _records = MutableStateFlow<List<GrowthRecord>>(emptyList())
    val records: StateFlow<List<GrowthRecord>> = _records

    private val _reviewResponse = MutableStateFlow<GrowthReviewResponse?>(null)
    val reviewResponse: StateFlow<GrowthReviewResponse?> = _reviewResponse

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadRecentRecords()
    }

    fun loadRecentRecords() {
        viewModelScope.launch {
            growthRecordRepository.getRecentRecords()
                .catch { e ->
                    _error.value = "加载记录失败: ${e.message}"
                }
                .collect { records ->
                    _records.value = records
                }
        }
    }

    fun loadAllRecords() {
        viewModelScope.launch {
            growthRecordRepository.getAllRecords()
                .catch { e ->
                    _error.value = "加载记录失败: ${e.message}"
                }
                .collect { records ->
                    _records.value = records
                }
        }
    }

    fun recordTodayGrowth() {
        viewModelScope.launch {
            try {
                growthRecordRepository.recordTodayGrowth()
                loadRecentRecords()
            } catch (e: Exception) {
                _error.value = "记录今日成长失败: ${e.message}"
            }
        }
    }

    fun generateGrowthReview(days: Int = 7) {
        val apiKey = apiKeyService.getApiKey()
        if (apiKey.isBlank()) {
            _error.value = "请先在设置中配置API Key"
            return
        }

        viewModelScope.launch {
            _isGenerating.value = true
            _error.value = null

            val result = growthRecordRepository.generateGrowthReview(days, apiKey)

            result.fold(
                onSuccess = { response ->
                    _reviewResponse.value = response
                },
                onFailure = { e ->
                    _error.value = e.message ?: "生成复盘报告失败"
                }
            )

            _isGenerating.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearReview() {
        _reviewResponse.value = null
    }

    // 分类筛选状态
    private val _selectedCategory = MutableStateFlow(TaskCategory.ALL)
    val selectedCategory: StateFlow<TaskCategory> = _selectedCategory

    // 选中记录ID
    private val _selectedRecordId = MutableStateFlow<String?>(null)
    val selectedRecordId: StateFlow<String?> = _selectedRecordId

    // 过滤后的记录
    val filteredRecords: StateFlow<List<GrowthRecord>> =
        combine(_records, _selectedCategory) { records, category ->
            if (category == TaskCategory.ALL) records
            else records.filter { it.matchesCategory(category) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 当前选中的记录
    val selectedRecord: StateFlow<GrowthRecord?> =
        combine(filteredRecords, _selectedRecordId) { records, id ->
            if (id == null) records.firstOrNull()
            else records.find { it.id == id } ?: records.firstOrNull()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 选中记录方法
    fun selectRecord(id: String) {
        _selectedRecordId.value = id
    }

    // 切换分类方法
    fun filterByCategory(category: TaskCategory) {
        _selectedCategory.value = category
        _selectedRecordId.value = null  // 重置选中，触发自动回退到第一条
    }
}

private fun GrowthRecord.matchesCategory(category: TaskCategory): Boolean {
    val summary = (aiSummary ?: "").lowercase()
    return when (category) {
        TaskCategory.ALL -> true
        TaskCategory.STUDY -> summary.contains("学") || summary.contains("理解") || summary.contains("learning")
        TaskCategory.CREATION -> summary.contains("写") || summary.contains("创") || summary.contains("content")
        TaskCategory.ENGLISH -> summary.contains("英语") || summary.contains("english")
        TaskCategory.FITNESS -> summary.contains("健身") || summary.contains("运动") || summary.contains("fitness")
    }
}
