package com.accounting.app.parser.time

import com.accounting.app.log.AppLogger
import com.accounting.app.parser.time.TimeUtils
import com.accounting.app.parser.model.MatchRequest
import com.accounting.app.parser.model.MatchResult
import com.accounting.app.parser.model.MatchSource

object TimeRuleMatcher {

    fun match(request: MatchRequest, requestId: String, billIndex: Int? = null): MatchResult? {
        val category = request.hint ?: return null

        if (category != "餐饮") return null

        val timeSub = TimeUtils.matchTimeCategory(request.description)?.second
        if (timeSub != null) {
            val message = "待匹配：${request.description}，来源：time_rule，分类：餐饮-${timeSub}，时段规则命中，最终分类：餐饮-${timeSub}"
            if (billIndex != null) AppLogger.d(requestId, "分类匹配", message, billIndex)
            else AppLogger.d(requestId, "分类匹配", message)
            return MatchResult(
                type = "expense",
                category = "餐饮",
                subCategory = timeSub,
                source = MatchSource.TIME_RULE,
                confidence = 0.85f
            )
        }

        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val fallback = when {
            hour in 5..9 -> "早餐"
            hour in 10..13 -> "午餐"
            hour in 14..16 -> "饮品"
            hour in 17..20 -> "晚餐"
            else -> "夜宵"
        }

        val message = "待匹配：${request.description}，来源：time_rule，分类：餐饮-${fallback}，时段兜底，最终分类：餐饮-${fallback}"
        if (billIndex != null) AppLogger.d(requestId, "分类匹配", message, billIndex)
        else AppLogger.d(requestId, "分类匹配", message)
        return MatchResult(
            type = "expense",
            category = "餐饮",
            subCategory = fallback,
            source = MatchSource.TIME_RULE,
            confidence = 0.7f
        )
    }
}