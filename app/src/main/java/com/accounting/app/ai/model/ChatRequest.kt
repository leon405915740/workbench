package com.accounting.app.ai.model

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.1,
    val stream: Boolean = false
)

data class ChatMessage(
    val role: String,
    val content: String
)