package com.illa.cashvan.sales.domain.kpi

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// Local persistence decisions for the live commission feature (DIS-558), ported
// from the PWA's src/lib/commissionStorage.ts. The localStorage reads/writes
// are not ported; their key, parse and serialize halves are (a stored value
// behaves the same), plus the pure popup/celebration decisions.
//
// Two independent per-agent records:
//  1. Baseline: the last payout and achieved sales the agent has SEEN; drives
//     the "you earned/sold X since you last looked" popup.
//  2. Unlock latch: whether the agent already saw the commission unlocked this
//     month; makes the confetti fire once per month.
// Both carry the commission month ('YYYY-MM') and are ignored once it no longer matches.

private const val BASELINE_KEY_PREFIX = "sales_agent_commission_last_seen_v1"
private const val UNLOCK_KEY_PREFIX = "sales_agent_commission_unlock_v1"

private val MONTH_KEY = Regex("[0-9]{4}-(0[1-9]|1[0-2])")

/** Minimum positive delta (EGP) required to show the "you earned" popup. */
const val COMMISSION_POPUP_MIN_DELTA_EGP = 1.0

data class StoredCommissionBaseline(
    /** Commission month, 'YYYY-MM'. */
    val month: String,
    /** Last seen live payout in EGP. */
    val payoutEgp: Double,
    /** Last seen live achieved sales in EGP. */
    val actualEgp: Double,
    /** Epoch millis when written. */
    val ts: Long,
)

data class StoredCommissionUnlock(
    /** Commission month, 'YYYY-MM'. */
    val month: String,
    /** Whether the agent already saw the unlocked state this month. */
    val unlocked: Boolean,
    /** Epoch millis when written. */
    val ts: Long,
)

/** The live values one commission fetch settled on. */
data class CommissionSnapshot(
    val payoutEgp: Double,
    val actualEgp: Double,
    val unlocked: Boolean,
    val monthKey: String,
)

enum class CommissionUpdateKind { PAYOUT, SALES }

/** What grew since the agent last looked; drives the popup copy and colour. */
data class CommissionUpdate(val kind: CommissionUpdateKind, val deltaEgp: Double)

/** What to announce after one settled fetch, and the baseline to persist (always) for next time. */
data class CommissionSettlePlan(val update: CommissionUpdate?, val nextBaseline: StoredCommissionBaseline)

private fun isValidMonthKey(value: String): Boolean = MONTH_KEY.matches(value)

fun commissionBaselineStorageKey(agentId: String): String = "$BASELINE_KEY_PREFIX::$agentId"

fun commissionUnlockStorageKey(agentId: String): String = "$UNLOCK_KEY_PREFIX::$agentId"

/** The stored baseline, or null when absent, malformed, from the legacy payout-only shape, or invalid. */
fun parseCommissionBaseline(raw: String?): StoredCommissionBaseline? {
    val json = parseStoredJsonObject(raw) ?: return null
    val month = json["month"].storedString()?.takeIf(::isValidMonthKey) ?: return null
    val payoutEgp = json["payoutEgp"].storedFiniteNumber() ?: return null
    val actualEgp = json["actualEgp"].storedFiniteNumber() ?: return null
    val ts = json["ts"].storedFiniteNumber() ?: return null
    return StoredCommissionBaseline(month, payoutEgp, actualEgp, ts.toLong())
}

/** JSON to store, or null when the baseline is invalid (the PWA then skips the write). */
fun serializeCommissionBaseline(state: StoredCommissionBaseline): String? {
    if (!isValidMonthKey(state.month) || !state.payoutEgp.isFinite() || !state.actualEgp.isFinite()) return null
    return buildJsonObject {
        put("month", state.month)
        put("payoutEgp", storedJsonNumber(state.payoutEgp))
        put("actualEgp", storedJsonNumber(state.actualEgp))
        put("ts", state.ts)
    }.toString()
}

fun parseCommissionUnlock(raw: String?): StoredCommissionUnlock? {
    val json = parseStoredJsonObject(raw) ?: return null
    val month = json["month"].storedString()?.takeIf(::isValidMonthKey) ?: return null
    val unlocked = json["unlocked"].storedBoolean() ?: return null
    val ts = json["ts"].storedFiniteNumber() ?: return null
    return StoredCommissionUnlock(month, unlocked, ts.toLong())
}

fun serializeCommissionUnlock(state: StoredCommissionUnlock): String? {
    if (!isValidMonthKey(state.month)) return null
    return buildJsonObject {
        put("month", state.month)
        put("unlocked", state.unlocked)
        put("ts", state.ts)
    }.toString()
}

/**
 * What to notify the agent about since [stored]. While LOCKED the payout is
 * pinned at 0, so sales growth is tracked; once UNLOCKED, payout growth.
 * Null without a baseline, for a previous month's baseline, or when the
 * tracked metric grew by less than [COMMISSION_POPUP_MIN_DELTA_EGP].
 */
fun computeCommissionUpdate(stored: StoredCommissionBaseline?, current: CommissionSnapshot): CommissionUpdate? {
    if (stored == null || stored.month != current.monthKey) return null

    val deltaEgp = if (current.unlocked) {
        current.payoutEgp - stored.payoutEgp
    } else {
        current.actualEgp - stored.actualEgp
    }

    if (!deltaEgp.isFinite() || deltaEgp < COMMISSION_POPUP_MIN_DELTA_EGP) return null
    return CommissionUpdate(if (current.unlocked) CommissionUpdateKind.PAYOUT else CommissionUpdateKind.SALES, deltaEgp)
}

/** Confetti fires only when unlocked now and the latch has not already seen unlocked this month. */
fun shouldCelebrateUnlock(stored: StoredCommissionUnlock?, currentMonthKey: String, unlockedNow: Boolean): Boolean {
    if (!unlockedNow) return false
    if (stored == null) return true
    return stored.month != currentMonthKey || !stored.unlocked
}

/**
 * The latch to persist after observing the unlock state. Sticky within a month:
 * a same-month unlocked latch is kept as-is even if achievement dips below the
 * threshold again, so the celebration is once per month, not once per crossing.
 */
fun nextUnlockLatch(
    stored: StoredCommissionUnlock?,
    currentMonthKey: String,
    unlockedNow: Boolean,
    nowMillis: Long = System.currentTimeMillis(),
): StoredCommissionUnlock {
    if (stored != null && stored.month == currentMonthKey && stored.unlocked) return stored
    return StoredCommissionUnlock(currentMonthKey, unlockedNow, nowMillis)
}

/**
 * Plan for one settled fetch. Pass the ORIGINAL stored baseline (read before
 * anything is written), then persist [CommissionSettlePlan.nextBaseline]
 * exactly once, whether or not an update came out.
 */
fun planCommissionSettle(
    stored: StoredCommissionBaseline?,
    current: CommissionSnapshot,
    nowMillis: Long = System.currentTimeMillis(),
): CommissionSettlePlan = CommissionSettlePlan(
    update = computeCommissionUpdate(stored, current),
    nextBaseline = StoredCommissionBaseline(
        month = current.monthKey,
        payoutEgp = current.payoutEgp,
        actualEgp = current.actualEgp,
        ts = nowMillis,
    ),
)
