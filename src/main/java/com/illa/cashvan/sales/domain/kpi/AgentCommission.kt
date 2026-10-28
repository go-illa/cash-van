package com.illa.cashvan.sales.domain.kpi

import com.illa.cashvan.sales.domain.cashvan.jsStringToNumber
import com.illa.cashvan.sales.domain.format.formatNumber
import com.illa.cashvan.sales.domain.format.formatPercent
import com.illa.cashvan.sales.domain.format.jsNumberToString
import java.time.Instant

// Live monthly commission (DIS-558): the pure parts of the PWA's
// src/hooks/useAgentCommission.ts (query shape, row reduction, error policy)
// and the display/celebration logic of components/dashboard/CommissionProgressCard.tsx.

/** Columns read from `commission_payouts`; numeric columns may be null. */
data class CommissionPayoutRow(
    val targetEgp: Double?,
    val actualEgp: Double?,
    val payoutEgp: Double?,
    val payoutAtThresholdEgp: Double?,
    val minThresholdPct: Double?,
    val snapshottedAt: String?,
)

data class AgentCommission(
    /** Commission month, 'YYYY-MM' (Cairo). */
    val monthKey: String,
    val targetEgp: Double,
    val actualEgp: Double,
    val payoutEgp: Double,
    /** Control-tower-computed commission at exactly the unlock threshold; shown frosted while locked. */
    val payoutAtThresholdEgp: Double,
    /** Unlock threshold in percent (e.g. 70). */
    val minThresholdPct: Double,
    /** actualEgp ÷ targetEgp × 100, recomputed from the summed rows. */
    val achievementPct: Double,
    /** achievementPct ≥ minThresholdPct. */
    val unlocked: Boolean,
    /** ISO timestamp of the live snapshot, or "". */
    val snapshottedAt: String,
)

const val COMMISSION_PAYOUTS_TABLE = "commission_payouts"
const val COMMISSION_PAYOUTS_COLUMNS =
    "target_egp,actual_egp,payout_egp,payout_at_threshold_egp,min_threshold_pct,snapshotted_at"
const val COMMISSION_PAYOUTS_LIMIT = 10

private const val DEFAULT_MIN_THRESHOLD_PCT = 70.0

/** Postgres codes of an environment without the live-sync migration (missing column / table). */
private val UNMIGRATED_PG_CODES = setOf("42703", "42P01")

/** The `commission_payouts` read: eq filters in order, applied with [limit]. */
data class CommissionPayoutsQuery(
    val table: String,
    val columns: String,
    val eqFilters: List<Pair<String, Any>>,
    val limit: Int,
)

/** A PostgREST error of the commission query. */
data class CommissionQueryError(val code: String?, val message: String?)

class CommissionLoadException(message: String) : RuntimeException(message)

/** Current Cairo month as 'YYYY-MM'. */
fun currentCommissionMonthKey(now: Instant = Instant.now()): String {
    val period = currentCairoPeriod(now)
    return "${period.year}-${period.month.toString().padStart(2, '0')}"
}

/** First day of the current Cairo month, 'YYYY-MM-01': how the `month` date column is stored. */
fun currentMonthDateISO(now: Instant = Instant.now()): String = "${currentCommissionMonthKey(now)}-01"

/**
 * Live rows of [agentId] for the current Cairo month. Identity is the agent
 * UUID alone, never the name (OLAP-sourced names drift from the agents table).
 */
fun commissionPayoutsQuery(agentId: String, now: Instant = Instant.now()): CommissionPayoutsQuery =
    CommissionPayoutsQuery(
        table = COMMISSION_PAYOUTS_TABLE,
        columns = COMMISSION_PAYOUTS_COLUMNS,
        eqFilters = listOf("agent_id" to agentId, "is_live" to true, "month" to currentMonthDateISO(now)),
        limit = COMMISSION_PAYOUTS_LIMIT,
    )

/**
 * Reduces live rows into the card's commission. Sums defensively (an agent on
 * several target defs) and uses the strictest unlock threshold. Null for no
 * rows or a non-positive target (agent not on a plan): the UI hides.
 */
fun reduceCommissionRows(rows: List<CommissionPayoutRow>, monthKey: String): AgentCommission? {
    if (rows.isEmpty()) return null

    val targetEgp = rows.sumOf { it.targetEgp ?: 0.0 }
    if (targetEgp <= 0) return null

    val actualEgp = rows.sumOf { it.actualEgp ?: 0.0 }
    val payoutEgp = rows.sumOf { it.payoutEgp ?: 0.0 }
    // Rows frozen before the column existed carry NULL → 0.
    val payoutAtThresholdEgp = rows.sumOf { it.payoutAtThresholdEgp ?: 0.0 }
    val minThresholdPct = rows.minOf { it.minThresholdPct ?: DEFAULT_MIN_THRESHOLD_PCT }
    val achievementPct = actualEgp / targetEgp * 100

    return AgentCommission(
        monthKey = monthKey,
        targetEgp = targetEgp,
        actualEgp = actualEgp,
        payoutEgp = payoutEgp,
        payoutAtThresholdEgp = payoutAtThresholdEgp,
        minThresholdPct = minThresholdPct,
        achievementPct = achievementPct,
        unlocked = achievementPct >= minThresholdPct,
        snapshottedAt = rows.first().snapshottedAt ?: "",
    )
}

/**
 * Outcome of the commission query. An unmigrated environment (42703/42P01) hides
 * the UI silently (null; the PWA logs a warning); any other error throws
 * [CommissionLoadException] so it surfaces as a failed load.
 */
fun resolveCommissionQueryResult(
    rows: List<CommissionPayoutRow>?,
    error: CommissionQueryError?,
    monthKey: String,
): AgentCommission? {
    if (error != null) {
        if (error.code in UNMIGRATED_PG_CODES) return null
        throw CommissionLoadException(error.message ?: "Failed to load commission")
    }
    return reduceCommissionRows(rows.orEmpty(), monthKey)
}

// --- CommissionProgressCard ---

const val COMMISSION_REMAINING_TO_UNLOCK_LABEL = "فاضل"
const val COMMISSION_ACHIEVED_LABEL = "المحقق"
const val COMMISSION_REMAINING_LABEL = "المتبقي"
const val COMMISSION_TARGET_LABEL = "المستهدف"

private const val UNLOCKED_STATUS = "العمولة مفتوحة ✅"
private const val UNLOCKED_CAPTION = "العمولة اتفتحت! دي عمولتك المحققة 🎉"
private const val TARGET_REACHED_CAPTION = "حققت المستهدف بالكامل 🏆"

/** Everything the progress card shows, with final (post count-up) values. */
data class CommissionProgressView(
    /** "عمولة الشهر (سبتمبر 2026)". */
    val title: String,
    /** Unlock rule or unlocked status, then the pace, e.g. "وتفتح العمولة عند 70% • يوم 20 من 30". */
    val subtitle: String,
    /** Achievement pill, e.g. "60%". */
    val achievementLabel: String,
    val unlocked: Boolean,
    /** payoutEgp > 0: the gold, earning number; otherwise the number is frosted (blurred). */
    val isEarning: Boolean,
    /** Hero number: the real payout, or the at-threshold teaser while nothing is earned. */
    val heroPayoutEgp: Double,
    val heroPayoutText: String,
    /** Unlocked only: celebration or "target fully reached" caption. */
    val unlockedCaption: String?,
    /** Locked only: EGP left to unlock, shown under [COMMISSION_REMAINING_TO_UNLOCK_LABEL]. */
    val remainingToUnlockText: String?,
    /** Bar fill in percent, clamped at 100 (animates up from 0). */
    val barPercent: Double,
    /** Key marker position (percent) while locked with a threshold strictly between 0 and 100. */
    val thresholdMarkerPct: Double?,
    val thresholdLabel: String,
    val achievedText: String,
    val remainingToTargetText: String,
    val targetText: String,
)

/** 'YYYY-MM' → "سبتمبر 2026" (PWA month names, Western digits). */
fun commissionMonthLabel(monthKey: String): String {
    val year = jsStringToNumber(monthKey.take(4))
    val month = jsStringToNumber(monthKey.drop(5).take(2))
    val name = if (month % 1.0 == 0.0 && month in 1.0..12.0) ARABIC_MONTHS[month.toInt() - 1] else ""
    return "$name ${jsNumberToString(year)}"
}

/**
 * The card for [commission], or null when it is hidden: on a fetch error, with
 * no live commission, or for a non-positive target. [now] drives the Cairo
 * day-of-month pace line.
 */
fun buildCommissionProgressView(
    commission: AgentCommission?,
    isError: Boolean = false,
    now: Instant = Instant.now(),
): CommissionProgressView? {
    if (isError || commission == null || commission.targetEgp <= 0) return null

    val pct = commission.achievementPct
    val threshold = commission.minThresholdPct
    val unlocked = commission.unlocked
    val remainingToTarget = maxOf(commission.targetEgp - commission.actualEgp, 0.0)
    val remainingToUnlock = maxOf(commission.targetEgp * threshold / 100 - commission.actualEgp, 0.0)
    val heroPayoutEgp = if (commission.payoutEgp > 0) commission.payoutEgp else commission.payoutAtThresholdEgp
    val today = cairoCalendarDate(now)
    val status = if (unlocked) UNLOCKED_STATUS else "وتفتح العمولة عند ${formatPercent(threshold)}"

    return CommissionProgressView(
        title = "عمولة الشهر (${commissionMonthLabel(commission.monthKey)})",
        subtitle = "$status • يوم ${today.dayOfMonth} من ${today.lengthOfMonth()}",
        achievementLabel = formatPercent(pct),
        unlocked = unlocked,
        isEarning = commission.payoutEgp > 0,
        heroPayoutEgp = heroPayoutEgp,
        heroPayoutText = formatNumber(heroPayoutEgp),
        unlockedCaption = when {
            !unlocked -> null
            remainingToTarget > 0 -> UNLOCKED_CAPTION
            else -> TARGET_REACHED_CAPTION
        },
        remainingToUnlockText = if (unlocked) null else formatNumber(remainingToUnlock),
        barPercent = minOf(pct, 100.0),
        thresholdMarkerPct = threshold.takeIf { !unlocked && it > 0 && it < 100 },
        thresholdLabel = formatPercent(threshold),
        achievedText = formatNumber(commission.actualEgp),
        remainingToTargetText = formatNumber(remainingToTarget),
        targetText = formatNumber(commission.targetEgp),
    )
}

/** Celebration decision for one observed commission, and the latch to write (null: unchanged). */
data class UnlockCelebrationPlan(val celebrate: Boolean, val latchToWrite: StoredCommissionUnlock?)

/**
 * Confetti + fanfare fire once per month; the latch is written only when it
 * changes, so the 60 s live refetches do not rewrite the same value.
 */
fun planUnlockCelebration(
    stored: StoredCommissionUnlock?,
    commission: AgentCommission,
    nowMillis: Long = System.currentTimeMillis(),
): UnlockCelebrationPlan {
    val nextLatch = nextUnlockLatch(stored, commission.monthKey, commission.unlocked, nowMillis)
    val changed = stored == null || stored.month != nextLatch.month || stored.unlocked != nextLatch.unlocked
    return UnlockCelebrationPlan(
        celebrate = shouldCelebrateUnlock(stored, commission.monthKey, commission.unlocked),
        latchToWrite = nextLatch.takeIf { changed },
    )
}

/** The card-viewed analytics event fires once per agent + month + locked/unlocked state. */
fun commissionViewSignature(agentId: String, commission: AgentCommission): String =
    "$agentId:${commission.monthKey}:${commission.unlocked}"
