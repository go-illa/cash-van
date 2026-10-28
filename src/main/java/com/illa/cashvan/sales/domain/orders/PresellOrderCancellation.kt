package com.illa.cashvan.sales.domain.orders

import com.illa.cashvan.sales.domain.format.parseJsDateMillis
import java.time.Instant
import java.time.ZoneId

private const val FOUR_HOURS_MS = 4L * 60 * 60 * 1000

/** Structural stand-in for the TS `{ status, order_date }` order shape. */
interface CancellablePresellOrder {
    val status: String
    val orderDate: String
}

/**
 * An agent may cancel an unfulfilled order until four hours after its
 * `order_date`. An unparseable date never allows cancellation (JS NaN comparison).
 * [zone] only matters for a timestamp without an offset, which JS reads as local time.
 */
fun canCancelPresellOrder(
    order: CancellablePresellOrder,
    now: Instant = Instant.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): Boolean {
    if (order.status != "unfulfilled") return false
    val orderMillis = parseJsDateMillis(order.orderDate, zone) ?: return false
    return now.toEpochMilli() < orderMillis + FOUR_HOURS_MS
}
