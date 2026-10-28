package com.illa.cashvan.sales.domain.kpi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

// Ported from the PWA's src/lib/commissionStorage.test.ts. localStorage is not
// ported: round-trips go through an in-memory map with the same keys and the
// parse/serialize halves ("storage unavailable" has no Kotlin counterpart).
class CommissionStorageTest {

    private val store = HashMap<String, String>()

    private fun writeBaseline(agentId: String, state: StoredCommissionBaseline) {
        serializeCommissionBaseline(state)?.let { store[commissionBaselineStorageKey(agentId)] = it }
    }

    private fun readBaseline(agentId: String) = parseCommissionBaseline(store[commissionBaselineStorageKey(agentId)])

    private fun writeUnlock(agentId: String, state: StoredCommissionUnlock) {
        serializeCommissionUnlock(state)?.let { store[commissionUnlockStorageKey(agentId)] = it }
    }

    private fun readUnlock(agentId: String) = parseCommissionUnlock(store[commissionUnlockStorageKey(agentId)])

    private val month = "2026-09"

    private fun snapshot(payoutEgp: Double, actualEgp: Double, unlocked: Boolean, monthKey: String = month) =
        CommissionSnapshot(payoutEgp, actualEgp, unlocked, monthKey)

    // baseline round-trip

    @Test
    fun `writes and reads back a valid baseline`() {
        writeBaseline("agent-1", StoredCommissionBaseline(month = "2026-09", payoutEgp = 1234.5, actualEgp = 98_765.0, ts = 1_000))

        assertEquals(StoredCommissionBaseline("2026-09", 1234.5, 98_765.0, 1_000), readBaseline("agent-1"))
    }

    @Test
    fun `stores the baseline JSON the PWA writes (extra)`() {
        writeBaseline("agent-1", StoredCommissionBaseline(month = "2026-09", payoutEgp = 1234.5, actualEgp = 98_765.0, ts = 1_000))

        assertEquals(
            """{"month":"2026-09","payoutEgp":1234.5,"actualEgp":98765,"ts":1000}""",
            store["sales_agent_commission_last_seen_v1::agent-1"],
        )
    }

    @Test
    fun `keeps agents isolated`() {
        writeBaseline("agent-1", StoredCommissionBaseline("2026-09", 10.0, 20.0, 1))

        assertNull(readBaseline("agent-2"))
    }

    @Test
    fun `returns null for corrupted JSON`() {
        store["sales_agent_commission_last_seen_v1::agent-1"] = "{not json"

        assertNull(readBaseline("agent-1"))
    }

    @Test
    fun `discards legacy baselines that lack the sales value`() {
        store["sales_agent_commission_last_seen_v1::agent-1"] = """{"month":"2026-09","payoutEgp":10,"ts":1}"""

        assertNull(readBaseline("agent-1"))
    }

    @Test
    fun `never stores an invalid baseline`() {
        writeBaseline("agent-1", StoredCommissionBaseline("2026-13", 10.0, 20.0, 1))

        assertNull(readBaseline("agent-1"))
        assertTrue(store.isEmpty())
    }

    @Test
    fun `rejects non-number values JSON parse would reject or type-check out (extra)`() {
        assertNull(parseCommissionBaseline("""{"month":"2026-09","payoutEgp":"10","actualEgp":20,"ts":1}"""))
        assertNull(parseCommissionBaseline("""{"month":"2026-09","payoutEgp":NaN,"actualEgp":20,"ts":1}"""))
        assertNull(parseCommissionBaseline("""{"month":"2026-09","payoutEgp":null,"actualEgp":20,"ts":1}"""))
        assertNull(parseCommissionBaseline("[]"))
        assertNull(parseCommissionBaseline(""))
        assertNull(serializeCommissionBaseline(StoredCommissionBaseline("2026-09", Double.NaN, 20.0, 1)))
    }

    // unlock latch round-trip

    @Test
    fun `writes and reads back the unlock latch`() {
        writeUnlock("agent-1", StoredCommissionUnlock(month = "2026-09", unlocked = true, ts = 2_000))

        assertEquals(StoredCommissionUnlock("2026-09", true, 2_000), readUnlock("agent-1"))
    }

    @Test
    fun `never stores an invalid latch`() {
        // A non-boolean `unlocked` cannot be constructed in Kotlin; a stored one is rejected on read.
        store["sales_agent_commission_unlock_v1::agent-1"] = """{"month":"2026-09","unlocked":"yes","ts":1}"""
        writeUnlock("agent-2", StoredCommissionUnlock("2026-9", unlocked = true, ts = 1))

        assertNull(readUnlock("agent-1"))
        assertNull(readUnlock("agent-2"))
    }

    // computeCommissionUpdate

    @Test
    fun `returns null with no baseline (first ever open with data)`() {
        assertNull(computeCommissionUpdate(null, snapshot(payoutEgp = 0.0, actualEgp = 500.0, unlocked = false)))
    }

    @Test
    fun `returns null when the baseline is from a previous month`() {
        assertNull(
            computeCommissionUpdate(
                StoredCommissionBaseline("2026-08", payoutEgp = 100.0, actualEgp = 900.0, ts = 1),
                snapshot(payoutEgp = 200.0, actualEgp = 500.0, unlocked = false),
            ),
        )
    }

    @Test
    fun `locked - announces SALES growth (payout is pinned at 0)`() {
        assertEquals(
            CommissionUpdate(CommissionUpdateKind.SALES, 43_035.0),
            computeCommissionUpdate(
                StoredCommissionBaseline(month, payoutEgp = 0.0, actualEgp = 100_000.0, ts = 1),
                snapshot(payoutEgp = 0.0, actualEgp = 143_035.0, unlocked = false),
            ),
        )
    }

    @Test
    fun `locked - ignores payout growth (cannot happen below threshold)`() {
        assertNull(
            computeCommissionUpdate(
                StoredCommissionBaseline(month, payoutEgp = 0.0, actualEgp = 100.0, ts = 1),
                snapshot(payoutEgp = 50.0, actualEgp = 100.0, unlocked = false),
            ),
        )
    }

    @Test
    fun `unlocked - announces PAYOUT growth`() {
        assertEquals(
            CommissionUpdate(CommissionUpdateKind.PAYOUT, 1_500.0),
            computeCommissionUpdate(
                StoredCommissionBaseline(month, payoutEgp = 2_100.0, actualEgp = 350_000.0, ts = 1),
                snapshot(payoutEgp = 3_600.0, actualEgp = 600_000.0, unlocked = true),
            ),
        )
    }

    @Test
    fun `unlocked - sales growth alone does not trigger the popup`() {
        assertNull(
            computeCommissionUpdate(
                StoredCommissionBaseline(month, payoutEgp = 2_100.0, actualEgp = 350_000.0, ts = 1),
                snapshot(payoutEgp = 2_100.0, actualEgp = 600_000.0, unlocked = true),
            ),
        )
    }

    @Test
    fun `returns null for zero, negative, or sub-1-EGP deltas of the tracked metric`() {
        val stored = StoredCommissionBaseline(month, payoutEgp = 0.0, actualEgp = 100.0, ts = 1)

        assertNull(computeCommissionUpdate(stored, snapshot(payoutEgp = 0.0, actualEgp = 100.0, unlocked = false)))
        assertNull(computeCommissionUpdate(stored, snapshot(payoutEgp = 0.0, actualEgp = 50.0, unlocked = false)))
        assertNull(computeCommissionUpdate(stored, snapshot(payoutEgp = 0.0, actualEgp = 100.5, unlocked = false)))
    }

    @Test
    fun `fires on a delta of exactly 1 EGP (the documented minimum)`() {
        assertEquals(
            CommissionUpdate(CommissionUpdateKind.SALES, 1.0),
            computeCommissionUpdate(
                StoredCommissionBaseline(month, payoutEgp = 0.0, actualEgp = 100.0, ts = 1),
                snapshot(payoutEgp = 0.0, actualEgp = 101.0, unlocked = false),
            ),
        )
    }

    @Test
    fun `returns null when the computed delta is not finite (bad upstream data)`() {
        assertNull(
            computeCommissionUpdate(
                StoredCommissionBaseline(month, payoutEgp = 0.0, actualEgp = 100.0, ts = 1),
                snapshot(payoutEgp = Double.POSITIVE_INFINITY, actualEgp = 100.0, unlocked = true),
            ),
        )
    }

    // nextUnlockLatch

    @Test
    fun `seeds a fresh latch when none exists`() {
        assertEquals(StoredCommissionUnlock(month, unlocked = false, ts = 5_000), nextUnlockLatch(null, month, false, 5_000))
    }

    @Test
    fun `advances a locked same-month latch to unlocked`() {
        assertEquals(
            StoredCommissionUnlock(month, unlocked = true, ts = 5_000),
            nextUnlockLatch(StoredCommissionUnlock(month, unlocked = false, ts = 1), month, true, 5_000),
        )
    }

    @Test
    fun `is sticky - keeps a same-month unlocked latch even when achievement dips back below the threshold`() {
        val latched = StoredCommissionUnlock(month, unlocked = true, ts = 1)

        assertSame(latched, nextUnlockLatch(latched, month, false, 5_000))
    }

    @Test
    fun `does not carry an unlocked latch across months (a new month re-arms the celebration)`() {
        assertEquals(
            StoredCommissionUnlock("2026-10", unlocked = false, ts = 5_000),
            nextUnlockLatch(StoredCommissionUnlock(month, unlocked = true, ts = 1), "2026-10", false, 5_000),
        )
    }

    // planCommissionSettle

    private val current = snapshot(payoutEgp = 0.0, actualEgp = 143_035.0, unlocked = false)

    @Test
    fun `seeds the baseline with no update on the first ever settle`() {
        val plan = planCommissionSettle(null, current, 9_000)

        assertNull(plan.update)
        assertEquals(StoredCommissionBaseline(month, payoutEgp = 0.0, actualEgp = 143_035.0, ts = 9_000), plan.nextBaseline)
    }

    @Test
    fun `announces growth AND refreshes the baseline on the same settle`() {
        val plan = planCommissionSettle(StoredCommissionBaseline(month, 0.0, 100_000.0, 1), current, 9_000)

        assertEquals(CommissionUpdate(CommissionUpdateKind.SALES, 43_035.0), plan.update)
        assertEquals(143_035.0, plan.nextBaseline.actualEgp, 0.0)
        assertEquals(9_000L, plan.nextBaseline.ts)
    }

    @Test
    fun `refreshes the baseline even when there is nothing to announce (growth can never fire twice)`() {
        val plan = planCommissionSettle(StoredCommissionBaseline(month, 0.0, 143_035.0, 1), current, 9_000)

        assertNull(plan.update)
        assertEquals(StoredCommissionBaseline(month, payoutEgp = 0.0, actualEgp = 143_035.0, ts = 9_000), plan.nextBaseline)
    }

    @Test
    fun `resets silently at month rollover - no update, baseline rewritten to the new month`() {
        val plan = planCommissionSettle(StoredCommissionBaseline("2026-08", 5_000.0, 900_000.0, 1), current, 9_000)

        assertNull(plan.update)
        assertEquals("2026-09", plan.nextBaseline.month)
        assertEquals(0.0, plan.nextBaseline.payoutEgp, 0.0)
        assertEquals(143_035.0, plan.nextBaseline.actualEgp, 0.0)
    }

    // shouldCelebrateUnlock

    @Test
    fun `celebrates when unlocked with no latch yet`() {
        assertTrue(shouldCelebrateUnlock(null, month, true))
    }

    @Test
    fun `celebrates when the latch is from a previous month`() {
        assertTrue(shouldCelebrateUnlock(StoredCommissionUnlock("2026-08", unlocked = true, ts = 1), month, true))
    }

    @Test
    fun `celebrates when the latch recorded locked for this month`() {
        assertTrue(shouldCelebrateUnlock(StoredCommissionUnlock(month, unlocked = false, ts = 1), month, true))
    }

    @Test
    fun `does not celebrate again once the latch saw unlocked this month`() {
        assertFalse(shouldCelebrateUnlock(StoredCommissionUnlock(month, unlocked = true, ts = 1), month, true))
    }

    @Test
    fun `never celebrates while locked`() {
        assertFalse(shouldCelebrateUnlock(null, month, false))
        assertFalse(shouldCelebrateUnlock(StoredCommissionUnlock(month, unlocked = false, ts = 1), month, false))
    }
}
