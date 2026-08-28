package com.aigrowth.os.feature.learning.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aigrowth.os.core.database.entity.Goal
import com.aigrowth.os.core.database.entity.GoalStatus
import com.aigrowth.os.feature.learning.domain.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 目标列表ViewModel
 */
@HiltViewModel
class GoalListViewModel @Inject constructor(
    private val goalRepository: GoalRepository
) : ViewModel() {
    
    // 目标列表
    private val _goals = MutableStateFlow<List<Goal>>(emptyList())
    val goals: StateFlow<List<Goal>> = _goals
    
    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    // 错误信息
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    init {
        loadGoals()
    }
    
    /**
     * 加载目标列表
     */
    fun loadGoals() {
        viewModelScope.launch {
            _isLoading.value = true
            goalRepository.getActiveGoals()
                .catch { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
                .collect { goals ->
                    _goals.value = goals
                    _isLoading.value = false
                }
        }
    }
    
    /**
     * 删除目标
     */
    fun deleteGoal(goal: Goal) {
        viewModelScope.launch {
            goalRepository.deleteGoal(goal)
        }
    }
    
    /**
     * 更新目标状态
     */
    fun updateGoalStatus(goal: Goal, status: GoalStatus) {
        viewModelScope.launch {
            goalRepository.updateGoalStatus(goal.id, status)
        }
    }
    
    /**
     * 清除错误
     */
    fun clearError() {
        _error.value = null
    }
}

/**
 * 创建/编辑目标ViewModel
 */
@HiltViewModel
class GoalEditViewModel @Inject constructor(
    private val goalRepository: GoalRepository
) : ViewModel() {
    
    // 标题
    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title
    
    // 描述
    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description
    
    // 是否正在保存
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving
    
    // 保存完成事件
    private val _saveComplete = MutableSharedFlow<Boolean>()
    val saveComplete: SharedFlow<Boolean> = _saveComplete
    
    /**
     * 更新标题
     */
    fun setTitle(title: String) {
        _title.value = title
    }
    
    /**
     * 更新描述
     */
    fun setDescription(description: String) {
        _description.value = description
    }
    
    /**
     * 保存目标
     */
    fun saveGoal() {
        if (_title.value.isBlank()) {
            return
        }
        
        viewModelScope.launch {
            _isSaving.value = true
            try {
                goalRepository.createGoal(
                    title = _title.value,
                    description = _description.value
                )
                _saveComplete.emit(true)
            } catch (e: Exception) {
                _saveComplete.emit(false)
            } finally {
                _isSaving.value = false
            }
        }
    }
}