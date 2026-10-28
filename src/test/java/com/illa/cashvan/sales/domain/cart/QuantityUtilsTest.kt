package com.illa.cashvan.sales.domain.cart

import org.junit.Assert.assertEquals
import org.junit.Test

// Ported from the PWA's src/components/QuantityStepper.test.tsx (pure-function cases).
class QuantityUtilsTest {

    @Test
    fun `accepts a large whole-number quantity`() {
        assertEquals(100L, normalizeQuantityInput("100", 1, 0))
    }

    @Test
    fun `clamps a quantity to the available stock`() {
        assertEquals(25L, normalizeQuantityInput("100", 1, 0, 25))
    }

    @Test
    fun `preserves a restored excessive quantity until the agent explicitly changes it`() {
        assertEquals(100L, normalizeQuantityInput("100", 100, 0, 25))
    }

    @Test
    fun `allows zero so the caller can remove the item`() {
        assertEquals(0L, normalizeQuantityInput("0", 5, 0))
    }

    @Test
    fun `restores the current quantity for invalid input`() {
        listOf("", "-2", "1.5", "1e3", "abc", "99999999999999999").forEach { draft ->
            assertEquals("draft=$draft", 7L, normalizeQuantityInput(draft, 7, 0))
        }
    }

    @Test
    fun `asks for confirmation before capping a newly entered quantity`() {
        assertEquals(
            QuantityCommitDecision.ConfirmCap(enteredValue = 150, cappedValue = 100),
            getQuantityCommitDecision("150", 1, 0, 100, true),
        )
    }

    @Test
    fun `keeps the existing immediate cap when confirmation is not enabled`() {
        assertEquals(QuantityCommitDecision.Apply(100), getQuantityCommitDecision("150", 1, 0, 100, false))
    }

    @Test
    fun `applies quantities that do not exceed available stock`() {
        assertEquals(QuantityCommitDecision.Apply(75), getQuantityCommitDecision("75", 1, 0, 100, true))
    }

    @Test
    fun `does not prompt for a restored excessive quantity that was not edited`() {
        assertEquals(QuantityCommitDecision.Apply(150), getQuantityCommitDecision("150", 150, 0, 100, true))
    }
}
