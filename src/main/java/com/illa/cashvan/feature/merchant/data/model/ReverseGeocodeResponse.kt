package com.illa.cashvan.feature.merchant.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ReverseGeocodeResponse(
    val formatted_address: String,
    val detailed_address: String,
    val components: AddressComponents,
    val coordinates: ReverseGeocodeCoordinates
)

@Serializable
data class AddressComponents(
    val governorate: String,
    val country: String
)

@Serializable
data class ReverseGeocodeCoordinates(
    val latitude: String,
    val longitude: String
)
