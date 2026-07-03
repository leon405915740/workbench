package com.accounting.app.data.remote

import com.accounting.app.data.remote.model.ChatRequest
import com.accounting.app.data.remote.model.ChatResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * DeepSeek API 接口定义。
 *
 * 使用 OpenAI 兼容的 Chat Completion 协议。
 *
 * 设计要点：
 * - Authorization 通过 @Header 动态传入而非拦截器固定，
 *   因为用户可能在设置页随时修改 API Key
 * - 返回 Response<ChatResponse> 而非裸 ChatResponse，
 *   便于调用方获取 HTTP 错误码与错误信息
 */
interface DeepSeekApi {

    /**
     * 创建一次对话补全。
     *
     * @param authorization 形如 "Bearer sk-xxxx" 的鉴权头
     * @param request       请求体
     */
    @POST("v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: ChatRequest
    ): Response<ChatResponse>
}
