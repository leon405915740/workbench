package com.aigrowth.os.feature.learning.domain

import com.aigrowth.os.core.aiengine.*
import com.aigrowth.os.core.database.dao.FeynmanSessionDao
import com.aigrowth.os.core.database.entity.FeynmanSession
import com.aigrowth.os.core.database.entity.FeynmanStatus
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 费曼学习仓库
 * 管理费曼学习会话和AI对话
 */
@Singleton
class FeynmanRepository @Inject constructor(
    private val feynmanSessionDao: FeynmanSessionDao,
    private val learningAgent: LearningAgent
) {

    fun getSessionsByCard(knowledgeCardId: String): Flow<List<FeynmanSession>> {
        return feynmanSessionDao.getSessionsByCard(knowledgeCardId)
    }

    fun getAllSessions(): Flow<List<FeynmanSession>> {
        return feynmanSessionDao.getAllSessions()
    }

    suspend fun getSessionById(id: String): FeynmanSession? {
        return feynmanSessionDao.getSessionById(id)
    }

    /**
     * 创建新的费曼学习会话
     */
    suspend fun createSession(
        topic: String,
        knowledgeCardId: String? = null,
        targetScore: Int = 90
    ): FeynmanSession {
        val now = System.currentTimeMillis()
        val session = FeynmanSession(
            id = UUID.randomUUID().toString(),
            topic = topic,
            knowledgeCardId = knowledgeCardId,
            status = FeynmanStatus.IN_PROGRESS,
            currentScore = 0,
            targetScore = targetScore,
            finalFeedback = null,
            createdAt = now,
            completedAt = null
        )
        feynmanSessionDao.insertSession(session)
        return session
    }

    /**
     * 进行费曼学习对话
     */
    suspend fun feynmanDialog(
        sessionId: String,
        userExplanation: String,
        conversationHistory: List<FeynmanMessage>,
        apiKey: String
    ): Result<FeynmanDialogResult> {
        val session = feynmanSessionDao.getSessionById(sessionId)
            ?: return Result.failure(Exception("会话不存在"))

        val result = learningAgent.feynmanDialog(
            topic = session.topic,
            userExplanation = userExplanation,
            conversationHistory = conversationHistory,
            apiKey = apiKey
        )

        return result.fold(
            onSuccess = { response ->
                // 更新会话状态
                val isCompleted = response.score >= session.targetScore
                val updatedSession = session.copy(
                    currentScore = response.score,
                    status = if (isCompleted) FeynmanStatus.COMPLETED else session.status,
                    finalFeedback = if (isCompleted) response.childResponse else session.finalFeedback,
                    completedAt = if (isCompleted) System.currentTimeMillis() else session.completedAt
                )
                feynmanSessionDao.updateSession(updatedSession)

                Result.success(
                    FeynmanDialogResult(
                        response = response,
                        session = updatedSession,
                        isCompleted = isCompleted
                    )
                )
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    /**
     * 放弃会话
     */
    suspend fun abandonSession(sessionId: String) {
        val session = feynmanSessionDao.getSessionById(sessionId) ?: return
        val updatedSession = session.copy(
            status = FeynmanStatus.ABANDONED,
            completedAt = System.currentTimeMillis()
        )
        feynmanSessionDao.updateSession(updatedSession)
    }
}

/**
 * 费曼学习对话结果
 */
data class FeynmanDialogResult(
    val response: FeynmanResponse,
    val session: FeynmanSession,
    val isCompleted: Boolean
)
