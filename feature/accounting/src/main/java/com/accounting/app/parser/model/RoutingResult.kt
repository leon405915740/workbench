package com.accounting.app.parser.model

import com.accounting.app.data.model.BillExecutePlan

sealed class RoutingResult {
    data class Success(val plan: BillExecutePlan, val source: String) : RoutingResult()
    data class AiSuccess(
        val plan: BillExecutePlan,
        val source: String
    ) : RoutingResult()
    data class Failure(val reason: String) : RoutingResult()
}