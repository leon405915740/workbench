package com.accounting.app.capture.extractor

import android.view.accessibility.AccessibilityNodeInfo
import com.accounting.app.capture.PaymentDetector
import com.accounting.app.capture.model.PaymentInfo
import com.accounting.app.capture.model.CaptureSource
import com.accounting.app.log.AppLogger
import java.math.BigDecimal
import java.util.regex.Pattern

class NodeExtractor {

    private val amountPattern = Pattern.compile("[¥￥]?\\d+\\.?\\d*")
    private val datePattern = Pattern.compile("\\d{4}[-/年]\\d{1,2}[-/月]\\d{1,2}\\s*\\d{1,2}:\\d{2}")

    private val WECHAT_MERCHANT_LABELS = listOf("商户", "收款方", "商家", "收款商户")
    private val ALIPAY_MERCHANT_LABELS = listOf("商户", "收款方", "商家", "对方账户")

    /**
     * 从无障碍节点树提取支付信息（主入口）
     */
    fun extract(rootNode: AccessibilityNodeInfo, pkg: String, captureId: String): PaymentInfo? {
        val startTime = System.currentTimeMillis()

        val allText = collectAllText(rootNode)
        val result = extractFromText(allText, pkg, captureId)

        val durationMs = System.currentTimeMillis() - startTime
        if (result != null) {
            AppLogger.d(captureId, "AutoCapture_Extractor",
                "节点解析成功 merchant=${result.merchant}, amount=${result.amount}, durationMs=$durationMs")
        }

        return result
    }

    /**
     * 从文本中提取支付信息（供单元测试调用）
     */
    fun extractFromText(rawText: String, pkg: String, captureId: String): PaymentInfo? {
        if (rawText.isBlank()) {
            AppLogger.d(captureId, "AutoCapture_Extractor", "原始文本为空")
            return null
        }

        val lines = rawText.lines()
            .filter { it.isNotBlank() }
            .map { it.trim() }

        val amount = extractAmount(lines) ?: run {
            AppLogger.d(captureId, "AutoCapture_Extractor", "未提取到金额")
            return null
        }

        val merchant = extractMerchant(lines, pkg)
        val payTime = extractPayTime(lines)
        val paymentType = when (pkg) {
            "com.tencent.mm" -> "WECHAT"
            "com.eg.android.AlipayGphone" -> "ALIPAY"
            else -> null
        }

        val info = PaymentInfo(
            source = CaptureSource.ACCESSIBILITY,
            merchant = merchant,
            amount = amount,
            payTime = payTime,
            paymentType = paymentType,
            confidence = calculateConfidence(merchant != null, payTime != null),
            rawText = rawText,
            captureId = captureId
        )

        if (!validate(info)) {
            AppLogger.d(captureId, "AutoCapture_Extractor", "校验失败，丢弃")
            return null
        }

        if (merchant == null) {
            AppLogger.d(captureId, "AutoCapture_Extractor", "部分成功：缺少商户")
        }

        return info
    }

    /**
     * 提取金额（最接近"成功"关键词的数值）
     */
    private fun extractAmount(lines: List<String>): Long? {
        var successLineIndex = -1
        lines.forEachIndexed { index, line ->
            if (line.contains("成功") || line.contains("完成") || line.contains("收款")) {
                successLineIndex = index
            }
        }

        var bestAmount: Long? = null
        var bestDistance = Int.MAX_VALUE

        for ((index, line) in lines.withIndex()) {
            val matcher = amountPattern.matcher(line)
            while (matcher.find()) {
                val str = matcher.group()
                try {
                    val cleanStr = str.replace("[¥￥]".toRegex(), "")
                    val value = BigDecimal(cleanStr).multiply(BigDecimal(100)).toLong()

                    if (value > 0 && value <= 10_000_00) {
                        val distance = if (successLineIndex >= 0) {
                            Math.abs(index - successLineIndex)
                        } else {
                            index
                        }

                        if (distance < bestDistance) {
                            bestDistance = distance
                            bestAmount = value
                        }
                    }
                } catch (e: NumberFormatException) {
                    AppLogger.e("", "NodeExtractor", "金额转换失败: ${e.message}", e)
                }
            }
        }

        return bestAmount
    }

    /**
     * 提取商户名
     */
    private fun extractMerchant(lines: List<String>, pkg: String): String? {
        val merchantLabels = when (pkg) {
            "com.tencent.mm" -> WECHAT_MERCHANT_LABELS
            "com.eg.android.AlipayGphone" -> ALIPAY_MERCHANT_LABELS
            else -> emptyList()
        }

        for ((index, line) in lines.withIndex()) {
            for (label in merchantLabels) {
                if (line.contains(label)) {
                    if (index + 1 < lines.size) {
                        val nextLine = lines[index + 1]
                        if (nextLine.isNotBlank() && !isAmount(nextLine) && !isSuccessKeyword(nextLine)) {
                            return nextLine
                        }
                    }
                }
            }
        }

        val successLineIndex = lines.indexOfFirst {
            it.contains("成功") || it.contains("完成")
        }

        if (successLineIndex > 0) {
            val prevLine = lines[successLineIndex - 1]
            if (!isAmount(prevLine) && !isSuccessKeyword(prevLine)) {
                return prevLine
            }
        }

        for (line in lines) {
            if (line.isNotBlank() && !isAmount(line) && !isSuccessKeyword(line) && !isDate(line)) {
                return line
            }
        }

        return null
    }

    /**
     * 提取支付时间
     */
    private fun extractPayTime(lines: List<String>): Long? {
        for (line in lines) {
            val matcher = datePattern.matcher(line)
            if (matcher.find()) {
                try {
                    return parseDateTime(matcher.group())
                } catch (e: Exception) {
                    AppLogger.e("", "NodeExtractor", "支付时间解析失败: ${e.message}", e)
                }
            }
        }
        return null
    }

    /**
     * 解析日期时间字符串为 Unix 时间戳
     */
    private fun parseDateTime(dateTimeStr: String): Long {
        var s = dateTimeStr.replace("年", "-").replace("月", "-").replace("日", "")
        try {
            val parts = s.split(Regex("[-/\\s]"))
            if (parts.size >= 5) {
                val year = parts[0].toInt()
                val month = parts[1].toInt()
                val day = parts[2].toInt()
                val hour = parts[3].toInt()
                val minute = parts[4].toInt()

                return java.time.LocalDateTime.of(year, month, day, hour, minute)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            }
        } catch (e: Exception) {
            AppLogger.e("", "NodeExtractor", "日期时间解析失败: ${e.message}", e)
        }
        return System.currentTimeMillis()
    }

    /**
     * 计算置信度
     */
    private fun calculateConfidence(hasMerchant: Boolean, hasTime: Boolean): Float {
        var score = 0.5f
        if (hasMerchant) score += 0.3f
        if (hasTime) score += 0.2f
        return score
    }

    /**
     * 校验 PaymentInfo（NodeExtractor 内部私有方法，不对外暴露）
     */
    fun validate(info: PaymentInfo): Boolean {
        val now = System.currentTimeMillis()
        return info.amount != null && info.amount in 1L..10_000_00L
            && info.payTime.let { it == null || (it in now - 600000..now + 60000) }
            && info.merchant?.let { it.isNotBlank() && it !in PaymentDetector.ALL_KEYWORDS } ?: false
    }

    /**
     * 判断是否为金额字符串
     */
    private fun isAmount(line: String): Boolean {
        return amountPattern.matcher(line).find()
    }

    /**
     * 判断是否为成功关键词
     */
    private fun isSuccessKeyword(line: String): Boolean {
        return PaymentDetector.ALL_KEYWORDS.any { line.contains(it) }
    }

    /**
     * 判断是否为日期字符串
     */
    private fun isDate(line: String): Boolean {
        return datePattern.matcher(line).find()
    }

    /**
     * 递归遍历节点树，拼接所有可见文本
     */
    private fun collectAllText(rootNode: AccessibilityNodeInfo): String {
        val sb = StringBuilder()
        val nodeStats = mutableMapOf<String, Int>().withDefault { 0 }
        var textNodeCount = 0
        var totalNodeCount = 0
        collectAllTextRecursive(rootNode, sb, nodeStats, { textNodeCount++ }, { totalNodeCount++ })
        AppLogger.d("", "NodeExtractor", "节点统计: 总数=$totalNodeCount, 含文本=$textNodeCount, 类型分布=$nodeStats")
        return sb.toString()
    }

    private fun collectAllTextRecursive(
        node: AccessibilityNodeInfo,
        sb: StringBuilder,
        nodeStats: MutableMap<String, Int>,
        onTextNode: () -> Unit,
        onNodeVisit: () -> Unit
    ) {
        try {
            onNodeVisit()
            val className = node.className?.toString() ?: "Unknown"
            nodeStats[className] = nodeStats[className]!! + 1

            val isVisible = try {
                node.isVisibleToUser
            } catch (e: Exception) {
                true
            }

            if (isVisible) {
                val text = node.text?.toString()
                if (!text.isNullOrBlank()) {
                    onTextNode()
                    sb.append(text).append("\n")
                }

                val contentDesc = node.contentDescription?.toString()
                if (!contentDesc.isNullOrBlank() && contentDesc != text) {
                    if (text.isNullOrBlank()) onTextNode()
                    sb.append(contentDesc).append("\n")
                }

                if (className.contains("WebView")) {
                    extractFromWebView(node, sb, onTextNode)
                }
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                try {
                    collectAllTextRecursive(child, sb, nodeStats, onTextNode, onNodeVisit)
                } finally {
                    child.recycle()
                }
            }
        } catch (e: Exception) {
            AppLogger.e("", "NodeExtractor", "节点文本采集失败: ${e.message}", e)
        }
    }

    private fun extractFromWebView(node: AccessibilityNodeInfo, sb: StringBuilder, onTextNode: () -> Unit) {
        try {
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                try {
                    val text = child.text?.toString()
                    if (!text.isNullOrBlank()) {
                        onTextNode()
                        sb.append(text).append("\n")
                    }
                    val contentDesc = child.contentDescription?.toString()
                    if (!contentDesc.isNullOrBlank() && contentDesc != text) {
                        if (text.isNullOrBlank()) onTextNode()
                        sb.append(contentDesc).append("\n")
                    }
                } finally {
                    child.recycle()
                }
            }
        } catch (e: Exception) {
            AppLogger.d("", "NodeExtractor", "WebView 提取失败: ${e.message}")
        }
    }
}
