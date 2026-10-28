package com.illa.cashvan.sales.feature.merchant.presentation.viewmodel

data class PresellMerchantFormState(
    val merchantName: String = "",
    val signName: String = "",
    val merchantTypeId: String = "",
    val phoneNumber: String = "",
    val secondaryPhone: String = "",
    val governorate: String = "",
    val regionId: String = "",
    val detailedAddress: String = "",
    val priceTier: String = "retail",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val visitDays: Set<String> = emptySet(),
    val routeId: String? = null
)

val PRICE_TIERS = listOf(
    "retail" to "قطاعي",
    "large_grocery" to "قطاعي كبير",
    "wholesale" to "جملة",
    "big_wholesale" to "جملة كبيرة"
)

val WEEKDAYS = listOf(
    "sunday" to "الأحد",
    "monday" to "الإثنين",
    "tuesday" to "الثلاثاء",
    "wednesday" to "الأربعاء",
    "thursday" to "الخميس",
    "friday" to "الجمعة",
    "saturday" to "السبت"
)

private val EGYPTIAN_MOBILE = Regex("^01[0-9]{9}$")

fun isValidEgyptianMobile(phone: String): Boolean =
    EGYPTIAN_MOBILE.matches(phone.replace(Regex("[\\s-]"), ""))

/** Merchant details step. Returns the first problem, or null when the step is complete. */
fun validateMerchantStep(form: PresellMerchantFormState): String? = when {
    form.merchantName.isBlank() -> "يرجى إدخال اسم التاجر"
    form.signName.isBlank() -> "يرجى إدخال اسم اللافتة"
    form.merchantTypeId.isEmpty() -> "يرجى اختيار نوع التاجر"
    form.phoneNumber.isBlank() -> "يرجى إدخال رقم الهاتف"
    !isValidEgyptianMobile(form.phoneNumber) -> "يرجى إدخال رقم هاتف مصري صحيح"
    form.secondaryPhone.isNotEmpty() && !isValidEgyptianMobile(form.secondaryPhone) -> "رقم الهاتف الثانوي غير صحيح"
    else -> null
}

/** Location and visit plan step. */
fun validateLocationStep(form: PresellMerchantFormState, routeCount: Int, isLocating: Boolean): String? = when {
    isLocating -> "جاري تحديد الموقع، يرجى الانتظار..."
    form.governorate.isEmpty() -> "يرجى اختيار المحافظة"
    form.regionId.isEmpty() -> "لم يتم تحديد منطقة التسعير. يرجى إعادة اختيار المحافظة أو تحديث الصفحة."
    form.detailedAddress.isBlank() -> "يرجى إدخال العنوان التفصيلي"
    routeCount > 1 && form.routeId == null -> "يرجى اختيار المسار"
    form.visitDays.isEmpty() -> "يرجى اختيار يوم زيارة واحد على الأقل"
    else -> null
}
