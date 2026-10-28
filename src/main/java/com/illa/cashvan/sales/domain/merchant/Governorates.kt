package com.illa.cashvan.sales.domain.merchant

import java.text.Normalizer
import java.util.Locale

/** The governorates the create-merchant form offers, in the PWA's order. */
val GOVERNORATES = listOf(
    "القاهرة", "الجيزة", "الإسكندرية", "الدقهلية", "البحر الأحمر", "البحيرة", "الفيوم", "الغربية",
    "الإسماعيلية", "المنوفية", "المنيا", "القليوبية", "الوادي الجديد", "السويس", "أسوان", "أسيوط",
    "بني سويف", "بورسعيد", "دمياط", "الشرقية", "جنوب سيناء", "كفر الشيخ", "مطروح", "الأقصر", "قنا",
    "شمال سيناء", "سوهاج"
)

data class GovernorateRegion(
    val id: String,
    val names: List<String>
)

/**
 * Compares governorate names across sources (Rails geocoding, the form's list, `regions`): drops diacritics,
 * "محافظة"/"governorate", punctuation and case. Port of the PWA's `normalizeGovernorateName`.
 */
fun normalizeGovernorateName(value: String?): String {
    val decomposed = Normalizer.normalize(value.orEmpty(), Normalizer.Form.NFKD)
    return decomposed
        .replace(ARABIC_MARKS, "")
        .replace(GOVERNORATE_WORD, "")
        .replace("محافظة", "")
        .replace(NON_LETTER_OR_DIGIT, "")
        .lowercase(Locale("ar"))
}

fun findGovernorateRegion(regions: List<GovernorateRegion>, detectedName: String?): GovernorateRegion? {
    val normalized = normalizeGovernorateName(detectedName)
    if (normalized.isEmpty()) return null
    return regions.firstOrNull { region -> region.names.any { normalizeGovernorateName(it) == normalized } }
}

private val ARABIC_MARKS = Regex("[ًٌٍَُِّْـ]")
private val GOVERNORATE_WORD = Regex("\\bgovernorate\\b", RegexOption.IGNORE_CASE)
private val NON_LETTER_OR_DIGIT = Regex("[^\\p{L}\\p{N}]")
