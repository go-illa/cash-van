package com.illa.cashvan.feature.orders.data.repository

import com.illa.cashvan.core.network.endpoint.ApiEndpoints
import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.orders.data.model.Order
import com.illa.cashvan.feature.orders.data.model.OrdersResponse
import com.illa.cashvan.feature.orders.domain.repository.OrderRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.request
import io.ktor.http.HttpMethod

class OrderRepositoryImpl(
    private val httpClient: HttpClient
) : OrderRepository {

    override suspend fun getOrders(createdAtDateEq: String?): ApiResult<OrdersResponse> {
        return try {
            val config = ApiEndpoints.Orders.getOrders(createdAtDateEq ?: "2025-09-28")
            val versionedPath = "v${config.version}/${config.path}"

            val response = httpClient.request(versionedPath) {
                method = HttpMethod.Get
                config.parameters?.forEach { key, values ->
                    url.parameters.appendAll(key, values)
                }
            }.body<OrdersResponse>()

            ApiResult.Success(response)
        } catch (e: Exception) {
            ApiResult.Error(e, e.message ?: "Failed to fetch orders")
        }
    }

    override suspend fun getOrderById(orderId: String): ApiResult<Order> {
        return try {
            val config = ApiEndpoints.Orders.getOrder(orderId)
            val versionedPath = "v${config.version}/${config.path}"

            val response = httpClient.request(versionedPath) {
                method = HttpMethod.Get
                config.parameters?.forEach { key, values ->
                    url.parameters.appendAll(key, values)
                }
            }.body<Order>()

            ApiResult.Success(response)
        } catch (e: Exception) {
            ApiResult.Error(e, e.message ?: "Failed to fetch order details")
        }
    }
}