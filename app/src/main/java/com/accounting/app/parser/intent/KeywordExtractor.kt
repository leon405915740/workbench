package com.accounting.app.parser.intent

import com.accounting.app.log.AppLogger

object KeywordExtractor {

    private val units = listOf("元", "块", "钱", "块钱")

    fun extractKeyword(description: String?, merchant: String?): String? {
        AppLogger.d("", "KeywordExtractor", "提取关键词: description=${description?.take(30)}, merchant=${merchant?.take(30)}")
        if (!merchant.isNullOrBlank() && merchant.trim().length >= 2) {
            AppLogger.d("", "KeywordExtractor", "提取结果: ${merchant.trim()}")
            return merchant.trim()
        }

        if (description.isNullOrBlank()) {
            AppLogger.d("", "KeywordExtractor", "未提取到关键词")
            return null
        }

        var result: String = description

        result = result.replace(Regex("[\\d.]+"), "")

        for (unit in units) {
            result = result.replace(unit, "")
        }

        result = result.replace(Regex("\\s+"), "")

        return if (result.length >= 2) {
            AppLogger.d("", "KeywordExtractor", "提取结果: $result")
            result
        } else {
            AppLogger.d("", "KeywordExtractor", "未提取到关键词")
            null
        }
    }
}