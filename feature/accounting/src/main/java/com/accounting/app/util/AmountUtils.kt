package com.accounting.app.util

import com.accounting.app.log.AppLogger

/**
 * 金额转换工具。
 *
 * 数据库统一以「分」为单位存储 Long，避免浮点误差；
 * 显示与输入时再在「元」与「分」之间转换。
 */
object AmountUtils {

    /** 金额+文本片段，用于多笔拆分 */
    data class AmountSegment(val amountFen: Long, val textBefore: String)

    fun fenToYuan(fen: Long): String = String.format("%.2f", fen / 100.0)
    fun fenToYuanWithSymbol(fen: Long): String = "¥${fenToYuan(fen)}"
    fun formatAmountWithSign(fen: Long): String = "¥${fenToYuan(fen)}"
    fun yuanToFen(yuan: String): Long = (yuan.toDouble() * 100).toLong()

    /**
     * 从支付通知文本中提取单笔金额（分）。
     *
     * 支持「¥12.5」「￥12.5」「12.5元」「12.50」等通知常见格式。
     * 第三类仅匹配两位小数的裸数字（支付通知金额几乎都带两位小数），避免误吃订单号/日期。
     */
    fun extractFenFromAmountText(text: String): Long? {
        Regex("""(¥|￥)\s*(\d+(?:\.\d+)?)""").find(text)?.let {
            return (it.groupValues[2].toDouble() * 100).toLong()
        }
        Regex("""(\d+(?:\.\d+)?)\s*元""").find(text)?.let {
            return (it.groupValues[1].toDouble() * 100).toLong()
        }
        Regex("""(\d+\.\d{2})""").find(text)?.let {
            return (it.groupValues[1].toDouble() * 100).toLong()
        }
        return null
    }

    fun isQuestionInput(rawInput: String): Boolean {
        val questionKeywords = listOf("怎么", "为什么", "看看", "查一下", "统计", "多少", "吗", "呢", "帮我", "分析", "总结", "对比")
        if (rawInput.contains("?") || rawInput.contains("？")) return true
        return questionKeywords.any { rawInput.contains(it) }
    }

    /**
     * 记账语境词表（含收入与支出）：命中任一且同时存在金额，判定为记账意图。
     * 纯语境词无金额不触发，需配合 containsAmount 使用。
     */
    private val accountingContextWords = listOf(
        "早饭", "午饭", "晚饭", "早餐", "午餐", "晚餐", "中餐", "宵夜", "夜宵",
        "吃饭", "饭", "奶茶", "咖啡", "饮品", "打车", "地铁", "公交",
        "话费", "电费", "水费", "房租", "超市", "水果",
        "花", "花了", "消费", "支出", "付了", "付款", "买单", "结账",
        "充了", "充值", "买", "买了", "购买", "用了",
        "工资", "奖金", "退款", "报销", "兼职", "红包", "收入", "分红",
        "补贴", "到账", "收到", "入账", "发工资", "理财", "收了"
    )

    fun containsAmount(rawInput: String): Boolean {
        val excludePatterns = listOf("几元", "多少钱", "几个", "多少块", "多钱")
        if (excludePatterns.any { rawInput.contains(it) }) return false
        if (rawInput.trim().matches(Regex("^[\\d.]+$"))) return true
        if (Regex("""(¥|￥)\s*\d+(\.\d+)?|\d+(\.\d+)?\s*[元块钱]""").containsMatchIn(rawInput)) return true
        if (Regex("""(花|花了|消费|支出|收入|赚了|充了|买了|用了|收到|到账|发工资|入账)\s*\d+(\.\d+)?""").containsMatchIn(rawInput)) return true

        // 极简记账句式兜底：中文语境词 + 裸数字，如「午饭12」「打车30」
        val bareNumberRe = Regex("""\d+(\.\d+)?""")
        if (bareNumberRe.containsMatchIn(rawInput) && accountingContextWords.any { rawInput.contains(it) }) {
            return true
        }
        return false
    }

    /** 单笔金额提取（保留兼容） */
    fun extractAmount(rawInput: String): Long? = extractAmountWithPos(rawInput)?.first

    /**
     * 多笔金额拆分。
     *
     * 节点3「金额提取」埋点：在方法入口打印原始文本、提取到的金额列表（含金额值+起始位置）、拆分后的片段列表。
     * 异常分支 catch 中调用 AppLogger.e() 打印异常。
     *
     * @param requestId 请求唯一ID（无默认值，调用方必须传入）
     */
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
            // 埋点：金额提取结果汇总
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

    /**
     * 片段清洗：去除分隔符、连接词，预处理多笔拆分描述片段。
     */
    fun cleanSegment(raw: String): String {
        var result = raw
        val separators = listOf(",", "，", "、", ";", "；", "。", ".", " ")
        val connectors = listOf("还有", "然后", "再", "加上", "另外", "以及")
        for (s in separators) result = result.replace(s, " ")
        for (c in connectors) result = result.replace(c, " ")
        return result.trim().replace(Regex("\\s+"), " ")
    }

    // ---- 私有方法 ----

    private fun extractAmountWithPos(rawInput: String): Triple<Long, Int, Int>? {
        // 口语 X块Y → 25块5
        val kuaiRe = Regex("""(\d+)\s*块\s*(\d+)""")
        val kuaiMatch = kuaiRe.find(rawInput)
        if (kuaiMatch != null) {
            val yuan = kuaiMatch.groupValues[1].toInt()
            val jiao = kuaiMatch.groupValues[2].toInt()
            return Triple((yuan * 100 + jiao * 10).toLong(), kuaiMatch.range.first, kuaiMatch.range.last + 1)
        }

        // ¥25 / ￥25
        val symbolRe = Regex("""(¥|￥)\s*(\d+(?:\.\d+)?)""")
        val symMatch = symbolRe.find(rawInput)
        if (symMatch != null) {
            return Triple((symMatch.groupValues[2].toDouble() * 100).toLong(), symMatch.range.first, symMatch.range.last + 1)
        }

        // 25元/25块/25.5元
        val unitRe = Regex("""(\d+(?:\.\d+)?)\s*[元块钱]""")
        val unitMatch = unitRe.find(rawInput)
        if (unitMatch != null) {
            return Triple((unitMatch.groupValues[1].toDouble() * 100).toLong(), unitMatch.range.first, unitMatch.range.last + 1)
        }

        // 花25 / 买了25 等
        val verbRe = Regex("""(?:花|花了|消费|支出|收入|赚了|充了|买了|用了|收到|到账|发工资|入账)\s*(\d+(?:\.\d+)?)""")
        val verbMatch = verbRe.find(rawInput)
        if (verbMatch != null) {
            // 只把数字部分当作金额，方向动词（如「收入」「花了」）保留在 textBefore 中，
            // 避免方向词在金额拆分阶段被吃掉，导致后续意图分流无法判定收支方向。
            val digitStart = verbMatch.groups[1]!!.range.first
            return Triple((verbMatch.groupValues[1].toDouble() * 100).toLong(), digitStart, verbMatch.range.last + 1)
        }

        // 极简句式兜底：裸数字紧跟在中文语境词后面，如「午饭12」「打车30」
        // 必须文本包含语境词且前面规则都没命中才到这里
        val bareRe = Regex("""(\d+(?:\.\d+)?)""")
        val bareMatch = bareRe.find(rawInput)
        if (bareMatch != null && accountingContextWords.any { rawInput.contains(it) }) {
            return Triple((bareMatch.groupValues[1].toDouble() * 100).toLong(), bareMatch.range.first, bareMatch.range.last + 1)
        }

        // 纯数字
        val trim = rawInput.trim()
        if (trim.matches(Regex("^[\\d.]+$"))) {
            return Triple((trim.toDouble() * 100).toLong(), 0, trim.length)
        }

        return null
    }
}
