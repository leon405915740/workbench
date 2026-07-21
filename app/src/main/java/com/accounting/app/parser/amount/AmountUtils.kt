package com.accounting.app.parser.amount

import com.accounting.app.log.AppLogger

object AmountUtils {

    data class AmountSegment(val amountFen: Long, val textBefore: String)

    fun fenToYuan(fen: Long): String = String.format("%.2f", fen / 100.0)
    fun fenToYuanWithSymbol(fen: Long): String = "¥${fenToYuan(fen)}"
    fun formatAmountWithSign(fen: Long): String = "¥${fenToYuan(fen)}"
    fun yuanToFen(yuan: String): Long = (yuan.toDouble() * 100).toLong()

    fun isQuestionInput(rawInput: String): Boolean {
        val questionKeywords = listOf("怎么", "为什么", "看看", "查一下", "统计", "多少", "吗", "呢", "帮我", "分析", "总结", "对比")
        if (rawInput.contains("?") || rawInput.contains("？")) return true
        return questionKeywords.any { rawInput.contains(it) }
    }

    private val expenseContextWords = listOf(
        "早饭", "午饭", "晚饭", "早餐", "午餐", "晚餐", "中餐", "宵夜", "夜宵",
        "吃饭", "饭", "奶茶", "咖啡", "饮品", "打车", "地铁", "公交",
        "话费", "电费", "水费", "网费", "房租", "超市", "水果",
        "花", "花了", "消费", "支出", "付了", "付款", "买单", "结账",
        "充了", "充值", "买", "买了", "购买", "用了",
        "网购", "网上买", "下单", "入手", "入了一个", "入手了",
        "外卖", "点餐", "点了一份", "点了",
        "转了", "收了", "退了", "赚"
    )

    fun containsAmount(rawInput: String): Boolean {
        val excludePatterns = listOf("几元", "多少钱", "几个", "多少块", "多钱")
        if (excludePatterns.any { rawInput.contains(it) }) return false
        if (rawInput.trim().matches(Regex("^[\\d.]+$"))) return true
        if (Regex("""(¥|￥)\s*\d+(\.\d+)?|\d+(\.\d+)?\s*[元块钱]""").containsMatchIn(rawInput)) return true
        if (Regex("""(花|花了|消费|支出|收入|赚了|充了|买了|用了)\s*\d+(\.\d+)?""").containsMatchIn(rawInput)) return true

        val bareNumberRe = Regex("""\d+(\.\d+)?""")
        if (bareNumberRe.containsMatchIn(rawInput) && expenseContextWords.any { rawInput.contains(it) }) {
            return true
        }
        return false
    }

    fun extractAmount(rawInput: String): Long? = extractAmountWithPos(rawInput)?.first

    fun extractAmounts(rawInput: String, requestId: String): List<AmountSegment> {
        AppLogger.d(requestId, "金额提取", "开始提取金额")
        val results = mutableListOf<AmountSegment>()
        var remaining = rawInput

        try {
            while (remaining.isNotEmpty()) {
                val match = extractAmountWithPos(remaining) ?: break
                val (amountFen, startIndex, endIndex) = match
                val textBefore = remaining.substring(0, startIndex).trim()
                results.add(AmountSegment(amountFen, textBefore))
                remaining = remaining.substring(endIndex).trimStart()
            }
            val amountsDesc = results.joinToString(", ") { "value=${it.amountFen}分" }
            val segmentsDesc = results.joinToString(", ") { "\"${it.textBefore}${it.amountFen}分\"" }
            AppLogger.d(
                requestId,
                "金额提取",
                "原始文本：$rawInput，提取金额：[$amountsDesc]，拆分片段：[$segmentsDesc]，共${results.size}笔"
            )
        } catch (e: Exception) {
            AppLogger.e(requestId, "金额提取", "金额拆分异常：${e.message}", e)
        }
        return results
    }

    fun filterStopWords(raw: String): String {
        val stopWords = listOf("花了", "消费", "支出", "付了", "买了", "吃了", "喝了", "用了", "花", "买", "吃", "喝", "用", "付")
        val units = listOf("元", "块", "块钱", "毛", "个", "份", "顿")
        val particles = listOf("啊", "哦", "啦", "吧", "了", "的", "个")

        var result = raw
        for (w in stopWords) result = result.replace(w, "")
        for (u in units) result = result.replace(u, "")
        for (p in particles) result = result.replace(p, "")
        return result.trim()
    }

    fun cleanSegment(raw: String): String {
        var result = raw
        val separators = listOf(",", "，", "、", ";", "；", "。", ".", " ")
        val connectors = listOf("还有", "然后", "再", "加上", "另外", "以及")
        for (s in separators) result = result.replace(s, " ")
        for (c in connectors) result = result.replace(c, " ")
        return result.trim().replace(Regex("\\s+"), " ")
    }

    private fun extractAmountWithPos(rawInput: String): Triple<Long, Int, Int>? {
        val kuaiRe = Regex("""(\d+)\s*块\s*(\d+)""")
        val kuaiMatch = kuaiRe.find(rawInput)
        if (kuaiMatch != null) {
            val yuan = kuaiMatch.groupValues[1].toInt()
            val jiao = kuaiMatch.groupValues[2].toInt()
            return Triple((yuan * 100 + jiao * 10).toLong(), kuaiMatch.range.first, kuaiMatch.range.last + 1)
        }

        val symbolRe = Regex("""(¥|￥)\s*(\d+(?:\.\d+)?)""")
        val symMatch = symbolRe.find(rawInput)
        if (symMatch != null) {
            return Triple((symMatch.groupValues[2].toDouble() * 100).toLong(), symMatch.range.first, symMatch.range.last + 1)
        }

        val unitRe = Regex("""(\d+(?:\.\d+)?)\s*[元块钱]""")
        val unitMatch = unitRe.find(rawInput)
        if (unitMatch != null) {
            return Triple((unitMatch.groupValues[1].toDouble() * 100).toLong(), unitMatch.range.first, unitMatch.range.last + 1)
        }

        val verbRe = Regex("""(?:花|花了|消费|支出|收入|赚了|充了|买了|用了)\s*(\d+(?:\.\d+)?)""")
        val verbMatch = verbRe.find(rawInput)
        if (verbMatch != null) {
            return Triple((verbMatch.groupValues[1].toDouble() * 100).toLong(), verbMatch.range.first, verbMatch.range.last + 1)
        }

        val bareRe = Regex("""(\d+(?:\.\d+)?)""")
        val bareMatch = bareRe.find(rawInput)
        if (bareMatch != null && expenseContextWords.any { rawInput.contains(it) }) {
            return Triple((bareMatch.groupValues[1].toDouble() * 100).toLong(), bareMatch.range.first, bareMatch.range.last + 1)
        }

        val trim = rawInput.trim()
        if (trim.matches(Regex("^[\\d.]+$"))) {
            return Triple((trim.toDouble() * 100).toLong(), 0, trim.length)
        }

        return null
    }
}