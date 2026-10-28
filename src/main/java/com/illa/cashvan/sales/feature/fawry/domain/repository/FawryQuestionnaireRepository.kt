package com.illa.cashvan.sales.feature.fawry.domain.repository

/**
 * DIS-517: before every visit the agent records whether the merchant has a Fawry device, or wants one.
 * Answers are written straight to Supabase `merchants`, as the PWA does.
 */
interface FawryQuestionnaireRepository {
    /** True until the merchant has a device number or has said they want one. A failed read doesn't block the visit. */
    suspend fun needsQuestionnaire(merchantId: String): Boolean

    /** @throws DuplicateFawryDeviceException when another merchant already has [deviceNumber]. */
    suspend fun saveDeviceNumber(merchantId: String, deviceNumber: String)

    suspend fun saveWantsDevice(merchantId: String, wantsDevice: Boolean)
}

class DuplicateFawryDeviceException(cause: Throwable) : Exception(cause.message, cause)
