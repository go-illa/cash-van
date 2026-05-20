package com.illa.cashvan.feature.merchant.data.model

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
