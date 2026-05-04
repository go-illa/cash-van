package com.illa.cashvan.navigation

data object SplashKey
data object SignInKey
data object HomeKey
data object ProfileKey
data object InventoryKey
data class CreateOrderKey(val paymentType: String? = null)
data object CreateMerchantKey
data class OrderDetailsKey(val orderId: String)
data object CreateVisitWithoutOrderKey
data class VisitWithoutOrderDetailsKey(val visitId: String)