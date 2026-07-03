package com.accounting.app.util

import android.util.Log
import com.accounting.app.BuildConfig

/**
 * 应用统一日志工具类（AppLogger）。
 *
 * 设计目标：
 * - 全局单例，统一收口所有日志打印入口
 * - 一次用户输入生成唯一 requestId，贯穿整条请求链路，可按 ID 精确检索
 * - 多笔拆分场景通过 billIndex 透传，区分同一 requestId 下的不同账单
 * - Release 包自动关闭日志，避免性能损耗与信息泄露
 * - API Key 等敏感信息调用 [maskApiKey] 显式脱敏
 * - 超长 message 自动截断，前缀（requestId / node / 笔序号）始终完整保留
 *
 * 规范约束：
 * - 业务代码禁止直接调用 [Log] 类，所有日志必须通过本类输出
 * - requestId 参数无默认值，调用方必须显式传入
 *
 * 零侵入：仅新增工具类，不修改任何业务逻辑。
 */
object AppLogger {

    /** 全局 Logcat 过滤 Tag */
    private const val TAG = "AccountingApp"

    /** 日志总开关：Debug 包开启，Release 包自动关闭 */
    private val enableLog = BuildConfig.DEBUG

    /** 单条日志 message 主体最大长度（不含前缀），超过自动截断 */
    private const val MAX_MESSAGE_LENGTH = 2000

    /**
     * 生成唯一请求ID。
     *
     * 格式：`req_时间戳_4位随机数`
     * 示例：`req_1720000000000_1234`
     *
     * 同一毫秒内多次调用通过随机数区分，保证唯一性。
     */
    fun generateRequestId(): String {
        return "req_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    // ===================== 调试日志 d =====================

    /** 调试日志（单笔场景） */
    fun d(requestId: String, node: String, message: String) {
        log(LogLevel.DEBUG, requestId, node, null, message, null)
    }

    /** 调试日志（多笔场景，自动追加 `[第N笔]`） */
    fun d(requestId: String, node: String, message: String, billIndex: Int) {
        log(LogLevel.DEBUG, requestId, node, null, message, billIndex)
    }

    // ===================== 错误日志 e =====================

    /** 错误日志（单笔场景） */
    fun e(requestId: String, node: String, message: String, throwable: Throwable?) {
        log(LogLevel.ERROR, requestId, node, null, message, null, throwable)
    }

    /** 错误日志（多笔场景，自动追加 `[第N笔]`，多笔失败可精准定位） */
    fun e(requestId: String, node: String, message: String, throwable: Throwable?, billIndex: Int) {
        log(LogLevel.ERROR, requestId, node, null, message, billIndex, throwable)
    }

    // ===================== 信息日志 i =====================

    /** 信息日志（单笔场景） */
    fun i(requestId: String, node: String, message: String) {
        log(LogLevel.INFO, requestId, node, null, message, null)
    }

    /** 信息日志（多笔场景，自动追加 `[第N笔]`） */
    fun i(requestId: String, node: String, message: String, billIndex: Int) {
        log(LogLevel.INFO, requestId, node, null, message, billIndex)
    }

    // ===================== 公开工具方法 =====================

    /**
     * 脱敏 API Key：保留前 4 位 + `****` + 后 4 位。
     *
     * 业务代码在打印 API Key 前必须显式调用本方法，禁止自行拼接脱敏字符串。
     *
     * 示例：`maskApiKey("sk-12345678abcdef")` → `"sk-12****cdef"`
     *
     * @param key 原始 API Key 字符串
     * @return 脱敏后的字符串；过短时返回 `****`
     */
    fun maskApiKey(key: String): String {
        if (key.length <= 8) return "****"
        return "${key.substring(0, 4)}****${key.substring(key.length - 4)}"
    }

    // ===================== 内部实现 =====================

    /** 日志级别枚举 */
    private enum class LogLevel { DEBUG, INFO, ERROR }

    /**
     * 统一日志输出入口。
     *
     * 流程：组装前缀 → message 主体截断 → 调用 Logcat → 调用 [saveLogToFile]（占位）
     *
     * 前缀格式：`[requestId] [node] [第N笔]`（多笔场景含笔序号）
     * 注意：前缀不计入 2000 字符长度限制，始终完整保留
     */
    private fun log(
        level: LogLevel,
        requestId: String,
        node: String,
        throwable: Throwable?,
        rawMessage: String,
        billIndex: Int? = null,
        extraThrowable: Throwable? = null
    ) {
        if (!enableLog) return

        val finalThrowable = throwable ?: extraThrowable

        // 组装前缀：[requestId] [node] [第N笔]
        val prefix = buildPrefix(requestId, node, billIndex)

        // message 主体截断（仅限主体，前缀不计入）
        val truncatedMessage = truncateText(rawMessage, MAX_MESSAGE_LENGTH)

        // 拼接最终日志文本
        val finalMessage = "$prefix $truncatedMessage"

        // 输出到 Logcat
        when (level) {
            LogLevel.DEBUG -> Log.d(TAG, finalMessage)
            LogLevel.INFO -> Log.i(TAG, finalMessage)
            LogLevel.ERROR -> Log.e(TAG, finalMessage, finalThrowable)
        }

        // 预留扩展位：本地日志文件输出（当前版本不实现）
        saveLogToFile(level.name, TAG, finalMessage, finalThrowable)
    }

    /** 组装日志前缀：`[requestId] [node]` 或 `[requestId] [node] [第N笔]` */
    private fun buildPrefix(requestId: String, node: String, billIndex: Int?): String {
        return if (billIndex != null) {
            "[$requestId] [$node] [第${billIndex}笔]"
        } else {
            "[$requestId] [$node]"
        }
    }

    /**
     * 截断超长文本（私有，仅由日志方法内部自动调用，业务代码不可见）。
     *
     * 截断规则：
     * - 仅对 text 主体截断，不影响调用方传入的 requestId/node/笔序号前缀
     * - 超过 maxLen 时截断到 maxLen 字符并追加 `...(已截断)`
     * - 不抛异常
     */
    private fun truncateText(text: String, maxLen: Int): String {
        if (text.length <= maxLen) return text
        return text.substring(0, maxLen) + "...(已截断)"
    }

    /**
     * 预留扩展位：本地日志文件输出。
     *
     * 当前版本不实现，仅留空方法占位。
     * 后续可在此处实现：写入应用私有目录的 .log 文件、用户反馈收集等。
     *
     * 约束：
     * - 不抛异常，不影响主流程
     * - 当前为 no-op
     */
    @Suppress("UNUSED_PARAMETER")
    private fun saveLogToFile(level: String, tag: String, msg: String, throwable: Throwable?) {
        // 预留扩展位：本地日志文件输出（当前版本不实现）
    }
}
