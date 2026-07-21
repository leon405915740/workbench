package com.accounting.app.parser.intent

import com.accounting.app.log.AppLogger
import com.accounting.app.util.CategoryConstants
import com.accounting.app.parser.time.TimeUtils
import com.accounting.app.parser.model.MatchRequest
import com.accounting.app.parser.model.MatchResult
import com.accounting.app.parser.model.MatchSource

object RuleMatcher {

    private val expenseKeywords = listOf(
        "花", "花了", "花费", "消费", "支出", "付了", "付款", "买单", "结账",
        "充了", "充值", "买", "买了", "购买", "打车", "吃饭", "外卖", "奶茶",
        "咖啡", "房租", "话费", "电费", "水费", "会员"
    )

    private val incomeKeywords = listOf(
        "发工资", "到账", "收入", "收到", "赚了", "盈利", "报销", "退款",
        "分红", "奖金", "工资", "兼职", "理财收益", "补贴", "收了", "入账"
    )

    fun preJudgeType(rawInput: String, requestId: String): String {
        val hitIncome = incomeKeywords.firstOrNull { rawInput.contains(it) }
        if (hitIncome != null) {
            AppLogger.d(requestId, "意图分流", "原始文本：${rawInput}，命中关键词：${hitIncome}，判定类型：income，判定依据：收入关键词")
            return "income"
        }
        val hitExpense = expenseKeywords.firstOrNull { rawInput.contains(it) }
        if (hitExpense != null) {
            AppLogger.d(requestId, "意图分流", "原始文本：${rawInput}，命中关键词：${hitExpense}，判定类型：expense，判定依据：支出关键词")
            return "expense"
        }
        AppLogger.d(requestId, "意图分流", "原始文本：${rawInput}，未命中收支关键词，默认判定：expense")
        return "expense"
    }

    fun match(request: MatchRequest, requestId: String, billIndex: Int? = null): MatchResult? {
        val type = preJudgeType(request.description, requestId)

        val timeCat = TimeUtils.matchTimeCategory(request.description)
        if (timeCat != null) {
            val message = "待匹配：${request.description}，触发词：${timeCat.first}，来源：rule，分类：${timeCat.first}-${timeCat.second}，时段规则命中，最终分类：${timeCat.first}-${timeCat.second}"
            if (billIndex != null) AppLogger.d(requestId, "分类匹配", message, billIndex)
            else AppLogger.d(requestId, "分类匹配", message)
            return MatchResult(
                type = type,
                category = timeCat.first,
                subCategory = timeCat.second,
                source = MatchSource.RULE,
                confidence = 0.95f
            )
        }

        for ((keyword, catPair) in CategoryConstants.builtinSceneMap) {
            if (request.description.contains(keyword)) {
                val message = "待匹配：${request.description}，触发词：${keyword}，来源：rule，分类：${catPair.first}-${catPair.second}，最终分类：${catPair.first}-${catPair.second}"
                if (billIndex != null) AppLogger.d(requestId, "分类匹配", message, billIndex)
                else AppLogger.d(requestId, "分类匹配", message)
                return MatchResult(
                    type = type,
                    category = catPair.first,
                    subCategory = catPair.second,
                    source = MatchSource.RULE,
                    confidence = 0.9f
                )
            }
        }

        val message = "待匹配：${request.description}，来源：rule，未命中，返回null"
        if (billIndex != null) AppLogger.d(requestId, "分类匹配", message, billIndex)
        else AppLogger.d(requestId, "分类匹配", message)
        return null
    }
}