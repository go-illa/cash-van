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
        id = id,
        orderNumber = formatted_code,
        merchantName = merchant?.sign_name ?: merchant?.name ?: "غير محدد",
        phoneNumber = merchant?.phone_number ?: "",
        totalAmount = total_income.toDoubleOrNull() ?: 0.0,
        itemsCount = total_sold_quantity,
        date = formattedTime.toString(),
        products = products,
        status = status,
        orderType = order_type
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
        id = merchant?.id ?: "",
        name = merchant?.sign_name ?: merchant?.name ?: "",
        phoneNumber = merchant?.phone_number ?: "",
        address = merchant?.address ?: "",
        code = merchant?.code ?: ""
    )
}

fun Order.toPaymentSummary(): PaymentSummary {
    val totalAmount = total_income.toDoubleOrNull() ?: 0.0

    var subtotal = 0.0
    var totalTax = 0.0
    var totalDiscount = 0.0
    var totalCashDiscount = 0.0
    var avgTaxPercentage = 0.0
    var taxItemsCount = 0

    order_plan_products?.forEach { planProduct ->
        val totalPriceDetails = planProduct.total_price_details

        if (totalPriceDetails != null) {
            subtotal += totalPriceDetails.total?.base_price ?: 0.0
            totalTax += totalPriceDetails.total?.vat_amount ?: 0.0
            totalDiscount += totalPriceDetails.total?.discount_amount ?: 0.0
            totalCashDiscount += totalPriceDetails.total?.cash_discount_amount ?: 0.0

            val vatPercentage = totalPriceDetails.vat_percentage ?: 0.0
            if (vatPercentage > 0) {
                avgTaxPercentage += vatPercentage
                taxItemsCount++
            }
        } else {
            val quantity = planProduct.sold_quantity
            val priceDetails = planProduct.plan_product_price?.price_details
            val basePrice = planProduct.plan_product_price?.base_price?.toDoubleOrNull() ?: 0.0

            subtotal += basePrice * quantity

            val vatAmount = priceDetails?.vat_amount ?: 0.0
            totalTax += vatAmount * quantity

            val discountAmount = priceDetails?.discount_amount ?: 0.0
            totalDiscount += discountAmount * quantity

            val vatPercentage = planProduct.plan_product_price?.vat_percentage ?: 0.0
            if (vatPercentage > 0) {
                avgTaxPercentage += vatPercentage
                taxItemsCount++
            }
        }
    }

    val taxPercentage = if (taxItemsCount > 0) {
        avgTaxPercentage / taxItemsCount
    } else {
        0.0
    }

    return PaymentSummary(
        subtotal = subtotal,
        taxAmount = totalTax,
        taxPercentage = taxPercentage,
        discountAmount = totalDiscount,
        cashDiscountAmount = totalCashDiscount,
        total = totalAmount
    )
}
