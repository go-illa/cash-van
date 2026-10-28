package com.illa.cashvan.sales.domain.cashvan

/** The two Fawry-device columns of a `merchants` row. */
data class MerchantFawryStatus(
    val fawryDeviceNumber: String?,
    val wantsFawryDevice: Boolean?,
)

/**
 * DIS-520: copies the Supabase-only Fawry fields onto the Rails merchants
 * (both sources share the same merchants table). Merchants without a row are
 * returned unchanged.
 */
fun applyMerchantFawryStatus(
    merchants: List<RailsMerchant>,
    statusById: Map<String, MerchantFawryStatus>,
): List<RailsMerchant> = merchants.map { merchant ->
    val status = statusById[merchant.id] ?: return@map merchant
    merchant.copy(fawryDeviceNumber = status.fawryDeviceNumber, wantsFawryDevice = status.wantsFawryDevice)
}
