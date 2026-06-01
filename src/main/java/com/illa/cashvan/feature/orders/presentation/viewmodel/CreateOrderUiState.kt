package com.illa.cashvan.feature.orders.presentation.viewmodel

import com.illa.cashvan.feature.orders.data.model.MerchantItem
import com.illa.cashvan.feature.orders.data.model.OngoingPlanResponse
import com.illa.cashvan.feature.orders.data.model.PlanProduct
import com.illa.cashvan.feature.orders.presentation.mapper.ProductPriceInfo

data class CreateOrderUiState(
    val isLoading: Boolean = false,
    val currentPlan: OngoingPlanResponse? = null,
    val merchants: List<MerchantItem> = emptyList(),
    val merchantPage: Int = 1,
    val hasMoreMerchants: Boolean = true,
    val isLoadingMoreMerchants: Boolean = false,
    val products: List<PlanProduct> = emptyList(),
    val allProducts: List<PlanProduct> = emptyList(),
    val productPage: Int = 1,
    val hasMoreProducts: Boolean = true,
    val isLoadingMoreProducts: Boolean = false,
    val selectedMerchant: MerchantItem? = null,
    val selectedProducts: Map<String, Int> = emptyMap(),
    val merchantSearchQuery: String = "",
    val productSearchQuery: String = "",
    val isSearchingMerchants: Boolean = false,
    val isSearchingProducts: Boolean = false,
    val error: String? = null,
    val noPlanFound: Boolean = false,
    val orderCreated: Boolean = false,
    val orderCreationError: String? = null,
    val isPrinting: Boolean = false,
    val printStatus: String? = null,
    val invoiceText: String? = null,
    val productPrices: Map<String, ProductPriceInfo> = emptyMap(),
    val loadingPriceForProducts: Set<String> = emptySet(),
    val previewProductPrice: ProductPriceInfo? = null,
    val isLoadingPreviewPrice: Boolean = false,
    val rebateValue: String = "",
    val locationGranted: Boolean = false,
    val isGettingLocation: Boolean = false,
    val userLatitude: Double? = null,
    val userLongitude: Double? = null,
    val paymentType: String? = null,
    val isUpdatingMerchantName: Boolean = false,
    val updateMerchantNameError: String? = null,
    val merchantNameUpdated: Boolean = false
)
