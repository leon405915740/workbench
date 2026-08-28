package com.accounting.app.ui.model

data class MemoryItemUi(
    val id: Long,
    val triggerWord: String,
    val category: String,
    val subcategory: String?,
    val type: String,
    val source: String
)
