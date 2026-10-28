package com.illa.cashvan.sales.domain.kpi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

// Ported from the PWA's src/hooks/useAgentCommission.test.ts (the Supabase
// mock's recorded query becomes [commissionPayoutsQuery]; its result handling
// becomes [resolveCommissionQueryResult]) and from
// src/components/dashboard/CommissionProgressCard.test.tsx (markup assertions
// become assertions on [CommissionProgressView]; skeleton/CSS-only cases are not ported).
class AgentCommissionTest {

    // Cairo = UTC+3 with Egypt DST in September/October 2026.
    private val septNow = Instant.parse("2026-09-21T10:00:00Z") // Cairo 2026-09-21 13:00
    private val cairoOctNow = Instant.parse("2026-09-30T21:30:00Z") // Cairo 2026-10-01 00:30

    private val monthKey = "2026-09"

    private fun row(
        target: Double?,
        actual: Double?,
        payout: Double?,
        atThreshold: Double?,
        threshold: Double?,
        snapshottedAt: String? = "t",
    ) = CommissionPayoutRow(target, actual, payout, atThreshold, threshold, snapshottedAt)

    // currentCommissionMonthKey

    @Test
    fun `formats the current Cairo month as YYYY-MM`() {
        assertEquals("2026-09", currentCommissionMonthKey(septNow))
    }

    @Test
    fun `follows Cairo (not UTC) across the month boundary`() {
        assertEquals("2026-10", currentCommissionMonthKey(cairoOctNow))
    }

    // reduceCommissionRows

    @Test
    fun `returns null for zero rows`() {
        assertNull(reduceCommissionRows(emptyList(), monthKey))
    }

    @Test
    fun `returns null when the summed target is zero (agent not on a plan)`() {
        assertNull(reduceCommissionRows(listOf(row(0.0, 500.0, 0.0, 0.0, 70.0)), monthKey))
    }

    @Test
    fun `sums multiple rows and uses the strictest unlock threshold`() {
        val result = requireNotNull(
            reduceCommissionRows(
                listOf(
                    row(1_000_000.0, 600_000.0, 0.0, 420.0, 70.0, "2026-09-21T09:00:00Z"),
                    row(500_000.0, 400_000.0, 5_100.0, 3_000.0, 60.0, "2026-09-21T09:00:00Z"),
                ),
                monthKey,
            ),
        )

        assertEquals(1_500_000.0, result.targetEgp, 0.0)
        assertEquals(1_000_000.0, result.actualEgp, 0.0)
        assertEquals(5_100.0, result.payoutEgp, 0.0)
        assertEquals(3_420.0, result.payoutAtThresholdEgp, 0.0)
        // Strictest of 70 and 60.
        assertEquals(60.0, result.minThresholdPct, 0.0)
        assertEquals(1_000_000.0 / 1_500_000.0 * 100, result.achievementPct, 1e-9)
        assertTrue(result.unlocked) // 66.67 >= 60
        assertEquals("2026-09-21T09:00:00Z", result.snapshottedAt)
    }

    @Test
    fun `coerces NULL numeric columns to 0 and NULL threshold to the 70 default`() {
        val result = requireNotNull(
            reduceCommissionRows(listOf(row(1_000_000.0, null, null, null, null, snapshottedAt = null)), monthKey),
        )

        assertEquals(0.0, result.actualEgp, 0.0)
        assertEquals(0.0, result.payoutEgp, 0.0)
        assertEquals(0.0, result.payoutAtThresholdEgp, 0.0)
        assertEquals(70.0, result.minThresholdPct, 0.0)
        assertEquals(0.0, result.achievementPct, 0.0)
        assertFalse(result.unlocked)
        assertEquals("", result.snapshottedAt)
    }

    @Test
    fun `treats achievement exactly at the threshold as unlocked (greater-or-equal boundary)`() {
        val result = requireNotNull(reduceCommissionRows(listOf(row(1_000_000.0, 700_000.0, 2_100.0, 2_100.0, 70.0)), monthKey))

        assertTrue(result.unlocked)
    }

    @Test
    fun `keeps achievement above 100 percent unlocked (the card clamps the bar)`() {
        val result = requireNotNull(reduceCommissionRows(listOf(row(100.0, 250.0, 10.0, 5.0, 70.0)), monthKey))

        assertEquals(250.0, result.achievementPct, 1e-9)
        assertTrue(result.unlocked)
    }

    // fetchAgentCommission: query shape and result handling

    @Test
    fun `queries only by agent UUID - never by agent name (identity is FK-enforced)`() {
        val query = commissionPayoutsQuery("agent-uuid-1", septNow)

        assertEquals("commission_payouts", query.table)
        assertEquals(
            "target_egp,actual_egp,payout_egp,payout_at_threshold_egp,min_threshold_pct,snapshotted_at",
            query.columns,
        )
        assertEquals(
            listOf<Pair<String, Any>>("agent_id" to "agent-uuid-1", "is_live" to true, "month" to "2026-09-01"),
            query.eqFilters,
        )
        assertFalse(query.eqFilters.any { (column, _) -> column == "agent_name" })
        assertEquals(10, query.limit)
    }

    @Test
    fun `uses the Cairo month for the month filter (UTC-Cairo skew at edges)`() {
        assertTrue(commissionPayoutsQuery("agent-uuid-1", cairoOctNow).eqFilters.contains("month" to "2026-10-01"))
    }

    @Test
    fun `swallows 42703 (missing columns - unmigrated environment) and hides the UI`() {
        assertNull(resolveCommissionQueryResult(null, CommissionQueryError("42703", "column does not exist"), monthKey))
    }

    @Test
    fun `swallows 42P01 (missing table - unmigrated environment) and hides the UI`() {
        assertNull(resolveCommissionQueryResult(null, CommissionQueryError("42P01", "relation does not exist"), monthKey))
    }

    @Test
    fun `rethrows real errors (network, RLS, auth) so they surface`() {
        val error = assertThrows(CommissionLoadException::class.java) {
            resolveCommissionQueryResult(null, CommissionQueryError("42501", "row-level security"), monthKey)
        }

        assertEquals("row-level security", error.message)
    }

    @Test
    fun `falls back to a generic message for an error without one (extra)`() {
        val error = assertThrows(CommissionLoadException::class.java) {
            resolveCommissionQueryResult(null, CommissionQueryError(null, null), monthKey)
        }

        assertEquals("Failed to load commission", error.message)
    }

    @Test
    fun `returns null when the agent has no live rows this month`() {
        assertNull(resolveCommissionQueryResult(emptyList(), null, currentCommissionMonthKey(septNow)))
        assertNull(resolveCommissionQueryResult(null, null, currentCommissionMonthKey(septNow)))
    }

    @Test
    fun `returns the reduced commission for live rows`() {
        val result = requireNotNull(
            resolveCommissionQueryResult(
                listOf(row(1_000_000.0, 750_000.0, 2_250.0, 2_100.0, 70.0, "2026-09-21T09:00:00Z")),
                null,
                currentCommissionMonthKey(septNow),
            ),
        )

        assertEquals("2026-09", result.monthKey)
        assertEquals(2_250.0, result.payoutEgp, 0.0)
        assertTrue(result.unlocked)
    }

    // CommissionProgressCard

    private fun commission(
        targetEgp: Double = 1_000_000.0,
        actualEgp: Double = 600_000.0,
        payoutEgp: Double = 0.0,
        payoutAtThresholdEgp: Double = 420.0,
        achievementPct: Double = 60.0,
        unlocked: Boolean = false,
    ) = AgentCommission(
        monthKey = "2026-09",
        targetEgp = targetEgp,
        actualEgp = actualEgp,
        // Below the threshold the plan pays nothing — matches real rows.
        payoutEgp = payoutEgp,
        // CT-computed teaser: the commission at exactly 70% of target.
        payoutAtThresholdEgp = payoutAtThresholdEgp,
        minThresholdPct = 70.0,
        achievementPct = achievementPct,
        unlocked = unlocked,
        snapshottedAt = "2026-09-20T10:00:00Z",
    )

    private fun view(commission: AgentCommission?, isError: Boolean = false) =
        buildCommissionProgressView(commission, isError, septNow)

    private fun progressCard(commission: AgentCommission) = requireNotNull(view(commission))

    @Test
    fun `renders nothing when there is no live commission data`() {
        assertNull(view(null))
    }

    @Test
    fun `renders nothing on fetch error (silent hide)`() {
        assertNull(view(null, isError = true))
        assertNull(view(commission(), isError = true))
    }

    @Test
    fun `renders nothing when the target is zero (agent not on a plan)`() {
        assertNull(view(commission(targetEgp = 0.0)))
    }

    @Test
    fun `shows the locked state with the remaining EGP to unlock below the threshold`() {
        val card = progressCard(commission())

        assertTrue(card.title.contains("عمولة الشهر"))
        assertTrue(card.title.contains("سبتمبر 2026"))
        assertEquals("عمولة الشهر (سبتمبر 2026)", card.title)
        // Pace eyebrow shows the day-of-month progress.
        assertTrue(Regex("يوم \\d+ من \\d+").containsMatchIn(card.subtitle))
        assertEquals("وتفتح العمولة عند 70% • يوم 21 من 30", card.subtitle)
        // The remaining-to-unlock line lives in the hero, under the payout number.
        assertEquals("فاضل", COMMISSION_REMAINING_TO_UNLOCK_LABEL)
        assertEquals("100,000", card.remainingToUnlockText)
        assertEquals("المحقق", COMMISSION_ACHIEVED_LABEL)
        assertEquals("المتبقي", COMMISSION_REMAINING_LABEL)
        assertEquals("المستهدف", COMMISSION_TARGET_LABEL)
        // The target itself must be clearly visible, not just the remainder.
        assertEquals("1,000,000", card.targetText)
        assertEquals("600,000", card.achievedText)
        assertEquals("400,000", card.remainingToTargetText)
        // Locked / zero payout: frosted, with the locked (red, slashed) styling.
        assertFalse(card.isEarning)
        assertFalse(card.unlocked)
        // The frosted number is the at-threshold commission teaser, not a blurred 0.
        assertEquals("420", card.heroPayoutText)
        // The key marker sits on the bar only while locked.
        assertEquals(70.0, card.thresholdMarkerPct)
        assertEquals("70%", card.thresholdLabel)
        // Locked — no unlocked celebration copy.
        assertNull(card.unlockedCaption)
        assertEquals("60%", card.achievementLabel)
        assertEquals(60.0, card.barPercent, 0.0)
    }

    @Test
    fun `shows the unlocked celebration block with the payout at or above the threshold`() {
        val card = progressCard(commission(actualEgp = 850_000.0, payoutEgp = 5_100.0, achievementPct = 85.0, unlocked = true))

        assertEquals("العمولة اتفتحت! دي عمولتك المحققة 🎉", card.unlockedCaption)
        // Earning — gold number on the unlocked styling.
        assertTrue(card.isEarning)
        assertTrue(card.unlocked)
        // Earning — the hero shows the real payout, never the teaser value.
        assertEquals("5,100", card.heroPayoutText)
        // Unlocked — the key marker disappears from the progress bar.
        assertNull(card.thresholdMarkerPct)
        // Unlocked — no locked hint.
        assertFalse(card.subtitle.contains("وتفتح العمولة"))
        assertFalse(card.subtitle.contains("مقفولة"))
        assertEquals("العمولة مفتوحة ✅ • يوم 21 من 30", card.subtitle)
        assertNull(card.remainingToUnlockText)
    }

    @Test
    fun `marks the threshold position on the progress track`() {
        assertEquals(70.0, progressCard(commission()).thresholdMarkerPct)
    }

    @Test
    fun `falls back to the blurred 0 when the at-threshold value is 0 (rows frozen before the column existed)`() {
        val card = progressCard(commission(payoutAtThresholdEgp = 0.0))

        // Still frosted (payoutEgp is 0), and the teaser must not render.
        assertFalse(card.isEarning)
        assertEquals("0", card.heroPayoutText)
    }

    @Test
    fun `celebrates reaching the full target and clamps the bar (extra)`() {
        val card = progressCard(commission(actualEgp = 1_200_000.0, payoutEgp = 9_000.0, achievementPct = 120.0, unlocked = true))

        assertEquals("حققت المستهدف بالكامل 🏆", card.unlockedCaption)
        assertEquals("0", card.remainingToTargetText)
        assertEquals(100.0, card.barPercent, 0.0)
        assertEquals("120%", card.achievementLabel)
    }

    @Test
    fun `labels the month with the PWA month names (extra)`() {
        assertEquals("إبريل 2026", commissionMonthLabel("2026-04"))
        assertEquals(" 2026", commissionMonthLabel("2026-13"))
    }

    @Test
    fun `celebrates once and writes the latch only when it changes (extra)`() {
        val unlocked = commission(actualEgp = 850_000.0, payoutEgp = 5_100.0, achievementPct = 85.0, unlocked = true)

        val first = planUnlockCelebration(null, unlocked, nowMillis = 1_000)
        assertTrue(first.celebrate)
        assertEquals(StoredCommissionUnlock("2026-09", unlocked = true, ts = 1_000), first.latchToWrite)

        val again = planUnlockCelebration(first.latchToWrite, unlocked, nowMillis = 2_000)
        assertFalse(again.celebrate)
        assertNull(again.latchToWrite)

        val dipped = planUnlockCelebration(first.latchToWrite, commission(), nowMillis = 3_000)
        assertFalse(dipped.celebrate)
        assertNull(dipped.latchToWrite)
    }

    @Test
    fun `tracks the card view once per agent, month and lock state (extra)`() {
        assertEquals("agent-1:2026-09:false", commissionViewSignature("agent-1", commission()))
    }
}
