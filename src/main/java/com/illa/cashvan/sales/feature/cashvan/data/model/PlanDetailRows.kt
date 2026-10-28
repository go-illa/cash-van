package com.illa.cashvan.sales.feature.cashvan.data.model

import com.illa.cashvan.core.network.util.FlexibleDoubleSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** `plans/:id` with products, orders and their merchants, for the daily reconciliation. */
@Serializable
data class PlanDetailRow(
    val id: String,
    val code: String? = null,
    val plan_type: String? = null,
    val created_at: String? = null,
    val sales_closed_at: String? = null,
    val plan_products: List<PlanDetailProductRow> = emptyList(),
    val orders: List<PlanDetailOrderRow> = emptyList()
)

@Serializable
data class PlanDetailProductRow(
    val id: String,
    val product_id: String? = null,
    @Serializable(with = FlexibleDoubleSerializer::class) val assigned_quantity: Double? = null,
    @Serializable(with = FlexibleDoubleSerializer::class) val sold_quantity: Double? = null,
    @Serializable(with = FlexibleDoubleSerializer::class) val total_income: Double? = null,
    @Serializable(with = FlexibleDoubleSerializer::class) val total_available_quantity: Double? = null,
    @Serializable(with = FlexibleDoubleSerializer::class) val pre_sell_available_quantity: Double? = null,
    @Serializable(with = FlexibleDoubleSerializer::class) val cash_van_available_quantity: Double? = null,
    @Serializable(with = FlexibleDoubleSerializer::class) val pre_sell_sold_quantity: Double? = null,
    @Serializable(with = FlexibleDoubleSerializer::class) val cash_van_sold_quantity: Double? = null,
    val product: CashVanProductRef? = null
)

@Serializable
data class PlanDetailOrderRow(
    val id: String,
    val formatted_code: String? = null,
    val order_type: String? = null,
    val status: String? = null,
    /** A number (0/1/2) or a string key, depending on the endpoint. */
    val payment_type: JsonElement? = null,
    @Serializable(with = FlexibleDoubleSerializer::class) val total_income: Double? = null,
    @Serializable(with = FlexibleDoubleSerializer::class) val rebate_value: Double? = null,
    val created_at: String? = null,
    val merchant: PlanDetailMerchantRow? = null,
    val sales_order: PlanDetailSalesOrderRow? = null
)

@Serializable
data class PlanDetailMerchantRow(
    val id: String? = null,
    val name: String? = null,
    val sign_name: String? = null,
    val phone_number: String? = null
)

@Serializable
data class PlanDetailSalesOrderRow(
    val id: String? = null,
    val status: String? = null,
    val payment_type: String? = null
)

@Serializable
data class ProductPrincipalRow(
    val sku: String? = null,
    val principal_id: String? = null
)

@Serializable
data class PrincipalNameRow(
    val id: String,
    val name: String
)
