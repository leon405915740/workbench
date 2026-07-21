package com.accounting.app.capture.extractor

import com.accounting.app.capture.model.CaptureSource
import com.accounting.app.capture.model.PaymentInfo
import com.accounting.app.log.AppLogger
import com.accounting.app.parser.time.TimeUtils
import java.math.BigDecimal

/**
 * 通知文本解析器
 *
 * 从支付通知文本中提取金额、商户等信息。
 * 支持微信支付、支付宝、部分银行APP通知格式。
 */
class NotificationExtractor {

    /**
     * 从通知文本中提取支付信息
     *
     * @param title 通知标题
     * @param text 通知内容
     * @param packageName 发送通知的包名
     * @param captureId 采集ID（用于日志串联）
     * @return PaymentInfo 或 null（解析失败）
     */
    fun extract(
        title: String?,
        text: String?,
        packageName: String,
        captureId: String
    ): PaymentInfo? {
        val fullText = buildString {
            if (!title.isNullOrBlank()) append(title)
            if (!text.isNullOrBlank()) {
                if (isNotBlank()) append(" ")
                append(text)
            }
        }

        if (fullText.isBlank()) {
            AppLogger.w(captureId, "NotificationExtractor", "通知文本为空")
            return null
        }

        val amount = extractAmount(fullText, captureId)
        if (amount == null) {
            AppLogger.d(captureId, "NotificationExtractor", "未提取到金额，text=$fullText")
            return null
        }

        val merchant = extractMerchant(fullText, packageName, captureId)
        val paymentType = mapPackageToPaymentType(packageName)

        val info = PaymentInfo(
            source = CaptureSource.NOTIFICATION,
            merchant = merchant,
            amount = amount,
            payTime = TimeUtils.now(),
            paymentType = paymentType,
            confidence = 0.8f,
            rawText = fullText,
            captureId = captureId
        )

        if (!validate(info)) {
            AppLogger.w(captureId, "NotificationExtractor", "校验失败，amount=$amount, merchant=$merchant")
            return null
        }

        AppLogger.i(captureId, "NotificationExtractor", "解析成功，merchant=$merchant, amount=$amount, source=$packageName")
        return info
    }

    /**
     * 从文本中提取金额
     *
     * 匹配格式：
     * - ¥26.80 / ￥26.80
     * - 26.80元 / 26元
     * - 金额：26.80 / 支付金额26.80
     */
    private fun extractAmount(text: String, captureId: String): Long? {
        val patterns = listOf(
            Regex("""[¥￥]\s*(\d+\.?\d*)"""),
            Regex("""(\d+\.?\d*)\s*元"""),
            Regex("""(?:金额|支付金额|交易金额|消费金额|支出)[：:]*\s*[¥￥]?\s*(\d+\.?\d*)"""),
            Regex("""(?:消费|支出|支付)[：:]*\s*[¥￥]?\s*(\d+\.?\d*)""")
        )

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                val amountStr = match.groupValues[1]
                val amountYuan = amountStr.toDoubleOrNull()
                if (amountYuan != null && amountYuan > 0) {
                    val amountFen = BigDecimal(amountYuan.toString()).multiply(BigDecimal(100)).toLong()
                    AppLogger.d(captureId, "NotificationExtractor", "提取金额：$amountYuan 元 (pattern=$pattern)")
                    return amountFen
                }
            }
        }

        return null
    }

    /**
     * 从文本中提取商户名
     *
     * 策略：
     * 1. 匹配商户关键词后的文本："商户：喜茶" → "喜茶"
     * 2. 银行通知中没有商户时，使用包名映射的默认商户名
     */
    private fun extractMerchant(text: String, packageName: String, captureId: String): String? {
        val merchantPatterns = listOf(
            Regex("""(?:商户|商家|收款方|付款给|支付给)[：:]*\s*([^\s,，]+)"""),
            Regex("""(?:向|付给)([^\s,，]+)(?:支付|付款)""")
        )

        for (pattern in merchantPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                val merchant = match.groupValues[1].trim()
                if (merchant.isNotBlank() && merchant.length <= 20) {
                    AppLogger.d(captureId, "NotificationExtractor", "提取商户：$merchant (pattern=$pattern)")
                    return merchant
                }
            }
        }

        // 银行通知可能没有商户，使用包名映射
        val fallbackMerchant = mapPackageToMerchant(packageName)
        if (fallbackMerchant != null) {
            AppLogger.d(captureId, "NotificationExtractor", "商户未匹配，使用包名映射：$fallbackMerchant")
        }
        return fallbackMerchant
    }

    /**
     * 包名映射到支付平台
     */
    private fun mapPackageToPaymentType(packageName: String): String? {
        return when {
            packageName.contains("tencent.mm") -> "WECHAT"
            packageName.contains("alipay") -> "ALIPAY"
            packageName.contains("cmb.pb") -> "BANK_CMB"
            packageName.contains("chinamworld.main") -> "BANK_CCB"
            packageName.contains("icbc") -> "BANK_ICBC"
            else -> null
        }
    }

    /**
     * 包名映射到默认商户名（用于银行通知）
     */
    private fun mapPackageToMerchant(packageName: String): String? {
        return when {
            packageName.contains("tencent.mm") -> "微信支付"
            packageName.contains("alipay") -> "支付宝"
            packageName.contains("cmb.pb") -> "招商银行"
            packageName.contains("chinamworld.main") -> "建设银行"
            packageName.contains("icbc") -> "工商银行"
            else -> null
        }
    }

    /**
     * 二次校验（避免误识别）
     *
     * - 金额范围：0.01 ~ 10000 元
     * - 商户非空
     */
    private fun validate(info: PaymentInfo): Boolean {
        val amount = info.amount ?: return false
        val merchant = info.merchant

        // 金额范围：1分 ~ 1000000分（10000元）
        if (amount < 1 || amount > 1_000_000) {
            return false
        }

        // 商户不能为空（或者至少有个默认值）
        if (merchant.isNullOrBlank()) {
            return false
        }

        return true
    }
}