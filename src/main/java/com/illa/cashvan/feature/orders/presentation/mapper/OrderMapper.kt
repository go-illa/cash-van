package com.illa.cashvan.feature.orders.presentation.mapper

import com.illa.cashvan.feature.orders.data.model.Order
import com.illa.cashvan.ui.orders.ui_components.OrderItem
import com.illa.cashvan.ui.orders.ui_components.OrderProductItem
import java.text.SimpleDateFormat
import java.util.Locale

fun Order.toOrderItem(): OrderItem {
    // Format the created_at date
    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    val formattedTime = try {
        val date = inputFormat.parse(created_at)
        outputFormat.format(date ?: "")
    } catch (e: Exception) {
        created_at.substring(11, 16) // fallback to extract time
    }

    // Map order_plan_products to OrderProductItem
    val products = order_plan_products?.map { orderPlanProduct ->
        OrderProductItem(
            name = orderPlanProduct.product?.name ?: "منتج غير محدد",
            quantity = orderPlanProduct.sold_quantity
        )
    } ?: emptyList()

    return OrderItem(
        id = id.toString(),
        orderNumber = formatted_code,
        merchantName = merchant?.name ?: "غير محدد",
        phoneNumber = merchant?.phone_number ?: "",
        totalAmount = total_income.toDoubleOrNull() ?: 0.0,
        itemsCount = total_sold_quantity,
        date = formattedTime.toString(),
        products = products
    )
}