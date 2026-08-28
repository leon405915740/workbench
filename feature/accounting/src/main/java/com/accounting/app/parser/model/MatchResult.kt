package com.accounting.app.parser.model

data class MatchResult(
    val type: String,
    val category: String,
    val subCategory: String? = null,
    val source: MatchSource,
    val confidence: Float,
    val matchedMemory: Boolean = false,
    val memoryId: Long? = null
)

enum class MatchSource {
    MAPPING,
    LOCAL_RULE,
    RULE,
    MEMORY,
    TIME_RULE,
    AI_HINT,
    AI_CORRECTION,
    FALLBACK
}