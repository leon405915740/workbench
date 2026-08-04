package com.accounting.app.parser.category

import com.accounting.app.ai.service.DeepSeekApi
import com.accounting.app.ai.model.ChatMessage
import com.accounting.app.ai.model.ChatRequest
import com.accounting.app.ai.model.DeepSeekModels
import com.accounting.app.log.AppLogger
import com.accounting.app.parser.model.MatchResult
import com.accounting.app.parser.model.MatchSource
import com.accounting.app.util.CategoryConstants
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * AI 兜底分类纠正器。
 *
 * 当记账分类的规则匹配（MappingMatcher/RuleMatcher/TimeRuleMatcher/AI_HINT）全部未命中时，
 * 调用 DeepSeek AI 从预置支出分类列表中选择最合适分类，避免直接 fallback 到"其他"。
 *
 * 设计要点：
 * - 内部自建独立 Retrofit 实例（短超时 5s/10s，不附加 HttpLoggingInterceptor，避免 Release 包明文泄露 API Key）
 * - LRU 去重缓存（容量 100，1 小时有效），相同 description 不重复调用 API
 * - 所有异常 catch 后返回 null，调用方降级走 fallback
 */
class AiClassifier(private val apiKeyProvider: suspend () -> String) {

    /** LRU 去重缓存：description -> Pair(category, timestamp)，容量 100，1 小时有效 */
    private val cache = java.util.LinkedHashMap<String, Pair<String, Long>>()
    private val cacheCapacity = 100
    private val cacheTtl = 3600000L

    /** 独立 Retrofit 实例（短超时，无 HttpLoggingInterceptor，避免 Release 包明文泄露 API Key） */
    private val deepSeekApi: DeepSeekApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.deepseek.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(DeepSeekApi::class.java)
    }

    /**
     * AI 兜底分类纠正入口。
     *
     * @param description 消费描述文本
     * @param requestId 请求ID，用于日志追踪
     * @param billIndex 账单序号（多笔场景），可为 null
     * @return 命中则返回 [MatchResult]（source=AI_CORRECTION，confidence=0.6f）；降级返回 null
     */
    suspend fun correct(description: String, requestId: String, billIndex: Int?): MatchResult? {
        return try {
            // 1. 查缓存：命中且未过期则直接返回
            synchronized(cache) {
                val cached = cache[description]
                if (cached != null && System.currentTimeMillis() - cached.second < cacheTtl) {
                    logI(requestId, "分类兜底缓存", "命中缓存，分类：${cached.first}", billIndex)
                    return MatchResult(
                        type = "expense",
                        category = cached.first,
                        source = MatchSource.AI_CORRECTION,
                        confidence = 0.6f
                    )
                }
            }

            // 2. 获取 apiKey 并校验
            val apiKey = apiKeyProvider()
            if (apiKey.isBlank() || apiKey == "your_api_key_here") {
                logE(requestId, "AI请求发起", "未配置 API Key（分类兜底）", billIndex)
                return null
            }

            // 3. 构建 Prompt：列出全部分类名，强约束输出
            val categoryList = CategoryConstants.expenseCategories.joinToString("、")
            val systemPrompt = """你是记账分类助手。请从以下支出分类列表中选择最合适的一个分类：
$categoryList

【严格规则】
1. 仅输出单个分类名，无标点、无解释、无换行、无 markdown
2. 必须从给定列表中选择，不能编造新分类
3. 无法判断时输出"其他支出"
4. 直接输出分类名，不要有任何多余文字""".trimIndent()
            val userPrompt = "消费描述：$description"

            // 4. 记日志：API Key 用 maskApiKey 脱敏
            val maskedKey = AppLogger.maskApiKey(apiKey)
            logI(
                requestId,
                "AI请求发起",
                "模型：${DeepSeekModels.FLASH}（分类兜底），Prompt长度：${systemPrompt.length + userPrompt.length}字符，API Key：$maskedKey",
                billIndex
            )

            // 5. 构造请求
            val request = ChatRequest(
                model = DeepSeekModels.FLASH,
                messages = listOf(
                    ChatMessage("system", systemPrompt),
                    ChatMessage("user", userPrompt)
                ),
                temperature = 0.1
            )

            // 6. 发请求
            val response = deepSeekApi.chatCompletion("Bearer $apiKey", request)

            // 7. 处理响应
            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content
                if (content.isNullOrBlank()) {
                    logE(requestId, "AI响应返回", "状态：成功（分类兜底），但返回内容为空", billIndex)
                    return null
                }
                val preview = if (content.length > 100) content.substring(0, 100) + "..." else content
                logI(
                    requestId,
                    "AI响应返回",
                    "状态：成功（分类兜底），返回长度：${content.length}字符，摘要：$preview",
                    billIndex
                )

                // 解析：trim 后精确匹配分类列表
                val trimmed = content.trim()
                if (CategoryConstants.expenseCategories.contains(trimmed)) {
                    // 写入缓存（线程安全）
                    synchronized(cache) {
                        cache[description] = Pair(trimmed, System.currentTimeMillis())
                        // 缓存满时移除最旧条目
                        if (cache.size > cacheCapacity) {
                            val oldestKey = cache.keys.firstOrNull()
                            if (oldestKey != null) {
                                cache.remove(oldestKey)
                            }
                        }
                    }
                    MatchResult(
                        type = "expense",
                        category = trimmed,
                        source = MatchSource.AI_CORRECTION,
                        confidence = 0.6f
                    )
                } else {
                    logE(requestId, "AI响应返回", "AI返回分类无效：$trimmed", billIndex)
                    null
                }
            } else {
                logE(
                    requestId,
                    "AI响应返回",
                    "状态：失败，错误码：${response.code()}",
                    billIndex
                )
                null
            }
        } catch (e: Exception) {
            logE(requestId, "AI响应返回", "分类兜底异常：${e.message}", billIndex, e)
            null
        }
    }

    // ===================== 日志辅助方法 =====================
    // AppLogger 的 d/e/i 方法 billIndex 参数为 Int（非空，无默认值），需按是否为 null 分支调用；
    // e 方法还需 throwable 参数（可为 null）。

    private fun logI(requestId: String, node: String, message: String, billIndex: Int?) {
        if (billIndex != null) AppLogger.i(requestId, node, message, billIndex)
        else AppLogger.i(requestId, node, message)
    }

    private fun logE(
        requestId: String,
        node: String,
        message: String,
        billIndex: Int?,
        throwable: Throwable? = null
    ) {
        if (billIndex != null) AppLogger.e(requestId, node, message, throwable, billIndex)
        else AppLogger.e(requestId, node, message, throwable)
    }
}
