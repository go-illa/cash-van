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

    // Orders endpoints
    object Orders {
        fun getOrders(page: Int = 1, limit: Int = 20) = RequestConfiguration(
            path = "orders",
            method = HttpMethod.Get,
            parameters = Parameters.build {
                append("page", page.toString())
                append("limit", limit.toString())
            },
            parameterEncoding = ParameterEncoding.QUERY,
            version = 1
        )

        fun getOrder(orderId: String) = RequestConfiguration(
            path = "orders/$orderId",
            method = HttpMethod.Get,
            version = 1
        )

        fun createOrder() = RequestConfiguration(
            path = "orders",
            method = HttpMethod.Post,
            parameterEncoding = ParameterEncoding.BODY,
            version = 1
        )

        fun updateOrder(orderId: String) = RequestConfiguration(
            path = "orders/$orderId",
            method = HttpMethod.Put,
            parameterEncoding = ParameterEncoding.BODY,
            version = 1
        )

        fun deleteOrder(orderId: String) = RequestConfiguration(
            path = "orders/$orderId",
            method = HttpMethod.Delete,
            version = 1
        )
    }

    // Inventory endpoints
    object Inventory {
        fun getProducts(page: Int = 1, limit: Int = 20) = RequestConfiguration(
            path = "inventory/products",
            method = HttpMethod.Get,
            parameters = Parameters.build {
                append("page", page.toString())
                append("limit", limit.toString())
            },
            parameterEncoding = ParameterEncoding.QUERY,
            version = 1
        )

        fun getProduct(productId: String) = RequestConfiguration(
            path = "inventory/products/$productId",
            method = HttpMethod.Get,
            version = 1
        )

        fun addProduct() = RequestConfiguration(
            path = "inventory/products",
            method = HttpMethod.Post,
            parameterEncoding = ParameterEncoding.BODY,
            version = 1
        )

        fun updateProduct(productId: String) = RequestConfiguration(
            path = "inventory/products/$productId",
            method = HttpMethod.Put,
            parameterEncoding = ParameterEncoding.BODY,
            version = 1
        )
    }
}

object ApiFields {
    // Auth fields
    object Auth {
        const val EMAIL = "email"
        const val PASSWORD = "password"
        const val CONFIRM_PASSWORD = "confirmPassword"
        const val REFRESH_TOKEN = "refreshToken"
        const val ACCESS_TOKEN = "accessToken"
    }

    // Profile fields
    object Profile {
        const val FIRST_NAME = "firstName"
        const val LAST_NAME = "lastName"
        const val PHONE = "phone"
        const val AVATAR = "avatar"
    }

    // Order fields
    object Order {
        const val ID = "id"
        const val MERCHANT_ID = "merchantId"
        const val PRODUCTS = "products"
        const val TOTAL_AMOUNT = "totalAmount"
        const val STATUS = "status"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
    }

    // Product fields
    object Product {
        const val ID = "id"
        const val NAME = "name"
        const val DESCRIPTION = "description"
        const val PRICE = "price"
        const val QUANTITY = "quantity"
        const val SKU = "sku"
        const val CATEGORY = "category"
    }
}

object ApiQueryParams {
    const val PAGE = "page"
    const val LIMIT = "limit"
    const val SORT = "sort"
    const val ORDER = "order"
    const val SEARCH = "search"
    const val FILTER = "filter"
    const val START_DATE = "startDate"
    const val END_DATE = "endDate"
}