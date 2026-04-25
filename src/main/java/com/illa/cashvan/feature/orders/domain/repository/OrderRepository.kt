package com.illa.cashvan.feature.orders.domain.repository

import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.orders.data.model.CreateOrderRequest
import com.illa.cashvan.feature.orders.data.model.CreateOrderResponse
import com.illa.cashvan.feature.orders.data.model.MerchantSearchResponse
import com.illa.cashvan.feature.orders.data.model.OngoingPlanResponse
import com.illa.cashvan.feature.orders.data.model.Order
import com.illa.cashvan.feature.orders.data.model.OrdersResponse
import com.illa.cashvan.feature.orders.data.model.PlanProductsResponse
import com.illa.cashvan.feature.orders.data.model.ProductPriceCalculationResponse
import com.illa.cashvan.feature.orders.data.model.UpdateOrderRequest

interface OrderRepository {
    suspend fun getOrders(
        planId: String? = null,
        createdAtDateEq: String? = null,
        orderTypeEq: String? = null
    ): ApiResult<OrdersResponse>
    suspend fun getOrderById(orderId: String): ApiResult<Order>
    suspend fun getOngoingPlan(): ApiResult<OngoingPlanResponse?>
    suspend fun searchMerchants(query: String, page: Int = 1, items: Int = 20): ApiResult<MerchantSearchResponse>
    suspend fun getPlanProducts(planId: String, query: String? = null, priceTier: String? = null, page: Int = 1, items: Int = 20): ApiResult<PlanProductsResponse>
    suspend fun createOrder(request: CreateOrderRequest): ApiResult<CreateOrderResponse>
    suspend fun updateOrder(orderId: String, request: UpdateOrderRequest): ApiResult<Order>
    suspend fun getProductTotalPrice(
        planId: String,
        productId: String,
        orderId: String,
        quantity: Int
    ): ApiResult<ProductPriceCalculationResponse>
    suspend fun getCashVanProductTotalPrice(
        planId: String,
        productId: String,
        merchantId: String,
        quantity: Int
    ): ApiResult<ProductPriceCalculationResponse>
}