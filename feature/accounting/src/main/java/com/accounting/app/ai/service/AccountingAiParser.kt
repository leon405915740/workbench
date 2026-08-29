package com.accounting.app.ai.service

import com.accounting.app.ai.model.DeepSeekModels
import com.accounting.app.ai.model.ChatMessage
import com.accounting.app.ai.model.ChatRequest
import com.accounting.app.ai.service.DeepSeekApi
import com.accounting.app.log.AppLogger
import com.accounting.app.ui.model.AccountingCandidate
import com.google.gson.Gson
import com.google.gson.JsonObject

/**
 * 记账解析专用 AI 解析器。
 *
 * 职责：仅做记账内容解析，不聊天、不查询、不写库。
 * 规则匹配失败时作为 AI 兜底，返回 AccountingCandidate 由调用方决定是否展示确认卡片。
 *
 * @param deepSeekApi      DeepSeek API 接口（与 AiPlanner 共用 createPlannerApi()）
 * @param apiKeyProvider   API Key 提供者（延迟获取，支持运行时修改）
 */
class AccountingAiParser(
    private val apiProvider: suspend () -> DeepSeekApi,
    private val apiKeyProvider: suspend () -> String,
    private val modelProvider: suspend () -> String = { DeepSeekModels.FLASH }
) {

    /**
     * 解析用户输入的记账内容。
     *
     * @param rawInput  用户原始输入
     * @param requestId 请求唯一ID，贯穿日志
     * @return 解析成功返回 AccountingCandidate（source="ai"），失败返回 null
     */
    suspend fun parse(rawInput: String, requestId: String): AccountingCandidate? {
        AppLogger.d(requestId, "AccountingAiParser", "开始解析: $rawInput")
        return try {
            val systemPrompt = buildSystemPrompt()
            val userPrompt = rawInput
            val json = callDeepSeek(systemPrompt, userPrompt, requestId)
            parseCandidate(json, requestId)
        } catch (e: Exception) {
            AppLogger.e(requestId, "AccountingAiParser", "解析失败: ${e.message}", e)
            null
        }
    }

    private fun buildSystemPrompt(): String {
        return """你是记账解析助手。解析用户输入的记账内容，输出标准 JSON。

【严格规则】
1. 仅输出标准 JSON，不能有任何解释或 markdown 格式
2. type 字段必须是 "expense"（支出）或 "income"（收入）
3. amount 字段为金额数值（单位：元，支持小数）
4. category 字段必须从以下列表中选择：
   - 支出分类：餐饮美食、交通出行、日用家居、娱乐休闲、服饰美容、住房房租、通讯资费、医疗健康、教育学习、人情往来、数码电器、爱车养车、宠物生活、旅行度假、育儿长辈、其他支出
   - 收入分类：工资薪水、兼职副业、理财收益、人情礼金、其他收入
5. 时间字段仅输出自然语言提示（如"今天""昨天中午"），严禁输出标准日期格式
6. 无法识别为记账内容时返回 {"error":"unparseable"}

请解析用户输入，输出包含 type/category/amount/description/time_hint/note 的 JSON。""".trimIndent()
    }

    private suspend fun callDeepSeek(systemPrompt: String, userPrompt: String, requestId: String): String {
        val apiKey = apiKeyProvider()
        if (apiKey.isBlank() || apiKey == "your_api_key_here") {
            AppLogger.e(requestId, "AccountingAiParser", "未配置 API Key", null)
            throw Exception("未配置 API Key")
        }
        val model = modelProvider()

        val maskedKey = AppLogger.maskApiKey(apiKey)
        AppLogger.i(
            requestId,
            "AccountingAiParser",
            "模型：$model，Prompt长度：${systemPrompt.length + userPrompt.length}字符，API Key：$maskedKey"
        )

        val request = ChatRequest(
            model = model,
            messages = listOf(
                ChatMessage("system", systemPrompt),
                ChatMessage("user", userPrompt)
            )
        )

        val response = apiProvider().chatCompletion("Bearer $apiKey", request)
        if (response.isSuccessful) {
            val content = response.body()?.choices?.firstOrNull()?.message?.content
                ?: throw Exception("AI 返回内容为空")
            val preview = if (content.length > 100) content.substring(0, 100) + "..." else content
            AppLogger.d(requestId, "AccountingAiParser", "状态：成功，返回长度：${content.length}字符，摘要：$preview")
            return content
        } else {
            val code = response.code()
            AppLogger.e(requestId, "AccountingAiParser", "状态：失败，错误码：$code", null)
            throw Exception("API 请求失败：$code")
        }
    }

    private fun parseCandidate(jsonStr: String, requestId: String): AccountingCandidate? {
        val cleaned = cleanJsonString(jsonStr)
        return try {
            val obj = Gson().fromJson(cleaned, JsonObject::class.java) ?: return null
            // 检查是否为不可解析
            if (obj.has("error")) {
                AppLogger.d(requestId, "AccountingAiParser", "AI 返回 unparseable")
                return null
            }
            val type = obj.get("type")?.takeIf { !it.isJsonNull }?.asString ?: return null
            val category = obj.get("category")?.takeIf { !it.isJsonNull }?.asString ?: return null

            // amount 可能是字符串或数字，统一处理
            val amountElement = obj.get("amount") ?: return null
            val amountYuan = when {
                amountElement.isJsonPrimitive && amountElement.asJsonPrimitive.isNumber -> amountElement.asDouble
                amountElement.isJsonPrimitive && amountElement.asJsonPrimitive.isString ->
                    amountElement.asString.replace(",", "").toDoubleOrNull() ?: return null
                else -> return null
            }
            // 元转分
            val amountFen = Math.round(amountYuan * 100)

            val description = obj.get("description")?.takeIf { !it.isJsonNull }?.asString ?: ""
            val timeHint = obj.get("time_hint")?.let { safeString(it) }
            val note = obj.get("note")?.let { safeString(it) }

            AppLogger.d(
                requestId,
                "AccountingAiParser",
                "解析成功: type=$type, category=$category, amount=$amountFen 分, description=$description"
            )
            AccountingCandidate(
                type = type,
                category = category,
                amount = amountFen,
                confidence = 0.8f,
                source = "ai",
                description = description,
                timeHint = timeHint,
                note = note
            )
        } catch (e: Exception) {
            AppLogger.e(requestId, "AccountingAiParser", "JSON 解析失败: ${e.message}", e)
            null
        }
    }

    /** 从 JsonElement 提取非空字符串，null/"null"/空白 均返回 null */
    private fun safeString(element: com.google.gson.JsonElement): String? {
        if (element.isJsonNull) return null
        val s = element.asString
        return if (s.isBlank() || s.equals("null", ignoreCase = true)) null else s
    }

    private fun cleanJsonString(raw: String): String {
        var s = raw.trim()
        if (s.startsWith("```")) {
            s = s.removePrefix("```json").removePrefix("```").trim()
            if (s.endsWith("```")) {
                s = s.removeSuffix("```").trim()
            }
        }
        val start = s.indexOf('{')
        val end = s.lastIndexOf('}')
        if (start in 0 until end) {
            s = s.substring(start, end + 1)
        }
        return s
    }
}
