package com.illa.cashvan.fulfillment.navigation

data object HomeKey
data object ProfileKey
data object InventoryKey
data class CreateOrderKey(val paymentType: String? = null)
data object CreateMerchantKey
data class OrderDetailsKey(val orderId: String)
data object CreateVisitWithoutOrderKey
data class VisitWithoutOrderDetailsKey(val visitId: String)