package com.aigrowth.os.feature.learning.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aigrowth.os.core.aiengine.ApiKeyService
import com.aigrowth.os.core.database.entity.DailyTask
import com.aigrowth.os.core.database.entity.TaskStatus
import com.aigrowth.os.core.database.entity.TaskType
import com.aigrowth.os.feature.learning.domain.DailyTaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DailyTaskViewModel @Inject constructor(
    private val dailyTaskRepository: DailyTaskRepository,
    private val apiKeyService: ApiKeyService
) : ViewModel() {
    
    private val _tasks = MutableStateFlow<List<DailyTask>>(emptyList())
    val tasks: StateFlow<List<DailyTask>> = _tasks
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    fun loadTodayTasks() {
        viewModelScope.launch {
            _isLoading.value = true
            dailyTaskRepository.getTodayTasks()
                .catch { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
                .collect { tasks ->
                    _tasks.value = tasks
                    _isLoading.value = false
                }
        }
    }
    
    fun generateDailyTasks(
        levelId: String,
        levelTitle: String
    ) {
        val apiKey = apiKeyService.getApiKey()
        if (apiKey.isBlank()) {
            _error.value = "请先在设置中配置API Key"
            return
        }
        
        viewModelScope.launch {
            _isGenerating.value = true
            
            val previousTasks = _tasks.value
                .filter { it.status == TaskStatus.COMPLETED }
                .takeLast(5)
                .joinToString("\n") { "- ${it.title}" }
            
            val result = dailyTaskRepository.generateDailyTasks(
                levelId = levelId,
                levelTitle = levelTitle,
                previousTasks = previousTasks,
                apiKey = apiKey
            )
            
            result.fold(
                onSuccess = {
                    loadTodayTasks()
                },
                onFailure = { e ->
                    _error.value = e.message ?: "生成任务失败"
                }
            )
            
            _isGenerating.value = false
        }
    }
    
    fun completeTask(taskId: String) {
        viewModelScope.launch {
            dailyTaskRepository.completeTask(taskId)
        }
    }
    
    fun skipTask(taskId: String) {
        viewModelScope.launch {
            dailyTaskRepository.skipTask(taskId)
        }
    }

    /**
     * 手动添加任务（本地闭环，不依赖AI）
     */
    fun addManualTask(title: String, description: String, minutes: Int, taskType: TaskType) {
        if (title.isBlank()) {
            _error.value = "任务标题不能为空"
            return
        }
        viewModelScope.launch {
            try {
                dailyTaskRepository.addManualTask(title, description, minutes, taskType)
                loadTodayTasks()
            } catch (e: Exception) {
                _error.value = e.message ?: "添加任务失败"
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}