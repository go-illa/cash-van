package com.illa.cashvan.sales.feature.cashvan.data.mapper

import com.illa.cashvan.sales.domain.cashvan.RailsMerchant
import com.illa.cashvan.sales.domain.cashvan.RailsOrder
import com.illa.cashvan.sales.domain.cashvan.RailsPlanProduct
import com.illa.cashvan.sales.domain.cashvan.RailsPreSellSalesOrder
import com.illa.cashvan.sales.domain.cashvan.RailsProduct
import com.illa.cashvan.sales.domain.cashvan.RawPaymentType
import com.illa.cashvan.sales.feature.cashvan.data.model.PlanDetailOrderRow
import com.illa.cashvan.sales.feature.cashvan.data.model.PlanDetailProductRow
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

fun PlanDetailProductRow.toRailsPlanProduct(): RailsPlanProduct = RailsPlanProduct(
    id = id,
    productId = product_id ?: product?.id.orEmpty(),
    assignedQuantity = assigned_quantity,
    soldQuantity = sold_quantity,
    totalIncome = total_income,
    totalAvailableQuantity = total_available_quantity,
    preSellAvailableQuantity = pre_sell_available_quantity,
    cashVanAvailableQuantity = cash_van_available_quantity,
    preSellSoldQuantity = pre_sell_sold_quantity,
    cashVanSoldQuantity = cash_van_sold_quantity,
    product = product?.let { RailsProduct(it.id ?: product_id.orEmpty(), it.name.orEmpty(), it.sku.orEmpty(), it.image_url) }
)

fun PlanDetailOrderRow.toRailsOrder(): RailsOrder = RailsOrder(
    id = id,
    formattedCode = formatted_code.orEmpty(),
    orderType = order_type,
    status = status.orEmpty(),
    // A JSON number is a payment code; a string, even "1", is a key — the PWA tells them apart.
    paymentType = (payment_type as? JsonPrimitive)?.takeUnless { it is JsonNull }?.let { value ->
        if (value.isString) RawPaymentType.Key(value.content) else value.intOrNull?.let(RawPaymentType::Code)
    },
    totalIncome = total_income,
    createdAt = created_at.orEmpty(),
    rebateValue = rebate_value,
    merchant = merchant?.let { RailsMerchant(it.id.orEmpty(), it.name.orEmpty(), it.sign_name, it.phone_number) },
    salesOrder = sales_order?.let { RailsPreSellSalesOrder(it.id.orEmpty(), it.status.orEmpty(), it.payment_type.orEmpty()) }
)
