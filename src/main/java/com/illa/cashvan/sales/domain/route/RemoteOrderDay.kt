package com.illa.cashvan.sales.domain.route

import com.illa.cashvan.sales.domain.format.parseJsDateMillis
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToLong

/**
 * DIS-991 — channel of a merchant's latest "remote" order: one placed without
 * a field agent, either by telesales over the phone or by the merchant in the
 * ILLA retail app. Mirrors the `channel` column of get_recent_remote_orders.
 */
enum class RemoteOrderChannel(val wireValue: String) {
    TELESALES("telesales"),
    RETAIL("retail"),
    ;

    companion object {
        fun fromWire(value: String?): RemoteOrderChannel? = entries.firstOrNull { it.wireValue == value }
    }
}

data class RecentRemoteOrder(
    /** ISO timestamp of the order (sales_orders.order_date). */
    val at: String,
    val channel: RemoteOrderChannel,
)

private const val DAY_MS = 86_400_000.0

/**
 * Buckets an order timestamp into whole calendar days elapsed (0 = today …
 * 3 = three days ago) in [zone] — the device's local timezone, Cairo for field
 * agents — the same "أمس / منذ X أيام" notion the route card uses for visits.
 * Returns null when there is no order, it is invalid, in the future, or older
 * than 3 days; the caller then renders nothing.
 */
fun getRemoteOrderDayBucket(
    orderAt: String?,
    now: Instant = Instant.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): Int? {
    if (orderAt.isNullOrEmpty()) return null
    val orderMillis = parseJsDateMillis(orderAt, zone) ?: return null

    val startOfToday = now.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
    val startOfOrder = Instant.ofEpochMilli(orderMillis).atZone(zone).toLocalDate()
        .atStartOfDay(zone).toInstant().toEpochMilli()
    // Rounding absorbs DST shifts (Egypt observes DST) that make a day ≠ 24h.
    // roundToLong is Math.round (ties towards +∞), the same as JS Math.round.
    val days = ((startOfToday - startOfOrder) / DAY_MS).roundToLong()

    if (days < 0 || days > 3) return null
    return days.toInt()
}
