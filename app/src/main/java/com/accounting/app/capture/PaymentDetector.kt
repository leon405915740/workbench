package com.accounting.app.capture

import android.view.accessibility.AccessibilityNodeInfo
import com.accounting.app.log.AppLogger

/**
 * 支付页面判定器（PaymentDetector）
 *
 * 通过关键词匹配判定当前页面是否为支付成功页面。
 *
 * 支持平台：
 * - 微信（com.tencent.mm）：付款成功、支付成功、已完成、收款
 * - 支付宝（com.eg.android.AlipayGphone）：支付成功、付款成功、交易成功、收款
 */
class PaymentDetector {

    companion object {
        private val WECHAT_KEYWORDS = listOf("付款成功", "支付成功", "已完成", "收款")
        private val ALIPAY_KEYWORDS = listOf("支付成功", "付款成功", "交易成功", "收款")

        // 合并所有关键词（用于商户黑名单）
        val ALL_KEYWORDS: Set<String> = (WECHAT_KEYWORDS + ALIPAY_KEYWORDS).toSet()

        fun keywordsFor(pkg: String?): List<String> {
            return when (pkg) {
                "com.tencent.mm" -> WECHAT_KEYWORDS
                "com.eg.android.AlipayGphone" -> ALIPAY_KEYWORDS
                else -> emptyList()
            }
        }
    }

    /**
     * 判定当前页面是否为支付成功页
     *
     * @param rootNode 根节点
     * @param pkg 当前包名
     * @param captureId 请求ID
     * @return true=是支付成功页，false=不是
     */
    fun isPaymentSuccessPage(
        rootNode: AccessibilityNodeInfo,
        pkg: String?,
        captureId: String
    ): Boolean {
        val keywords = keywordsFor(pkg)
        if (keywords.isEmpty()) {
            return false
        }

        val allText = collectAllText(rootNode)
        val matched = keywords.filter { keyword -> allText.contains(keyword) }

        return if (matched.isNotEmpty()) {
            AppLogger.d(captureId, "AutoCapture_Detector",
                "判定成功 pkg=$pkg, matchedKeywords=$matched, rawTextLength=${allText.length}")
            true
        } else {
            // DEBUG 级，避免每个非支付页都打日志
            AppLogger.d(captureId, "AutoCapture_Detector",
                "判定失败 pkg=$pkg, keywords=$keywords, rawTextLength=${allText.length}")
            false
        }
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
        AppLogger.d("", "PaymentDetector", "节点统计: 总数=$totalNodeCount, 含文本=$textNodeCount, 类型分布=$nodeStats")
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
            AppLogger.e("", "PaymentDetector", "节点遍历异常: ${e.message}", e)
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
            AppLogger.d("", "PaymentDetector", "WebView 提取失败: ${e.message}")
        }
    }
}
