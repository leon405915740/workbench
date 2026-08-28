package com.accounting.app.plan.model

import java.util.Date

data class NormalizedItem(
    val description: String,
    val amount: Double,
    val time: Date,
    val categoryHint: String? = null,
    val note: String? = null,
    val sourceRaw: String
)