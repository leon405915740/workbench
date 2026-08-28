package com.accounting.app.ai.model

data class AiOutput(
    val items: List<AiItem> = emptyList()
)

data class AiItem(
    val description: String? = null,
    val amount: String? = null,
    val time_hint: String? = null,
    val category_hint: String? = null,
    val note: String? = null
)