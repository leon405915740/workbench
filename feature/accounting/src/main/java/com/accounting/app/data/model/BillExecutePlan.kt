package com.accounting.app.data.model

import com.accounting.app.plan.model.PlanType
import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Parcelize
data class BillExecutePlan(
    val requestId: String,
    val totalCount: Int,
    val totalAmount: Long,
    val items: List<BillPlanItem>,
    val rawInput: String,
    val type: PlanType = PlanType.SINGLE,
    val summary: PlanSummary? = null
) : Parcelable

@Parcelize
data class PlanSummary(
    val totalAmount: Double,
    val count: Int,
    val firstCategoryHint: String? = null
) : Parcelable