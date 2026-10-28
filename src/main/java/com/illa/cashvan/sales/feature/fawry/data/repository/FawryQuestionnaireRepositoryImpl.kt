package com.illa.cashvan.sales.feature.fawry.data.repository

import com.illa.cashvan.core.supabase.SupabaseClient
import com.illa.cashvan.core.supabase.SupabaseException
import com.illa.cashvan.sales.domain.cashvan.MerchantFawryStatus
import com.illa.cashvan.sales.domain.cashvan.needsFawryQuestionnaire
import com.illa.cashvan.sales.feature.fawry.domain.repository.DuplicateFawryDeviceException
import com.illa.cashvan.sales.feature.fawry.domain.repository.FawryQuestionnaireRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import timber.log.Timber

class FawryQuestionnaireRepositoryImpl(
    private val supabaseClient: SupabaseClient
) : FawryQuestionnaireRepository {

    @Serializable
    private data class FawryFields(
        val id: String? = null,
        val fawry_device_number: String? = null,
        val wants_fawry_device: Boolean? = null
    )

    override suspend fun needsQuestionnaire(merchantId: String): Boolean = try {
        val merchant = supabaseClient.selectFirstOrNull<FawryFields>("merchants") {
            select("id,fawry_device_number,wants_fawry_device")
            eq("id", merchantId)
        }
        needsFawryQuestionnaire(merchant?.let { MerchantFawryStatus(it.fawry_device_number, it.wants_fawry_device) })
    } catch (e: Exception) {
        Timber.w(e, "Fawry questionnaire check failed; not blocking the visit")
        false
    }

    override suspend fun saveDeviceNumber(merchantId: String, deviceNumber: String) {
        // wants_fawry_device is cleared explicitly: a device on file supersedes the earlier answer.
        val body = buildJsonObject {
            put("fawry_device_number", deviceNumber)
            put("wants_fawry_device", JsonNull)
        }
        try {
            update(merchantId, body)
        } catch (e: SupabaseException) {
            if (e.code == UNIQUE_VIOLATION) throw DuplicateFawryDeviceException(e)
            throw e
        }
    }

    override suspend fun saveWantsDevice(merchantId: String, wantsDevice: Boolean) {
        update(merchantId, buildJsonObject { put("wants_fawry_device", wantsDevice) })
    }

    private suspend fun update(merchantId: String, body: JsonObject) {
        supabaseClient.update<JsonObject, FawryFields>("merchants", body) {
            select("id")
            eq("id", merchantId)
        }
    }

    private companion object {
        const val UNIQUE_VIOLATION = "23505"
    }
}
