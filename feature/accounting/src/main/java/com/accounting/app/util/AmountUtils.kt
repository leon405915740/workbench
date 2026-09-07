package com.accounting.app.util

import com.accounting.app.log.AppLogger
import java.math.RoundingMode

/**
 * 金额转换工具。
 *
 * 数据库统一以「分」为单位存储 Long，避免浮点误差；
 * 显示与输入时再在「元」与「分」之间转换。
 */
object AmountUtils {

    private const val CHINESE_INTEGER_PATTERN = "[零〇一二两三四五六七八九十百千万亿]+"
    private val chineseDigits = mapOf(
        '零' to 0L, '〇' to 0L, '一' to 1L, '二' to 2L, '两' to 2L,
        '三' to 3L, '四' to 4L, '五' to 5L, '六' to 6L,
        '七' to 7L, '八' to 8L, '九' to 9L
    )

    /** 金额+文本片段，用于多笔拆分 */
    data class AmountSegment(val amountFen: Long, val textBefore: String)

    fun fenToYuan(fen: Long): String = String.format("%.2f", fen / 100.0)
    fun fenToYuanWithSymbol(fen: Long): String = "¥${fenToYuan(fen)}"
    fun formatAmountWithSign(fen: Long): String = "¥${fenToYuan(fen)}"
    fun yuanToFen(yuan: String): Long = yuan.toBigDecimal()
        .movePointRight(2)
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact()

    private fun yuanToFenOrNull(yuan: String): Long? = runCatching { yuanToFen(yuan) }.getOrNull()
    private fun yuanToFenOrNull(yuan: Long): Long? = runCatching { Math.multiplyExact(yuan, 100L) }.getOrNull()

    /**
     * 从支付通知文本中提取单笔金额（分）。
     *
     * 支持「¥12.5」「￥12.5」「12.5元」「12.50」等通知常见格式。
     * 第三类仅匹配两位小数的裸数字（支付通知金额几乎都带两位小数），避免误吃订单号/日期。
     */
    fun extractFenFromAmountText(text: String): Long? {
        // 去掉千分位逗号（如 1,829.97 → 1829.97），否则 \d+ 会从逗号后开始匹配导致金额截断
        val normalized = text.replace(Regex("""(?<=\d),(?=\d)"""), "")
        Regex("""(¥|￥)\s*(\d+(?:\.\d+)?)""").find(normalized)?.let {
            return yuanToFenOrNull(it.groupValues[2])
        }
        Regex("""(\d+(?:\.\d+)?)\s*元""").find(normalized)?.let {
            return yuanToFenOrNull(it.groupValues[1])
        }
        Regex("""(\d+\.\d{2})""").find(normalized)?.let {
            return yuanToFenOrNull(it.groupValues[1])
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
        if (Regex("""$CHINESE_INTEGER_PATTERN\s*(?:元|块钱|块)""").containsMatchIn(rawInput)) return true
        if (Regex("""(?:花了|消费|支出|收入|赚了|充了|买了|用了|收到|到账|发工资|入账|花)\s*$CHINESE_INTEGER_PATTERN""").containsMatchIn(rawInput)) return true
        if (rawInput.trim().matches(Regex(CHINESE_INTEGER_PATTERN))) return true

        // 极简记账句式兜底：中文语境词 + 裸数字，如「午饭12」「打车30」
        val bareNumberRe = Regex("""\d+(\.\d+)?""")
        if (bareNumberRe.containsMatchIn(rawInput) && accountingContextWords.any { rawInput.contains(it) }) {
            return true
        }
        return false
    }

    /** 单笔金额提取（保留兼容） */
    fun extractAmount(rawInput: String): Long? = extractAmountWithPos(rawInput)?.first

    /** 映射与 AI 降级路径已确认是记账语境，可接受商户名后的无单位数字。 */
    internal fun extractLooseAmount(rawInput: String): Long? =
        extractAmountWithPos(rawInput, allowBareNumber = true)?.first

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
        val results = extractAmountsSilent(rawInput)
        // 埋点：金额提取结果汇总
        val amountsDesc = results.joinToString(", ") { "value=${it.amountFen}分" }
        val segmentsDesc = results.joinToString(", ") { "\"${it.textBefore}${it.amountFen}分\"" }
        AppLogger.d(
            requestId,
            "金额提取",
            "原始文本：$rawInput，提取金额：[$amountsDesc]，拆分片段：[$segmentsDesc]，共${results.size}笔"
        )
        return results
    }

    /**
     * 静默金额计数：复用 [extractAmounts] 的匹配逻辑但不打任何日志。
     *
     * 仅用于需要金额段数（上限）检查、却不想触发「金额提取」完整解析埋点的场景
     * （如记账入口的段数上限检查），避免同一输入被重复完整解析。
     */
    fun countAmounts(rawInput: String): Int = extractAmountsSilent(rawInput).size

    /** 金额拆分核心逻辑（不打日志）。提取成功返回数量等价的片段列表，供日志埋点与静默计数复用。 */
    private fun extractAmountsSilent(rawInput: String): List<AmountSegment> {
        val results = mutableListOf<AmountSegment>()
        var remaining = rawInput
        while (remaining.isNotEmpty()) {
            val match = extractAmountWithPos(remaining) ?: break
            val (amountFen, startIndex, endIndex) = match
            val textBefore = remaining.substring(0, startIndex).trim()
            results.add(AmountSegment(amountFen, textBefore))
            remaining = remaining.substring(endIndex).trimStart()
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
        return result.trim()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("^(今天|昨天|前天)\\s*"), "")
            .replace(Regex("(?<=\\S)\\s*花了$"), "")
            .trim()
    }

    /** ponytail: 本地兜底只解析中文整数；若产品接受“点/毛/分”，再扩展小数与角分。 */
    internal fun chineseIntegerToLong(raw: String): Long? = runCatching {
        if (raw.isEmpty()) return@runCatching null
        if (raw.all { chineseDigits.containsKey(it) }) {
            return@runCatching raw.fold(0L) { value, char ->
                Math.addExact(Math.multiplyExact(value, 10L), chineseDigits.getValue(char))
            }
        }
        var total = 0L
        var section = 0L
        var number: Long? = null
        var lastUnit = 1L
        var explicitZero = false
        for (char in raw) {
            val digit = chineseDigits[char]
            when {
                digit != null -> {
                    number = digit
                    if (digit == 0L) explicitZero = true
                }
                char == '十' || char == '百' || char == '千' -> {
                    val unit = when (char) {
                        '十' -> 10L
                        '百' -> 100L
                        else -> 1_000L
                    }
                    section = Math.addExact(
                        section,
                        Math.multiplyExact(number ?: 1L, unit)
                    )
                    number = null
                    lastUnit = unit
                    explicitZero = false
                }
                char == '万' -> {
                    section = Math.addExact(section, number ?: 0L)
                    if (section == 0L) return@runCatching null
                    total = Math.addExact(total, Math.multiplyExact(section, 10_000L))
                    section = 0L
                    number = null
                    lastUnit = 10_000L
                    explicitZero = false
                }
                char == '亿' -> {
                    section = Math.addExact(section, number ?: 0L)
                    if (total == 0L && section == 0L) return@runCatching null
                    total = Math.multiplyExact(Math.addExact(total, section), 100_000_000L)
                    section = 0L
                    number = null
                    lastUnit = 100_000_000L
                    explicitZero = false
                }
                else -> return@runCatching null
            }
        }
        val trailing = number?.let {
            if (!explicitZero && lastUnit >= 100L) Math.multiplyExact(it, lastUnit / 10L) else it
        } ?: 0L
        Math.addExact(Math.addExact(total, section), trailing)
    }.getOrNull()

    // ---- 私有方法 ----

    private fun extractAmountWithPos(
        rawInput: String,
        allowBareNumber: Boolean = false
    ): Triple<Long, Int, Int>? {
        // 含中文的 X块Y 角分速记尚未支持；整句拒绝，避免部分匹配成整数元。
        if (Regex(
                """(?:\d+\s*(?:块钱|块)\s*$CHINESE_INTEGER_PATTERN|""" +
                    """$CHINESE_INTEGER_PATTERN\s*(?:块钱|块)\s*(?:\d+|$CHINESE_INTEGER_PATTERN))"""
            ).containsMatchIn(rawInput)
        ) return null

        // 口语 X块Y：一位尾数按角、两位尾数按分；更多位视为无效输入。
        val kuaiRe = Regex("""(\d+)\s*(?:块钱|块)\s*(\d+)""")
        val kuaiMatch = kuaiRe.find(rawInput)
        if (kuaiMatch != null) {
            val fraction = kuaiMatch.groupValues[2]
            if (fraction.length > 2) return null
            val amountFen = yuanToFenOrNull("${kuaiMatch.groupValues[1]}.$fraction") ?: return null
            return Triple(amountFen, kuaiMatch.range.first, kuaiMatch.range.last + 1)
        }

        // ¥25 / ￥25
        val symbolRe = Regex("""(¥|￥)\s*(\d+(?:\.\d+)?)""")
        val symMatch = symbolRe.find(rawInput)
        if (symMatch != null) {
            val amountFen = yuanToFenOrNull(symMatch.groupValues[2]) ?: return null
            return Triple(amountFen, symMatch.range.first, symMatch.range.last + 1)
        }

        // 25元/25块/25.5元
        val unitRe = Regex("""(\d+(?:\.\d+)?)\s*(?:元|块钱|块)""")
        val unitMatch = unitRe.find(rawInput)
        if (unitMatch != null) {
            val amountFen = yuanToFenOrNull(unitMatch.groupValues[1]) ?: return null
            return Triple(amountFen, unitMatch.range.first, unitMatch.range.last + 1)
        }

        val chineseUnitMatch = Regex("""($CHINESE_INTEGER_PATTERN)\s*(?:元|块钱|块)""").find(rawInput)
        if (chineseUnitMatch != null) {
            val yuan = chineseIntegerToLong(chineseUnitMatch.groupValues[1]) ?: return null
            val amountFen = yuanToFenOrNull(yuan) ?: return null
            return Triple(amountFen, chineseUnitMatch.range.first, chineseUnitMatch.range.last + 1)
        }

        // 花25 / 买了25 等
        val verbRe = Regex("""(?:花|花了|消费|支出|收入|赚了|充了|买了|用了|收到|到账|发工资|入账)\s*(\d+(?:\.\d+)?)""")
        val verbMatch = verbRe.find(rawInput)
        if (verbMatch != null) {
            // 只把数字部分当作金额，方向动词（如「收入」「花了」）保留在 textBefore 中，
            // 避免方向词在金额拆分阶段被吃掉，导致后续意图分流无法判定收支方向。
            val digitStart = verbMatch.groups[1]!!.range.first
            val amountFen = yuanToFenOrNull(verbMatch.groupValues[1]) ?: return null
            return Triple(amountFen, digitStart, verbMatch.range.last + 1)
        }

        val chineseVerbMatch = Regex("""(?:花了|消费|支出|收入|赚了|充了|买了|用了|收到|到账|发工资|入账|花)\s*($CHINESE_INTEGER_PATTERN)""").find(rawInput)
        if (chineseVerbMatch != null) {
            val yuan = chineseIntegerToLong(chineseVerbMatch.groupValues[1]) ?: return null
            val amountStart = chineseVerbMatch.groups[1]!!.range.first
            val amountFen = yuanToFenOrNull(yuan) ?: return null
            return Triple(amountFen, amountStart, chineseVerbMatch.range.last + 1)
        }

        // 极简句式兜底：裸数字紧跟在中文语境词后面，如「午饭12」「打车30」
        // 必须文本包含语境词且前面规则都没命中才到这里
        val bareRe = Regex("""(\d+(?:\.\d+)?)""")
        val bareMatch = bareRe.find(rawInput)
        if (bareMatch != null && (allowBareNumber || accountingContextWords.any { rawInput.contains(it) })) {
            val amountFen = yuanToFenOrNull(bareMatch.groupValues[1]) ?: return null
            return Triple(amountFen, bareMatch.range.first, bareMatch.range.last + 1)
        }

        val chineseBareMatch = Regex(CHINESE_INTEGER_PATTERN).find(rawInput)
        if (chineseBareMatch != null &&
            accountingContextWords.any { rawInput.contains(it) } &&
            (chineseBareMatch.value.length > 1 || chineseBareMatch.value.any { it in "十百千万亿" })
        ) {
            val yuan = chineseIntegerToLong(chineseBareMatch.value) ?: return null
            val amountFen = yuanToFenOrNull(yuan) ?: return null
            return Triple(amountFen, chineseBareMatch.range.first, chineseBareMatch.range.last + 1)
        }

        // 纯数字
        val trim = rawInput.trim()
        yuanToFenOrNull(trim)?.let { return Triple(it, 0, trim.length) }
        if (trim.matches(Regex(CHINESE_INTEGER_PATTERN))) {
            val yuan = chineseIntegerToLong(trim) ?: return null
            val amountFen = yuanToFenOrNull(yuan) ?: return null
            return Triple(amountFen, 0, trim.length)
        }

        return null
    }
}
