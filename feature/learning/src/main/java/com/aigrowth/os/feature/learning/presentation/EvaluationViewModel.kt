package com.aigrowth.os.feature.learning.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aigrowth.os.core.aiengine.ApiKeyService
import com.aigrowth.os.core.aiengine.EvaluationResponse
import com.aigrowth.os.core.aiengine.LearningAgent
import com.aigrowth.os.core.database.entity.DailyTask
import com.aigrowth.os.feature.learning.domain.DailyTaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EvaluationViewModel @Inject constructor(
    private val dailyTaskRepository: DailyTaskRepository,
    private val learningAgent: LearningAgent,
    private val apiKeyService: ApiKeyService
) : ViewModel() {

    private val _task = MutableStateFlow<DailyTask?>(null)
    val task: StateFlow<DailyTask?> = _task

    private val _userAnswer = MutableStateFlow("")
    val userAnswer: StateFlow<String> = _userAnswer

    private val _evaluation = MutableStateFlow<EvaluationResponse?>(null)
    val evaluation: StateFlow<EvaluationResponse?> = _evaluation

    private val _isEvaluating = MutableStateFlow(false)
    val isEvaluating: StateFlow<Boolean> = _isEvaluating

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadTask(taskId: String) {
        viewModelScope.launch {
            _task.value = dailyTaskRepository.getTaskById(taskId)
        }
    }

    fun setUserAnswer(answer: String) {
        _userAnswer.value = answer
    }

    fun evaluateAnswer() {
        val currentTask = _task.value ?: return
        val answer = _userAnswer.value
        val apiKey = apiKeyService.getApiKey()

        if (apiKey.isBlank()) {
            _error.value = "请先在设置中配置API Key"
            return
        }

        if (answer.isBlank()) {
            _error.value = "请输入你的回答"
            return
        }

        viewModelScope.launch {
            _isEvaluating.value = true
            _error.value = null

            val result = learningAgent.evaluateAnswer(
                task = currentTask.title,
                userAnswer = answer,
                apiKey = apiKey
            )

            result.fold(
                onSuccess = { evaluation ->
                    _evaluation.value = evaluation
                    // 保存考核结果到数据库
                    dailyTaskRepository.saveTaskResponse(
                        taskId = currentTask.id,
                        userResponse = answer,
                        aiFeedback = buildFeedbackString(evaluation),
                        score = evaluation.score
                    )
                },
                onFailure = { e ->
                    _error.value = e.message ?: "考核失败"
                }
            )

            _isEvaluating.value = false
        }
    }

    fun clearEvaluation() {
        _evaluation.value = null
        _userAnswer.value = ""
    }

    fun clearError() {
        _error.value = null
    }

    private fun buildFeedbackString(evaluation: EvaluationResponse): String {
        return buildString {
            appendLine("理解程度：${evaluation.understandingLevel}")
            appendLine("应用能力：${evaluation.applicationAbility}")
            appendLine()
            appendLine("错误分析：")
            appendLine(evaluation.errorAnalysis)
            appendLine()
            appendLine("补充知识：")
            appendLine(evaluation.supplementaryKnowledge)
        }
    }
}