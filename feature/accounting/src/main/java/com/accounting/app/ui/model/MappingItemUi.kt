package com.accounting.app.ui.model

data class MappingItemUi(
    val id: Long,
    val keyword: String,
    val categoryName: String,
    val subcategoryName: String?,
    val type: String,
    val isManual: Boolean,
    val isEnabled: Boolean,
    val hitCount: Int
)
