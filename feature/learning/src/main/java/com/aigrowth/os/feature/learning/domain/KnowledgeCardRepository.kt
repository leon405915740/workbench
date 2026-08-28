package com.aigrowth.os.feature.learning.domain

import com.aigrowth.os.core.aiengine.KnowledgeCardResponse
import com.aigrowth.os.core.aiengine.LearningAgent
import com.aigrowth.os.core.database.dao.KnowledgeCardDao
import com.aigrowth.os.core.database.entity.KnowledgeCard
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KnowledgeCardRepository @Inject constructor(
    private val knowledgeCardDao: KnowledgeCardDao,
    private val learningAgent: LearningAgent
) {

    fun getCardsByLevel(levelId: String): Flow<List<KnowledgeCard>> {
        return knowledgeCardDao.getCardsByLevel(levelId)
    }

    fun getAllCards(): Flow<List<KnowledgeCard>> {
        return knowledgeCardDao.getAllCards()
    }

    suspend fun getCardById(id: String): KnowledgeCard? {
        return knowledgeCardDao.getCardById(id)
    }

    suspend fun generateKnowledgeCard(
        levelId: String,
        topic: String,
        context: String,
        apiKey: String
    ): Result<KnowledgeCard> {
        val result = learningAgent.generateKnowledgeCard(topic, context, apiKey)

        return result.fold(
            onSuccess = { response ->
                val card = saveKnowledgeCard(levelId, response)
                Result.success(card)
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    private suspend fun saveKnowledgeCard(
        levelId: String,
        response: KnowledgeCardResponse
    ): KnowledgeCard {
        val now = System.currentTimeMillis()
        val card = KnowledgeCard(
            id = UUID.randomUUID().toString(),
            learningLevelId = levelId,
            topic = response.topic,
            coreDefinition = response.coreDefinition,
            keyConcepts = com.google.gson.Gson().toJson(response.keyConcepts),
            useCases = com.google.gson.Gson().toJson(response.useCases),
            commonMistakes = com.google.gson.Gson().toJson(response.commonMistakes),
            checklist = com.google.gson.Gson().toJson(response.checklist),
            selfTestQuestions = com.google.gson.Gson().toJson(response.selfTestQuestions),
            masteryScore = 0,
            createdAt = now,
            updatedAt = now
        )

        knowledgeCardDao.insertCard(card)
        return card
    }

    suspend fun updateMasteryScore(cardId: String, score: Int) {
        val card = knowledgeCardDao.getCardById(cardId) ?: return
        val updatedCard = card.copy(
            masteryScore = score,
            updatedAt = System.currentTimeMillis()
        )
        knowledgeCardDao.updateCard(updatedCard)
    }
}