package com.accounting.app.ai.model

data class ChatResponse(
    val choices: List<Choice>?
)

data class Choice(
    val message: ResponseMessage?
)

data class ResponseMessage(
    val content: String?
)