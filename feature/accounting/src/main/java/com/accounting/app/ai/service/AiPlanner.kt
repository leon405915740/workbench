package com.accounting.app.ai.service

import com.accounting.app.ai.model.DeepSeekModels
import com.accounting.app.ai.model.ChatMessage
import com.accounting.app.ai.model.ChatRequest
import com.accounting.app.ai.service.DeepSeekApi
import com.accounting.app.log.AppLogger
import com.google.gson.Gson

class AiPlanner(
    private val apiProvider: suspend () -> DeepSeekApi,
    private val apiKeyProvider: suspend () -> String,
    private val modelProvider: suspend () -> String = { DeepSeekModels.FLASH }
) {

    suspend fun parse(input: String, requestId: String): com.accounting.app.ai.model.AiOutput {
        return try {
            val (systemPrompt, userPrompt) = buildPrompt(input)
            val json = callDeepSeek(systemPrompt, userPrompt, requestId)
            parseJson(json, requestId)
        } catch (e: Exception) {
            AppLogger.w(requestId, "AI解析", "解析失败，返回空结果：${e.message}")
            com.accounting.app.ai.model.AiOutput()
        }
    }

    private fun buildPrompt(input: String): Pair<String, String> {
        val systemPrompt = """你是专业记账解析助手。

【严格规则】
1. 绝对不能说"已记账""已记录""已保存"等执行类表述
2. 绝对不能执行任何数据库操作
3. 仅输出标准 JSON，不能有任何解释、说明、markdown 格式
4. 缺失的字段填 null，不要编造
5. 金额单位为元，支持小数
6. 支持拆分多笔消费
7. 【时间强制规则】时间字段仅输出自然语言提示（如"今天""昨天中午""12:30"），严禁输出YYYY-MM-DD格式日期、毫秒时间戳、纯数字日期；时间不确定填空，禁止编造

请解析用户输入的记账内容，输出包含items数组的JSON。每条记录包含以下字段：
- description: 描述（如"午餐"、"打车"）
- amount: 金额（支持"30"、"30.5"、"三十"等格式）
- time_hint: 时间提示（仅允许自然语言，如"今天""昨天中午""12:30"；严禁YYYY-MM-DD、时间戳、纯数字日期；不确定填空）
- category_hint: 分类提示（如"餐饮"、"交通"，不需要严格匹配）
- note: 备注（可为null）

多条记录用"然后"、"接着"、"顺便"、"又"、"还"等词分隔。

输出格式：
{"items":[{"description":"","amount":"","time_hint":"","category_hint":"","note":null}]}""".trimIndent()

        val userPrompt = "用户输入：$input"
        return systemPrompt to userPrompt
    }

    private suspend fun callDeepSeek(systemPrompt: String, userPrompt: String, requestId: String): String {
        val apiKey = apiKeyProvider()
        if (apiKey.isBlank() || apiKey == "your_api_key_here") {
            AppLogger.e(requestId, "AI请求发起", "未配置 API Key", null)
            throw Exception("未配置 API Key")
        }
        val model = modelProvider()

        val maskedKey = AppLogger.maskApiKey(apiKey)
        AppLogger.i(
            requestId,
            "AI请求发起",
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
            AppLogger.d(requestId, "AI响应返回", "状态：成功，返回长度：${content.length}字符，摘要：$preview")
            return content
        } else {
            val code = response.code()
            AppLogger.e(requestId, "AI响应返回", "状态：失败，错误码：$code", null)
            throw Exception("API 请求失败：$code")
        }
    }

    private fun parseJson(jsonStr: String, requestId: String): com.accounting.app.ai.model.AiOutput {
        val cleaned = cleanJsonString(jsonStr)
        return try {
            val parsed = Gson().fromJson(cleaned, com.accounting.app.ai.model.AiOutput::class.java)
            sanitizeTimeHints(parsed, requestId)
        } catch (e: Exception) {
            AppLogger.w(requestId, "AI解析", "JSON解析失败，返回空结果：${e.message}")
            com.accounting.app.ai.model.AiOutput()
        }
    }

    private fun sanitizeTimeHints(output: com.accounting.app.ai.model.AiOutput, requestId: String): com.accounting.app.ai.model.AiOutput {
        val forbiddenPatterns = listOf(
            Regex("""\d{4}-\d{2}-\d{2}"""),
            Regex("""\d{13}"""),
            Regex("""\d{4}/\d{2}/\d{2}"""),
            Regex("""\d{2}/\d{2}/\d{4}"""),
            Regex("""\d{4}年\d{2}月\d{2}日""")
        )

        val sanitizedItems = output.items.map { item ->
            val hint = item.time_hint
            if (!hint.isNullOrBlank() && forbiddenPatterns.any { it.containsMatchIn(hint) }) {
                AppLogger.w(requestId, "AI解析", "检测到AI返回标准时间格式，已清空time_hint：${hint}")
                item.copy(time_hint = null)
            } else {
                item
            }
        }

        return output.copy(items = sanitizedItems)
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