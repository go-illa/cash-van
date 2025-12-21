package com.illa.cashvan.feature.orders.domain.repository

import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.orders.data.model.CreateOrderRequest
import com.illa.cashvan.feature.orders.data.model.CreateOrderResponse
import com.illa.cashvan.feature.orders.data.model.MerchantSearchResponse
import com.illa.cashvan.feature.orders.data.model.OngoingPlanResponse
import com.illa.cashvan.feature.orders.data.model.Order
import com.illa.cashvan.feature.orders.data.model.OrdersResponse
import com.illa.cashvan.feature.orders.data.model.PlanProductsResponse

interface OrderRepository {
    suspend fun getOrders(
        planId: String? = null,
        createdAtDateEq: String? = null
    ): ApiResult<OrdersResponse>
    suspend fun getOrderById(orderId: String): ApiResult<Order>
    suspend fun getOngoingPlan(): ApiResult<OngoingPlanResponse?>
    suspend fun searchMerchants(query: String): ApiResult<MerchantSearchResponse>
    suspend fun getPlanProducts(planId: String, query: String? = null): ApiResult<PlanProductsResponse>
    suspend fun createOrder(request: CreateOrderRequest): ApiResult<CreateOrderResponse>
}