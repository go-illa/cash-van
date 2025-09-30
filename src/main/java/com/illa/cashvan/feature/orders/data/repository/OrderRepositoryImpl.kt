package com.illa.cashvan.feature.orders.data.repository

import com.illa.cashvan.core.network.endpoint.ApiEndpoints
import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.orders.data.model.CreateOrderRequest
import com.illa.cashvan.feature.orders.data.model.CreateOrderResponse
import com.illa.cashvan.feature.orders.data.model.MerchantSearchResponse
import com.illa.cashvan.feature.orders.data.model.OngoingPlanResponse
import com.illa.cashvan.feature.orders.data.model.Order
import com.illa.cashvan.feature.orders.data.model.OrdersResponse
import com.illa.cashvan.feature.orders.data.model.PlanProductsResponse
import com.illa.cashvan.feature.orders.domain.repository.OrderRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType

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

    override suspend fun getOngoingPlan(): ApiResult<OngoingPlanResponse> {
        return try {
            val config = ApiEndpoints.Plans.getOngoingPlan()
            val versionedPath = "v${config.version}/${config.path}"

            val response = httpClient.request(versionedPath) {
                method = HttpMethod.Get
            }.body<OngoingPlanResponse>()

            ApiResult.Success(response)
        } catch (e: Exception) {
            ApiResult.Error(e, e.message ?: "Failed to fetch ongoing plan")
        }
    }

    override suspend fun searchMerchants(query: String): ApiResult<MerchantSearchResponse> {
        return try {
            val config = ApiEndpoints.Merchant.searchMerchants(query)
            val versionedPath = "v${config.version}/${config.path}"

            val response = httpClient.request(versionedPath) {
                method = HttpMethod.Get
                config.parameters?.forEach { key, values ->
                    url.parameters.appendAll(key, values)
                }
            }.body<MerchantSearchResponse>()

            ApiResult.Success(response)
        } catch (e: Exception) {
            ApiResult.Error(e, e.message ?: "Failed to search merchants")
        }
    }

    override suspend fun getPlanProducts(planId: String, query: String?): ApiResult<PlanProductsResponse> {
        return try {
            val config = ApiEndpoints.Plans.getPlanProducts(planId, query)
            val versionedPath = "v${config.version}/${config.path}"

            val response = httpClient.request(versionedPath) {
                method = HttpMethod.Get
                config.parameters?.forEach { key, values ->
                    url.parameters.appendAll(key, values)
                }
            }.body<PlanProductsResponse>()

            ApiResult.Success(response)
        } catch (e: Exception) {
            ApiResult.Error(e, e.message ?: "Failed to fetch plan products")
        }
    }

    override suspend fun createOrder(request: CreateOrderRequest): ApiResult<CreateOrderResponse> {
        return try {
            val config = ApiEndpoints.Orders.createOrder()
            val versionedPath = "v${config.version}/${config.path}"

            val response = httpClient.request(versionedPath) {
                method = HttpMethod.Post
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<CreateOrderResponse>()

            ApiResult.Success(response)
        } catch (e: Exception) {
            ApiResult.Error(e, e.message ?: "Failed to create order")
        }
    }
}