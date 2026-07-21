package com.accounting.app.log

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.accounting.app.BuildConfig
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

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
 * - 日志自动写入本地文件，支持导出分享
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

    /** 日志文件目录名 */
    private const val LOG_DIR_NAME = "logs"

    /** 日志文件名前缀 */
    private const val LOG_FILE_PREFIX = "accounting_"

    /** 日志文件名后缀 */
    private const val LOG_FILE_SUFFIX = ".log"

    /** 保留日志天数 */
    private const val RETAIN_DAYS = 7

    /** 单文件最大大小（5MB） */
    private const val MAX_FILE_SIZE = 5 * 1024 * 1024L

    /** 应用 Context，延迟初始化 */
    private lateinit var appContext: Context

    /** 日志写入线程池（单线程，保证顺序） */
    private val logExecutor = Executors.newSingleThreadExecutor()

    /** UI 线程 Handler */
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * 初始化日志系统，必须在 Application onCreate 中调用。
     */
    fun init(context: Context) {
        appContext = context.applicationContext
        cleanOldLogs()
    }

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

    // ===================== 警告日志 w =====================

    /** 警告日志（单笔场景） */
    fun w(requestId: String, node: String, message: String) {
        log(LogLevel.WARN, requestId, node, null, message, null)
    }

    /** 警告日志（多笔场景，自动追加 `[第N笔]`） */
    fun w(requestId: String, node: String, message: String, billIndex: Int) {
        log(LogLevel.WARN, requestId, node, null, message, billIndex)
    }

    // ===================== 决策日志（DecisionLog） =====================

    /**
     * 决策日志：记录"为什么这样判断"
     *
     * @param requestId 请求ID
     * @param node 决策节点（如"分类匹配"、"意图路由"）
     * @param keyword 触发关键词（可为null）
     * @param reason 决策原因描述
     * @param confidence 置信度（0~1，可为null）
     * @param source 决策来源（如"mapping"、"rule"、"ai"、"fallback"）
     * @param billIndex 账单序号（多笔场景）
     */
    fun decision(
        requestId: String,
        node: String,
        keyword: String?,
        reason: String,
        confidence: Float? = null,
        source: String? = null,
        billIndex: Int? = null
    ) {
        val confidenceStr = confidence?.let { String.format("%.2f", it) } ?: "null"
        val keywordStr = keyword ?: "null"
        val sourceStr = source ?: "null"
        val message = "keyword:$keywordStr, reason:$reason, confidence:$confidenceStr, source:$sourceStr"
        log(LogLevel.INFO, requestId, "决策日志[$node]", null, message, billIndex)
    }

    /**
     * 决策日志（简化版，无账单序号）
     */
    fun decision(
        requestId: String,
        node: String,
        keyword: String?,
        reason: String,
        confidence: Float? = null,
        source: String? = null
    ) {
        decision(requestId, node, keyword, reason, confidence, source, null)
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

    /**
     * 统一日志脱敏方法：对敏感信息进行正则替换。
     *
     * 脱敏规则：
     * - 手机号（1[3-9] 开头 11 位）：138****1234
     * - 银行卡号（16~19 位纯数字）：**** **** **** 1234
     * - 金额（数字 + 分/元单位）：***
     *
     * 该方法由 [log] 内部自动调用，业务代码无需手动调用。
     *
     * @param message 原始日志消息
     * @return 脱敏后的日志消息
     */
    fun sanitizeLog(message: String): String {
        var result = message
        // 1. 手机号脱敏：1[3-9] 开头 11 位，保留前3后4
        result = PHONE_REGEX.replace(result) { match ->
            val s = match.value
            "${s.substring(0, 3)}****${s.substring(7)}"
        }
        // 2. 银行卡号脱敏：16~19 位纯数字，保留后4位
        result = BANK_CARD_REGEX.replace(result) { match ->
            val s = match.value
            "**** **** **** ${s.substring(s.length - 4)}"
        }
        // 3. 金额脱敏：数字（含小数）+ 分/元单位
        result = AMOUNT_REGEX.replace(result) { "***" }
        return result
    }

    /** 手机号正则：1[3-9] 开头，后跟 9 位数字 */
    private val PHONE_REGEX = Regex("1[3-9]\\d{9}")

    /** 银行卡号正则：16~19 位纯数字（前后非数字边界防止误匹配） */
    private val BANK_CARD_REGEX = Regex("(?<!\\d)\\d{16,19}(?!\\d)")

    /** 金额正则：整数或两位小数 + 分/元单位 */
    private val AMOUNT_REGEX = Regex("\\d+(\\.\\d{1,2})?(分|元)")

    // ===================== 内部实现 =====================

    /** 日志级别枚举 */
    private enum class LogLevel { DEBUG, INFO, WARN, ERROR }

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

        // 日志脱敏：对 message 进行敏感信息替换
        val sanitizedMessage = sanitizeLog(rawMessage)

        // 组装前缀：[requestId] [node] [第N笔]
        val prefix = buildPrefix(requestId, node, billIndex)

        // message 主体截断（仅限主体，前缀不计入）
        val truncatedMessage = truncateText(sanitizedMessage, MAX_MESSAGE_LENGTH)

        // 拼接最终日志文本
        val finalMessage = "$prefix $truncatedMessage"

        // 输出到 Logcat
        when (level) {
            LogLevel.DEBUG -> Log.d(TAG, finalMessage)
            LogLevel.INFO -> Log.i(TAG, finalMessage)
            LogLevel.WARN -> Log.w(TAG, finalMessage)
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
     * 将日志写入本地文件。
     *
     * 写入规则：
     * - 异步写入，不阻塞主线程
     * - 按日期分割文件，格式：accounting_yyyyMMdd.log
     * - 单文件超过 5MB 自动切换新文件
     * - 自动清理超过 7 天的日志文件
     * - 异常静默处理，不影响主流程
     */
    private fun saveLogToFile(level: String, tag: String, msg: String, throwable: Throwable?) {
        if (!::appContext.isInitialized) return
        logExecutor.execute {
            try {
                val logFile = getCurrentLogFile()
                val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.CHINA).format(Date())
                val logLine = "$timeStr [$level] $tag: $msg\n"

                FileWriter(logFile, true).use { writer ->
                    writer.write(logLine)
                    if (throwable != null) {
                        val printWriter = PrintWriter(writer)
                        throwable.printStackTrace(printWriter)
                        printWriter.flush()
                    }
                }
            } catch (e: Exception) {
                // 日志写入失败不影响主流程，静默处理
            }
        }
    }

    /**
     * 获取当前日志文件。
     * 按日期分割，单文件超过 5MB 自动切换。
     */
    private fun getCurrentLogFile(): File {
        val logDir = File(appContext.filesDir, LOG_DIR_NAME)
        if (!logDir.exists()) {
            logDir.mkdirs()
        }

        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.CHINA)
        val dateStr = dateFormat.format(Date())
        var fileName = "$LOG_FILE_PREFIX$dateStr$LOG_FILE_SUFFIX"
        var logFile = File(logDir, fileName)

        var counter = 1
        while (logFile.exists() && logFile.length() >= MAX_FILE_SIZE) {
            fileName = "$LOG_FILE_PREFIX${dateStr}_$counter$LOG_FILE_SUFFIX"
            logFile = File(logDir, fileName)
            counter++
        }
        return logFile
    }

    /**
     * 清理超过 RETAIN_DAYS 天的日志文件。
     */
    private fun cleanOldLogs() {
        logExecutor.execute {
            try {
                val logDir = File(appContext.filesDir, LOG_DIR_NAME)
                if (!logDir.exists()) return@execute

                val cutoffTime = System.currentTimeMillis() - RETAIN_DAYS * 24 * 60 * 60 * 1000L
                logDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.lastModified() < cutoffTime) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                // 清理失败不影响主流程
            }
        }
    }

    // ===================== 日志导出 API =====================

    /**
     * 获取所有日志文件列表。
     * @return 日志文件列表，按修改时间降序排列
     */
    fun getLogFiles(): List<File> {
        if (!::appContext.isInitialized) return emptyList()
        return try {
            val logDir = File(appContext.filesDir, LOG_DIR_NAME)
            if (!logDir.exists()) return emptyList()
            logDir.listFiles { file -> file.isFile && file.name.endsWith(LOG_FILE_SUFFIX) }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 获取最新的日志文件。
     * @return 最新的日志文件，如果不存在返回 null
     */
    fun getLatestLogFile(): File? {
        return getLogFiles().firstOrNull()
    }

    /**
     * 清空所有日志文件。
     */
    fun clearAllLogs() {
        logExecutor.execute {
            try {
                val logDir = File(appContext.filesDir, LOG_DIR_NAME)
                if (!logDir.exists()) return@execute
                logDir.listFiles()?.forEach { file ->
                    if (file.isFile) file.delete()
                }
            } catch (e: Exception) {
                // 清空失败不影响主流程
            }
        }
    }

    /**
     * 获取日志目录路径。
     */
    fun getLogDirPath(): String {
        return if (::appContext.isInitialized) {
            File(appContext.filesDir, LOG_DIR_NAME).absolutePath
        } else {
            ""
        }
    }

    /**
     * 将所有日志文件合并为一个临时文件，用于导出。
     * 按修改时间升序排列（旧日志在前，新日志在后）。
     *
     * @return 合并后的临时文件，如果没有日志返回 null
     */
    fun getMergedLogFile(): File? {
        if (!::appContext.isInitialized) return null
        return try {
            val logFiles = getLogFiles().sortedBy { it.lastModified() }
            if (logFiles.isEmpty()) return null

            val mergedFile = File(appContext.cacheDir, "accounting_merged_${System.currentTimeMillis()}.log")
            FileWriter(mergedFile, true).use { writer ->
                for ((index, file) in logFiles.withIndex()) {
                    if (index > 0) {
                        writer.write("\n\n========== ${file.name} ==========\n\n")
                    }
                    file.forEachLine { line ->
                        writer.write(line)
                        writer.write("\n")
                    }
                }
                writer.flush()
            }
            mergedFile
        } catch (e: Exception) {
            null
        }
    }
}