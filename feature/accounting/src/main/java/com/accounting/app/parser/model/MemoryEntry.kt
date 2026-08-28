package com.accounting.app.parser.model

import java.util.Date

data class MemoryEntry(
    val description: String,
    val type: String,
    val category: String,
    val subCategory: String?,
    val time: Date,
    val userId: String? = null
)