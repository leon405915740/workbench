package com.accounting.app.plan.validator

import com.accounting.app.data.model.BillExecutePlan
import com.accounting.app.data.model.BillPlanItem
import com.accounting.app.data.model.PlanAction
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanValidatorTest {
    private fun item(amount: Long) = BillPlanItem(
        action = PlanAction.ADD,
        type = "expense",
        amount = amount,
        category = "其他支出",
        subCategory = null,
        merchant = null,
        billTime = 1L,
        remark = null,
        confidence = 1f,
        source = "test",
        matchedMemory = false
    )

    @Test
    fun `validate should reject overflowed or inconsistent totals`() {
        val overflow = BillExecutePlan(
            requestId = "test",
            totalCount = 2,
            totalAmount = -2L,
            items = listOf(item(Long.MAX_VALUE), item(Long.MAX_VALUE)),
            rawInput = "test"
        )
        val inconsistent = BillExecutePlan(
            requestId = "test",
            totalCount = 1,
            totalAmount = 99L,
            items = listOf(item(100L)),
            rawInput = "test"
        )

        assertTrue(PlanValidator.validate(overflow, "test") is ValidationResult.Failure)
        assertTrue(PlanValidator.validate(inconsistent, "test") is ValidationResult.Failure)
    }
}
