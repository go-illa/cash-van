package com.illa.cashvan.feature.orders.presentation.mapper

import com.illa.cashvan.feature.orders.data.model.Order
import com.illa.cashvan.ui.orders.ui_components.*
import java.text.SimpleDateFormat
import java.util.Locale

fun Order.toOrderItem(): OrderItem {
    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    val formattedTime = try {
        val date = inputFormat.parse(created_at)
        outputFormat.format(date ?: "")
    } catch (e: Exception) {
        created_at.substring(11, 16)
    }

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

fun Order.toOrderSpecs(): OrderSpecs {
    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    val (formattedDate, formattedTime) = try {
        val date = inputFormat.parse(created_at)
        Pair(
            dateFormat.format(date ?: ""),
            timeFormat.format(date ?: "")
        )
    } catch (e: Exception) {
        Pair(
            created_at.substring(0, 10),
            created_at.substring(11, 16)
        )
    }

    return OrderSpecs(
        orderNumber = formatted_code,
        orderDate = formattedDate.toString(),
        orderTime = formattedTime.toString()
    )
}

fun Order.toUIMerchant(): Merchant {
    return Merchant(
        id = merchant?.id?.toString() ?: "",
        name = merchant?.name ?: "غير محدد",
        phoneNumber = merchant?.phone_number ?: "",
        address = merchant?.address ?: ""
    )
}

fun Order.toPaymentSummary(): PaymentSummary {
    val totalAmount = total_income.toDoubleOrNull() ?: 0.0
    val taxPercentage = 10.0
    val subtotal = totalAmount / (1 + taxPercentage / 100)
    val tax = totalAmount - subtotal

    return PaymentSummary(
        subtotal = subtotal,
        tax = tax,
        taxPercentage = taxPercentage,
        total = totalAmount
    )
}

fun Order.toProductDetailsList(): List<ProductDetails> {
    return order_plan_products?.mapNotNull { orderPlanProduct ->
        orderPlanProduct.product?.let { product ->
            val unitPrice = product.price.toDoubleOrNull() ?: 0.0
            val quantity = orderPlanProduct.sold_quantity
            val totalPrice = unitPrice * quantity

            ProductDetails(
                productName = product.name,
                sku = product.sku_code,
                price = totalPrice,
                unitPrice = unitPrice,
                quantity = quantity
            )
        }
    } ?: emptyList()
}