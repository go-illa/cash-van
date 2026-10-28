package com.illa.cashvan.sales.ui.route

import com.illa.cashvan.sales.data.parseTimestamp
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SHORT_DATE = DateTimeFormatter.ofPattern("d MMM", Locale("ar", "EG"))

/** "اليوم", "أمس", "منذ 3 أيام", then a short date, as the PWA's route card shows the last visit. */
fun formatLastVisit(isoDate: String?, now: Instant = Instant.now()): String? {
    val date = parseTimestamp(isoDate) ?: return null
    val days = Duration.between(date, now).abs().toDays()
    return when {
        days == 0L -> "اليوم"
        days == 1L -> "أمس"
        days <= 7L -> "منذ $days أيام"
        else -> SHORT_DATE.format(date.atZone(ZoneId.systemDefault()))
    }
}

private val DATE_TIME = DateTimeFormatter.ofPattern("d MMM yyyy، h:mm a", Locale("ar", "EG"))

fun formatDateTime(isoDate: String?): String =
    parseTimestamp(isoDate)?.let { DATE_TIME.format(it.atZone(ZoneId.systemDefault())) }.orEmpty()

fun priceTierLabel(tier: String?): String = when (tier) {
    "wholesale" -> "جملة"
    "big_wholesale" -> "جملة كبيرة"
    "large_grocery" -> "قطاعي كبير"
    else -> "قطاعي"
}
