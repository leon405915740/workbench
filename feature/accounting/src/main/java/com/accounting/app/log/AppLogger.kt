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
import java.util.LinkedHashMap
import java.util.Locale
import java.util.concurrent.Executors

/**
 * 应用统一日志工具类（AppLogger）。
 *
 * 设计目标：
 * - 全局单例，统一收口所有日志打印入口
 * - 一次用户输入生成唯一 requestId，贯穿整条请求链路，可按 ID 精确检索
 * - 多笔拆分场景通过 billIndex 透传，区分同一 requestId 下的不同账单
 * - Release 包默认关闭"详细日志"（DEBUG/INFO/WARN），但 ERROR 与 CRASH 始终落盘，可设置页开启详细日志
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

    /** 详细日志开关（DEBUG/INFO/WARN/决策日志）：默认跟随构建类型，可在设置页运行时切换并持久化 */
    @Volatile
    private var debugLogEnabled = BuildConfig.DEBUG

    /** 日志开关持久化 Key */
    private const val PREFS_NAME = "app_logger_prefs"
    private const val KEY_DEBUG_LOG = "debug_log_enabled"

    /** 单条日志 message 主体最大长度（不含前缀），超过自动截断 */
    private const val MAX_MESSAGE_LENGTH = 2000

    /**
     * 重复日志抑制固定时间窗（毫秒）。
     * 以第一条匹配指纹的日志调用时刻为窗口起点，窗口不随每条日志滑动续期。
     */
    private const val WINDOW_MS = 500L

    /** 重复日志抑制指纹 LRU 缓存上限（条），超限按访问顺序淘汰最久未命中的指纹 */
    private const val FINGERPRINT_CACHE_SIZE = 256

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
        // 读取持久化的日志开关（默认跟随构建类型）
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        debugLogEnabled = prefs.getBoolean(KEY_DEBUG_LOG, BuildConfig.DEBUG)
        cleanOldLogs()
    }

    /**
     * 设置详细日志开关（运行时切换，立即持久化）。
     * 注意：ERROR 与 CRASH 级别不受此开关影响，始终写入文件。
     */
    fun setDebugLogEnabled(enabled: Boolean) {
        val wasEnabled = debugLogEnabled
        debugLogEnabled = enabled
        // debugLogEnabled 关闭→重新开启时清空指纹抑制缓存，重建
        if (enabled && !wasEnabled) {
            synchronized(throttleLock) {
                fingerprintCache.clear()
            }
        }
        if (::appContext.isInitialized) {
            appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_DEBUG_LOG, enabled)
                .apply()
        }
    }

    /** 当前详细日志是否开启 */
    fun isDebugLogEnabled(): Boolean = debugLogEnabled

    /**
     * 记录未捕获崩溃。始终写入日志文件（绕过详细日志开关），
     * 便于 Release 包在崩溃后通过日志导出排查问题。
     *
     * @param throwable 崩溃异常
     * @param extraInfo 可选的上下文信息（如当前页面、用户操作）
     */
    fun logCrash(throwable: Throwable, extraInfo: String = "") {
        if (!::appContext.isInitialized) return
        val header = buildString {
            append("应用崩溃捕获: ${throwable.javaClass.name}: ${throwable.message ?: "无消息"}")
            if (extraInfo.isNotBlank()) append("\n上下文: $extraInfo")
        }
        saveLogToFile("CRASH", TAG, header, throwable)
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
        // 决策日志不参与重复抑制（bypassThrottle=true），node 直接用原值
        log(LogLevel.INFO, requestId, node, null, message, billIndex, bypassThrottle = true)
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

    /** 重复日志抑制的窗口计数条目 */
    private data class ThrottleEntry(var windowStart: Long, var count: Int)

    /** 指纹抑制计数同步锁（调用线程同步，仅最终文件写入投递到 logExecutor） */
    private val throttleLock = Any()

    /** 指纹 → 窗口计数 LRU 缓存，accessOrder=true，超限淘汰最久未命中的指纹 */
    private val fingerprintCache = object :
        LinkedHashMap<String, ThrottleEntry>(FINGERPRINT_CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ThrottleEntry>?): Boolean {
            return size > FINGERPRINT_CACHE_SIZE
        }
    }

    /**
     * 统一日志输出入口。
     *
     * 流程：组装前缀 → message 主体截断 → 调用 Logcat → 重复抑制判断 → 调用 [saveLogToFile]
     *
     * 前缀格式：`[requestId] [node] [第N笔]`（多笔场景含笔序号）
     * 注意：前缀不计入 2000 字符长度限制，始终完整保留
     *
     * @param bypassThrottle 为 true 时不做重复抑制（决策日志 decision 使用），Logcat 与文件均原样输出
     */
    private fun log(
        level: LogLevel,
        requestId: String,
        node: String,
        throwable: Throwable?,
        rawMessage: String,
        billIndex: Int? = null,
        extraThrowable: Throwable? = null,
        bypassThrottle: Boolean = false
    ) {
        // ERROR 级别始终写入（即使 Release 包关闭了详细日志），保证崩溃/关键错误可排查
        val alwaysWrite = level == LogLevel.ERROR
        if (!debugLogEnabled && !alwaysWrite) return

        val finalThrowable = throwable ?: extraThrowable

        // 日志脱敏：对 message 进行敏感信息替换
        val sanitizedMessage = sanitizeLog(rawMessage)

        // 组装前缀：[requestId] [node] [第N笔]
        val prefix = buildPrefix(requestId, node, billIndex)

        // message 主体截断（仅限主体，前缀不计入）
        val truncatedMessage = truncateText(sanitizedMessage, MAX_MESSAGE_LENGTH)

        // 拼接最终日志文本
        val finalMessage = "$prefix $truncatedMessage"

        // 输出到 Logcat（Logcat 始终原样输出，不受重复抑制影响）
        when (level) {
            LogLevel.DEBUG -> Log.d(TAG, finalMessage)
            LogLevel.INFO -> Log.i(TAG, finalMessage)
            LogLevel.WARN -> Log.w(TAG, finalMessage)
            LogLevel.ERROR -> Log.e(TAG, finalMessage, finalThrowable)
        }

        // 异步写入本地日志文件（ERROR 级及以上始终落盘）
        // ERROR/CRASH 与决策日志（decision）豁免重复抑制，直接原样写入
        if (alwaysWrite || bypassThrottle) {
            saveLogToFile(level.name, TAG, finalMessage, finalThrowable)
        } else {
            writeWithThrottle(requestId, node, truncatedMessage, finalMessage, finalThrowable, level)
        }
    }

    /**
     * 重复日志抑制写入：仅作用于【文件写入】。
     *
     * 以第一条匹配指纹的日志时刻为窗口起点（WINDOW_MS），窗口内同指纹后续日志不写文件、累计计数；
     * 超窗后的下一条到来时先写一行摘要 `原message [xN次]`，再以本条为新窗口首条写原文，计数重置 0。
     * 指纹计算与窗口计数在调用线程同步执行（synchronized），仅最终文件写入投递到 logExecutor。
     */
    private fun writeWithThrottle(
        requestId: String,
        node: String,
        messageBody: String,
        finalMessage: String,
        finalThrowable: Throwable?,
        level: LogLevel
    ) {
        val fingerprint = buildFingerprint(requestId, node, messageBody)
        val now = System.currentTimeMillis()

        var summary: String? = null
        var writeOriginal = true

        synchronized(throttleLock) {
            val entry = fingerprintCache[fingerprint]
            if (entry == null) {
                // 新指纹：作为窗口首条，写原文
                fingerprintCache[fingerprint] = ThrottleEntry(now, 0)
            } else if (now - entry.windowStart < WINDOW_MS) {
                // 窗口内重复：累计计数，不写文件
                entry.count = entry.count + 1
                writeOriginal = false
            } else {
                // 已超窗：先写摘要（总量-1 次），再以本条为新窗口首条写原文，计数重置 0
                summary = "$finalMessage [x${entry.count}次]"
                entry.windowStart = now
                entry.count = 0
            }
        }

        val summaryOut = summary
        if (summaryOut != null) {
            saveLogToFile(level.name, TAG, summaryOut, finalThrowable)
        }
        if (writeOriginal) {
            saveLogToFile(level.name, TAG, finalMessage, finalThrowable)
        }
    }

    /**
     * 构建重复抑制指纹：`requestId + "|" + node + "|" + 截断/脱敏后的 message 主体`。
     * requestId 为空时退化为 `node + message`。
     */
    private fun buildFingerprint(requestId: String, node: String, message: String): String {
        return if (requestId.isBlank()) {
            "$node|$message"
        } else {
            "$requestId|$node|$message"
        }
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