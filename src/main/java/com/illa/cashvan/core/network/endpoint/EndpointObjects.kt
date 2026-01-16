package com.illa.cashvan.core.network.endpoint

import io.ktor.http.HttpMethod
import io.ktor.http.Parameters

object ApiEndpoints {

    // Auth endpoints
    object Auth {
        fun signIn() = RequestConfiguration(
            path = "auth/login",
            method = HttpMethod.Post,
            parameterEncoding = ParameterEncoding.BODY,
            version = 2
        )

        fun refreshToken() = RequestConfiguration(
            path = "auth/refresh",
            method = HttpMethod.Post,
            parameterEncoding = ParameterEncoding.BODY,
            version = 1
        )

        fun logout() = RequestConfiguration(
            path = "auth/logout",
            method = HttpMethod.Delete,
            parameterEncoding = ParameterEncoding.BODY,
            version = 2
        )
    }

    // Profile endpoints
    object Profile {
        fun getProfile(salesAgentId: String) = RequestConfiguration(
            path = "sales_agents/$salesAgentId",
            method = HttpMethod.Get,
            parameters = Parameters.build {
                append("include", "supervisor")
            },
            parameterEncoding = ParameterEncoding.QUERY,
            version = 2
        )
    }

    // Plans endpoints
    object Plans {
        fun getPlans() = RequestConfiguration(
            path = "plans",
            method = HttpMethod.Get,
            parameterEncoding = ParameterEncoding.QUERY,
            version = 2
        )

        fun getOngoingPlan() = RequestConfiguration(
            path = "plans/ongoing",
            method = HttpMethod.Get,
            parameterEncoding = ParameterEncoding.QUERY,
            version = 2
        )

        fun getPlanProducts(planId: String, query: String? = null) = RequestConfiguration(
            path = "plans/$planId/plan_products",
            method = HttpMethod.Get,
            parameters = Parameters.build {
                append("include", "product")
                query?.let { append("q", it) }
            },
            parameterEncoding = ParameterEncoding.QUERY,
            version = 2
        )
    }

    // Merchant endpoints
    object Merchant {
        fun createMerchant() = RequestConfiguration(
            path = "merchants",
            method = HttpMethod.Post,
            parameterEncoding = ParameterEncoding.BODY,
            version = 2
        )

        fun searchMerchants(query: String) = RequestConfiguration(
            path = "merchants",
            method = HttpMethod.Get,
            parameters = Parameters.build {
                append("q", query)
            },
            parameterEncoding = ParameterEncoding.QUERY,
            version = 2
        )
    }

    // Orders endpoints
    object Orders {
        fun getOrders(
            planId: String? = null,
            createdAtDateEq: String? = null,
            orderTypeEq: String? = null
        ) = RequestConfiguration(
            path = "orders",
            method = HttpMethod.Get,
            parameters = Parameters.build {
                append("include", "order_plan_products,order_plan_products.product,order_plan_products.plan_product_price,merchant")
                planId?.let { append("f[plan_id_eq]", it) }
                createdAtDateEq?.let { append("f[created_at_day_lteq]", it) }
                orderTypeEq?.let { append("f[order_type_eq]", it) }
            },
            parameterEncoding = ParameterEncoding.QUERY,
            version = 2
        )

        fun getOrder(orderId: String) = RequestConfiguration(
            path = "orders/$orderId",
            method = HttpMethod.Get,
            parameters = Parameters.build {
                append("include", "order_plan_products,order_plan_products.product,order_plan_products.plan_product_price,merchant,invoice_attachment")
            },
            parameterEncoding = ParameterEncoding.QUERY,
            version = 2
        )

        fun createOrder() = RequestConfiguration(
            path = "orders",
            method = HttpMethod.Post,
            parameterEncoding = ParameterEncoding.BODY,
            version = 2
        )

        fun updateOrder(orderId: String) = RequestConfiguration(
            path = "orders/$orderId",
            method = HttpMethod.Put,
            parameterEncoding = ParameterEncoding.BODY,
            version = 2
        )
    }
}