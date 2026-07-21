package com.accounting.app.capture.model

import kotlinx.parcelize.Parcelize
import android.os.Parcelable

/**
 * 支付信息统一数据模型（PaymentInfo）
 *
 * 由 NodeExtractor（或未来的 OcrExtractor）生成。
 * 经 CaptureDispatcher 分发到 IntentRouter。
 *
 * 字段说明：
 * - source: 采集来源
 * - merchant: 商户名（可为 null，Validator 会拒绝 null）
 * - amount: 金额（单位：分，Long 类型）
 * - payTime: 支付时间（unix 时间戳，毫秒）
 * - paymentType: 支付类型（WECHAT / ALIPAY）
 * - confidence: 识别置信度（0~1）
 * - rawText: 节点原始文本（用于调试）
 * - captureId: 唯一采集ID（复用 AppLogger 的 requestId 字段）
 * - dedupKey: 用于分发去重（merchant_amount_payTime 哈希）
 */
@Parcelize
data class PaymentInfo(
    val source: CaptureSource,
    val merchant: String?,
    val amount: Long?,
    val payTime: Long?,
    val paymentType: String?,
    val confidence: Float,
    val rawText: String,
    val captureId: String,
    val extractedAt: Long = System.currentTimeMillis()
) : Parcelable {
    /**
     * 用于分发去重的 key
     *
     * **跨来源去重设计**：
     * - 无障碍从页面提取的 payTime 是支付时间（如 10:23:45）
     * - 通知的 payTime 是通知到达时间（如 10:24:02）
     * - 两者 payTime 不同会导致 dedupKey 不同，无法跨来源去重
     *
     * **解决方案**：dedupKey 只用 merchant + amount，依靠 60 秒时间窗口做时间维度去重
     * 对于个人记账场景，同一商户同一金额在 60 秒内重复的概率极低。
     *
     * null 安全处理：
     * - merchant 为 null 时使用 "unknown" 占位
     * - amount 为 null 时使用 0
     */
    val dedupKey: String
        get() = "${merchant ?: "unknown"}_${amount ?: 0}"
}
