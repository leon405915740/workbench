package com.aigrowth.os.feature.learning.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aigrowth.os.core.aiengine.ApiKeyService
import com.aigrowth.os.core.database.entity.LearningLevel
import com.aigrowth.os.core.database.entity.LearningPath
import com.aigrowth.os.feature.learning.domain.LearningPathRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LearningPathViewModel @Inject constructor(
    private val learningPathRepository: LearningPathRepository,
    private val apiKeyService: ApiKeyService
) : ViewModel() {
    
    private val _learningPath = MutableStateFlow<LearningPath?>(null)
    val learningPath: StateFlow<LearningPath?> = _learningPath
    
    private val _learningLevels = MutableStateFlow<List<LearningLevel>>(emptyList())
    val learningLevels: StateFlow<List<LearningLevel>> = _learningLevels
    
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    fun loadLearningPath(goalId: String) {
        viewModelScope.launch {
            learningPathRepository.getLearningPath(goalId)
                .catch { e ->
                    _error.value = e.message
                }
                .collect { path ->
                    _learningPath.value = path
                    
                    if (path != null) {
                        learningPathRepository.getLearningLevels(path.id)
                            .catch { e ->
                                _error.value = e.message
                            }
                            .collect { levels ->
                                _learningLevels.value = levels
                            }
                    }
                }
        }
    }
    
    fun generateLearningPath(
        goalId: String,
        topic: String,
        userLevel: String = "初学者"
    ) {
        val apiKey = apiKeyService.getApiKey()
        if (apiKey.isBlank()) {
            _error.value = "请先在设置中配置API Key"
            return
        }
        
        viewModelScope.launch {
            _isGenerating.value = true
            _error.value = null
            
            val result = learningPathRepository.generateLearningPath(
                goalId = goalId,
                topic = topic,
                userLevel = userLevel,
                apiKey = apiKey
            )
            
            result.fold(
                onSuccess = { path ->
                    _learningPath.value = path
                    loadLearningPath(goalId)
                },
                onFailure = { e ->
                    _error.value = e.message ?: "生成学习路线失败"
                }
            )
            
            _isGenerating.value = false
        }
    }
    
    fun startLevel(levelId: String) {
        viewModelScope.launch {
            learningPathRepository.updateLevelStatus(levelId, com.aigrowth.os.core.database.entity.LevelStatus.IN_PROGRESS)
        }
    }
    
    fun completeLevel(levelId: String) {
        viewModelScope.launch {
            learningPathRepository.updateLevelStatus(levelId, com.aigrowth.os.core.database.entity.LevelStatus.COMPLETED)
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}