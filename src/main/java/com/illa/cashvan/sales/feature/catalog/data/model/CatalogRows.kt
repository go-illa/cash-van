package com.illa.cashvan.sales.feature.catalog.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class RegionIdRow(
    val region_id: String
)

@Serializable
data class ProductIdRow(
    val product_id: String
)

@Serializable
data class MerchantPricingRow(
    val price_tier: String? = null,
    val region_id: String? = null
)

@Serializable
data class PricedProductRow(
    val id: String = "",
    val principal_id: String? = null,
    val sku: String,
    val description: String? = null,
    val image_url: String? = null,
    /** `product_prices!inner(...)` rows, already filtered to the merchant's tier and region. */
    val product_prices: List<JsonObject> = emptyList()
)

@Serializable
data class PrincipalRow(
    val id: String,
    val name: String,
    val vendor_image: String? = null
)

/** The merchant's price tier and region decide every price in a presell order. */
data class MerchantPricing(
    val priceTier: String,
    val regionId: String?
)

data class Vendor(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val productCount: Int
)

data class VendorCatalog(
    val pricing: MerchantPricing,
    val vendors: List<Vendor>,
    /** Priced SKUs per vendor, so vendors with no stocked product can be hidden once inventory loads. */
    val vendorSkus: List<Pair<String, String>>
)
