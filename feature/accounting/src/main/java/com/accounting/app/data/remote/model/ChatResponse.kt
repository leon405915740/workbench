package com.accounting.app.data.remote.model

/**
 * DeepSeek Chat Completion 响应体。
 *
 * 仅保留记账解析所需的字段：choices 数组中的 message.content。
 * 其余字段（usage、finish_reason 等）忽略，避免模型冗余。
 *
 * 所有字段均允许为空，由调用方统一做非空校验，
 * 防止 API 异常返回时直接 NPE 崩溃。
 */
data class ChatResponse(
    val choices: List<Choice>?
)

data class Choice(
    val message: ResponseMessage?
)

data class ResponseMessage(
    val content: String?
)
