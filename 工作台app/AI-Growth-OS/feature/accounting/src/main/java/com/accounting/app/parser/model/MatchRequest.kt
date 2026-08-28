package com.accounting.app.parser.model

import java.util.Date

data class MatchRequest(
    val hint: String?,
    val description: String,
    val time: Date,
    val userId: String? = null,
    val categoryHint: String? = null
)