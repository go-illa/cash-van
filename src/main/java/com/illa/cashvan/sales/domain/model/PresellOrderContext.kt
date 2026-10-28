package com.illa.cashvan.sales.domain.model

import com.illa.cashvan.core.location.LocationData

/**
 * What a presell order needs from the visit that started it: the PWA's vendor-selection navigation state.
 *
 * @property priceTier and [regionId] are re-read from the merchant before products load and before submit.
 * @property visitLocation the geofence-approved position, used if a fresh fix fails at submit.
 */
data class PresellOrderContext(
    val merchantId: String,
    val merchantName: String,
    val priceTier: String,
    val regionId: String?,
    val routeId: String?,
    val routeName: String?,
    val isAdHoc: Boolean,
    val visitLocation: LocationData?
)
