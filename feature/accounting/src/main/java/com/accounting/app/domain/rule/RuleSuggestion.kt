package com.accounting.app.domain.rule

data class RuleSuggestion(
    val id: String,
    val triggerPattern: String,
    val category: String,
    val subCategory: String?,
    val amountExtractor: String?,
    val confidence: Float,
    val sourceInput: String,
    val keywords: List<String>,
    val type: String = "expense",
    val createdAt: Long = 0
)