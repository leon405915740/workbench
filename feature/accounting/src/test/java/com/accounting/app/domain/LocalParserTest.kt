package com.accounting.app.domain

import com.accounting.app.ai.model.AiItem
import com.accounting.app.ai.model.AiOutput
import com.accounting.app.plan.parser.LocalParser
import com.accounting.app.plan.model.NormalizedItem
import org.junit.Test

class LocalParserTest {

    @Test
    fun `parse should parse numeric amount`() {
        val aiOutput = AiOutput(listOf(
            AiItem(description = "午餐", amount = "30", time_hint = "今天")
        ))
        val result = LocalParser.parse(aiOutput, "test-request")
        assert(result.size == 1)
        assert(result[0].amount == 30.0)
        assert(result[0].description == "午餐")
    }

    @Test
    fun `parse should parse decimal amount`() {
        val aiOutput = AiOutput(listOf(
            AiItem(description = "咖啡", amount = "35.5", time_hint = "今天")
        ))
        val result = LocalParser.parse(aiOutput, "test-request")
        assert(result[0].amount == 35.5)
    }

    @Test
    fun `parse should handle null amount as 0`() {
        val aiOutput = AiOutput(listOf(
            AiItem(description = "未知", amount = null, time_hint = "今天")
        ))
        val result = LocalParser.parse(aiOutput, "test-request")
        assert(result[0].amount == 0.0)
    }

    @Test
    fun `parse should handle multiple items`() {
        val aiOutput = AiOutput(listOf(
            AiItem(description = "午餐", amount = "30", time_hint = "今天"),
            AiItem(description = "打车", amount = "25", time_hint = "今天")
        ))
        val result = LocalParser.parse(aiOutput, "test-request")
        assert(result.size == 2)
        assert(result.sumOf { it.amount } == 55.0)
    }

    @Test
    fun `parse should filter invalid items`() {
        val aiOutput = AiOutput(listOf(
            AiItem(description = null, amount = "invalid", time_hint = "invalid")
        ))
        val result = LocalParser.parse(aiOutput, "test-request")
        assert(result.isEmpty())
    }
}
