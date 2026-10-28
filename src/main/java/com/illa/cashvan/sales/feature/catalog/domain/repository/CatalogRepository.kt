package com.illa.cashvan.sales.feature.catalog.domain.repository

import com.illa.cashvan.sales.feature.catalog.data.model.MerchantPricing
import com.illa.cashvan.sales.feature.catalog.data.model.PricedProductRow
import com.illa.cashvan.sales.feature.catalog.data.model.VendorCatalog
import kotlinx.serialization.json.JsonObject

interface CatalogRepository {
    /** Re-reads the merchant's tier and region; navigation state is only a fallback. Null if the read fails. */
    suspend fun getMerchantPricing(merchantId: String): MerchantPricing?

    /** Vendors with at least one product priced for [pricing]. Empty when the merchant has no region. */
    suspend fun getVendors(pricing: MerchantPricing): VendorCatalog

    /** Products the agent's regions can sell, priced for [pricing], optionally for one vendor. */
    suspend fun getProducts(pricing: MerchantPricing, principalId: String?): List<PricedProductRow>

    /** `product_prices` rows for the given products under [pricing], for refreshing cart prices. */
    suspend fun getPriceRows(productIds: List<String>, pricing: MerchantPricing): List<JsonObject>

    /** Raw `get_presell_inventory` payload: config, freshness and per-SKU balances. */
    suspend fun getPresellInventory(merchantId: String, skus: List<String>): JsonObject
}
