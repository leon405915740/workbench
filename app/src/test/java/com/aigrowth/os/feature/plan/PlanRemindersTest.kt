package com.aigrowth.os.feature.plan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class PlanRemindersTest {
    private val zone = ZoneId.of("Asia/Shanghai")

    @Test
    fun planInput_requiresTitlePriorityDateAndHhMmTime() {
        assertTrue(isValidPlanInput("写周报", "P1", "2026-09-02", "09:30"))
        assertTrue(isValidPlanInput("写周报", "P1", "2026-09-02", null))
        assertFalse(isValidPlanInput(" ", "P1", "2026-09-02", "09:30"))
        assertFalse(isValidPlanInput("写周报", "P3", "2026-09-02", "09:30"))
        assertTrue(isValidPlanInput("旧计划", "自定义", "2026-09-02", null, allowCustomPriority = true))
        assertFalse(isValidPlanInput("写周报", "P1", "2026-02-30", "09:30"))
        assertFalse(isValidPlanInput("写周报", "P1", "2026-09-02", "9:30"))
        assertFalse(isValidPlanInput("写周报", "P1", "2026-09-02", "24:00"))
    }

    @Test
    fun reminderTriggerAt_combinesLocalDateAndTime() {
        assertEquals(
            Instant.parse("2026-09-02T01:30:00Z").toEpochMilli(),
            reminderTriggerAt("2026-09-02", "09:30", zone)
        )
    }

    @Test
    fun reminderTriggerAt_rejectsInvalidDateOrTime() {
        assertNull(reminderTriggerAt("2026-02-30", "09:30", zone))
        assertNull(reminderTriggerAt("2026-09-02", "25:00", zone))
    }

    @Test
    fun hasFutureTrigger_doesNotSchedulePastOrCurrentTime() {
        val triggerAt = reminderTriggerAt("2026-09-02", "09:30", ZoneId.systemDefault())!!
        assertTrue(PlanReminders.hasFutureTrigger("2026-09-02", "09:30", triggerAt - 1))
        assertFalse(PlanReminders.hasFutureTrigger("2026-09-02", "09:30", triggerAt))
    }
}
