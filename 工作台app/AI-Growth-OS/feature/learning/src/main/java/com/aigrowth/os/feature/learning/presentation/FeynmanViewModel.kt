package com.aigrowth.os.feature.learning.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aigrowth.os.core.aiengine.ApiKeyService
import com.aigrowth.os.core.aiengine.FeynmanMessage
import com.aigrowth.os.core.aiengine.FeynmanRole
import com.aigrowth.os.core.database.entity.FeynmanSession
import com.aigrowth.os.feature.learning.domain.FeynmanDialogResult
import com.aigrowth.os.feature.learning.domain.FeynmanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 费曼学习ViewModel
 * 管理费曼学习模式的UI状态和业务逻辑
 */
@HiltViewModel
class FeynmanViewModel @Inject constructor(
    private val feynmanRepository: FeynmanRepository,
    private val apiKeyService: ApiKeyService
) : ViewModel() {

    // 当前会话
    private val _currentSession = MutableStateFlow<FeynmanSession?>(null)
    val currentSession: StateFlow<FeynmanSession?> = _currentSession

    // 对话历史
    private val _messages = MutableStateFlow<List<FeynmanMessage>>(emptyList())
    val messages: StateFlow<List<FeynmanMessage>> = _messages

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // 错误信息
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // 是否完成
    private val _isCompleted = MutableStateFlow(false)
    val isCompleted: StateFlow<Boolean> = _isCompleted

    // 当前AI响应
    private val _lastResponse = MutableStateFlow<FeynmanDialogResult?>(null)
    val lastResponse: StateFlow<FeynmanDialogResult?> = _lastResponse

    /**
     * 创建新会话并开始费曼学习
     */
    fun startSession(topic: String, knowledgeCardId: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _messages.value = emptyList()
            _isCompleted.value = false
            _lastResponse.value = null

            try {
                val session = feynmanRepository.createSession(topic, knowledgeCardId)
                _currentSession.value = session
            } catch (e: Exception) {
                _error.value = "创建会话失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 发送用户解释/回答
     */
    fun sendExplanation(content: String) {
        val session = _currentSession.value ?: run {
            _error.value = "请先开始一个会话"
            return
        }

        val apiKey = apiKeyService.getApiKey()
        if (apiKey.isBlank()) {
            _error.value = "请先在设置中配置API Key"
            return
        }

        // 添加用户消息到历史
        val userMessage = FeynmanMessage(FeynmanRole.USER, content)
        val updatedHistory = _messages.value + userMessage
        _messages.value = updatedHistory

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val result = feynmanRepository.feynmanDialog(
                sessionId = session.id,
                userExplanation = content,
                conversationHistory = updatedHistory,
                apiKey = apiKey
            )

            result.fold(
                onSuccess = { dialogResult ->
                    // 添加AI消息到历史
                    val aiMessage = FeynmanMessage(
                        FeynmanRole.AI_CHILD,
                        dialogResult.response.childResponse
                    )
                    _messages.value = _messages.value + aiMessage
                    _lastResponse.value = dialogResult
                    _isCompleted.value = dialogResult.isCompleted

                    // 更新会话引用
                    _currentSession.value = dialogResult.session
                },
                onFailure = { e ->
                    _error.value = e.message ?: "对话失败"
                }
            )

            _isLoading.value = false
        }
    }

    /**
     * 放弃当前会话
     */
    fun abandonSession() {
        val sessionId = _currentSession.value?.id ?: return
        viewModelScope.launch {
            feynmanRepository.abandonSession(sessionId)
            _currentSession.value = null
            _messages.value = emptyList()
            _isCompleted.value = false
            _lastResponse.value = null
        }
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * 重置状态
     */
    fun reset() {
        _currentSession.value = null
        _messages.value = emptyList()
        _isCompleted.value = false
        _lastResponse.value = null
        _error.value = null
        _isLoading.value = false
    }
}
