package com.accounting.app.plan.model

sealed class ExecuteResult {
    data class Success(val count: Int, val insertedIds: List<Long> = emptyList()) : ExecuteResult()
    data class Failure(val reason: String) : ExecuteResult()
}