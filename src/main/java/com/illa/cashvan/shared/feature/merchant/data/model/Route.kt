package com.illa.cashvan.shared.feature.merchant.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Route(
    val id: String,
    val name: String
)

@Serializable
data class RoutesResponse(
    val routes: List<Route>
)
