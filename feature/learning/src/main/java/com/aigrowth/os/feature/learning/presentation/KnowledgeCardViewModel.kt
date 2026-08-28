package com.aigrowth.os.feature.learning.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aigrowth.os.core.aiengine.ApiKeyService
import com.aigrowth.os.core.database.entity.KnowledgeCard
import com.aigrowth.os.feature.learning.domain.KnowledgeCardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KnowledgeCardViewModel @Inject constructor(
    private val knowledgeCardRepository: KnowledgeCardRepository,
    private val apiKeyService: ApiKeyService
) : ViewModel() {

    private val _cards = MutableStateFlow<List<KnowledgeCard>>(emptyList())
    val cards: StateFlow<List<KnowledgeCard>> = _cards

    private val _selectedCard = MutableStateFlow<KnowledgeCard?>(null)
    val selectedCard: StateFlow<KnowledgeCard?> = _selectedCard

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadCardsByLevel(levelId: String) {
        viewModelScope.launch {
            knowledgeCardRepository.getCardsByLevel(levelId)
                .catch { e ->
                    _error.value = e.message
                }
                .collect { cards ->
                    _cards.value = cards
                }
        }
    }

    fun loadAllCards() {
        viewModelScope.launch {
            knowledgeCardRepository.getAllCards()
                .catch { e ->
                    _error.value = e.message
                }
                .collect { cards ->
                    _cards.value = cards
                }
        }
    }

    fun selectCard(cardId: String) {
        viewModelScope.launch {
            _selectedCard.value = knowledgeCardRepository.getCardById(cardId)
        }
    }

    fun generateKnowledgeCard(
        levelId: String,
        topic: String,
        context: String
    ) {
        val apiKey = apiKeyService.getApiKey()
        if (apiKey.isBlank()) {
            _error.value = "请先在设置中配置API Key"
            return
        }

        viewModelScope.launch {
            _isGenerating.value = true
            _error.value = null

            val result = knowledgeCardRepository.generateKnowledgeCard(
                levelId = levelId,
                topic = topic,
                context = context,
                apiKey = apiKey
            )

            result.fold(
                onSuccess = {
                    loadCardsByLevel(levelId)
                },
                onFailure = { e ->
                    _error.value = e.message ?: "生成知识卡片失败"
                }
            )

            _isGenerating.value = false
        }
    }

    fun updateMasteryScore(cardId: String, score: Int) {
        viewModelScope.launch {
            knowledgeCardRepository.updateMasteryScore(cardId, score)
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSelectedCard() {
        _selectedCard.value = null
    }
}