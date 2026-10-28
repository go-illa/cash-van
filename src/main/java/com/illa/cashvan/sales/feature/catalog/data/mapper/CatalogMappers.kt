package com.illa.cashvan.sales.feature.catalog.data.mapper

import com.illa.cashvan.sales.domain.cart.CartItem
import com.illa.cashvan.sales.domain.cart.CartProduct
import com.illa.cashvan.sales.domain.cart.PriceInfo
import com.illa.cashvan.sales.domain.cart.PriceTier
import com.illa.cashvan.sales.domain.cart.getFinalPrice
import com.illa.cashvan.sales.domain.inventory.PresellInventoryConfig
import com.illa.cashvan.sales.domain.inventory.PresellInventoryRoutingStatus
import com.illa.cashvan.sales.domain.inventory.PresellStockBalance
import com.illa.cashvan.sales.domain.inventory.PresellStockStatus
import com.illa.cashvan.sales.domain.inventory.ReservationSource
import com.illa.cashvan.sales.feature.cart.data.model.StoredCartLine
import com.illa.cashvan.sales.feature.cart.data.model.StoredProduct
import com.illa.cashvan.sales.feature.catalog.data.model.PricedProductRow
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/** A `product_prices` row. Missing numbers default the way the PWA's `?? 0` reads them. */
fun JsonObject.toPriceInfo(): PriceInfo = PriceInfo(
    id = string("id").orEmpty(),
    productId = string("product_id").orEmpty(),
    priceTier = PriceTier.fromWire(string("price_tier")) ?: PriceTier.RETAIL,
    price = double("price") ?: 0.0,
    basePrice = double("base_price"),
    finalPrice = double("final_price"),
    discounted = boolean("discounted") ?: false,
    discountDetails = this["discount_details"]?.takeUnless { it is JsonNull }?.toString(),
    vatPercentage = double("vat_percentage") ?: 0.0,
    vatApplication = (this["vat_application"] as? JsonPrimitive)?.intOrNull ?: 0,
    startDate = string("start_date"),
    endDate = string("end_date"),
    active = boolean("active") ?: true,
    discountType = (this["discount_type"] as? JsonPrimitive)?.intOrNull,
    cashDiscount = double("cash_discount"),
    governorateId = string("governorate_id"),
    createdAt = string("created_at").orEmpty(),
    updatedAt = string("updated_at").orEmpty()
)

/** The product as the cart stores it: final price minus the cash discount, as the PWA's product page does. */
fun PricedProductRow.toStoredProduct(): StoredProduct {
    val priceRow = product_prices.firstOrNull()
    val priceInfo = priceRow?.toPriceInfo()
    return StoredProduct(
        id = id,
        description = description.orEmpty(),
        sku = sku,
        price = getFinalPrice(priceInfo) - (priceInfo?.cashDiscount ?: 0.0),
        principal_id = principal_id,
        image_url = image_url,
        price_info = priceRow
    )
}

fun StoredCartLine.toCartItem(): CartItem = CartItem(
    productId = product.id,
    quantity = quantity,
    product = CartProduct(
        id = product.id,
        description = product.description,
        sku = product.sku,
        price = product.price,
        principalId = product.principal_id,
        imageUrl = product.image_url,
        priceInfo = product.price_info?.toPriceInfo()
    ),
    principalName = principalName
)

/** Inventory as `get_presell_inventory` returns it. */
data class PresellInventorySnapshot(
    val config: PresellInventoryConfig?,
    val fetchedAt: String?,
    val isStale: Boolean,
    val stocks: Map<String, PresellStockBalance>
)

fun JsonObject.toInventorySnapshot(): PresellInventorySnapshot {
    val config = (this["config"] as? JsonObject)?.let { config ->
        PresellInventoryConfig(
            enforcementEnabled = config.boolean("enforcement_enabled") ?: false,
            // Unknown non-empty statuses block like an unconfigured warehouse, as the PWA does.
            routingStatus = config.string("routing_status")?.let {
                PresellInventoryRoutingStatus.fromWire(it) ?: PresellInventoryRoutingStatus.WAREHOUSE_NOT_CONFIGURED
            }
        )
    }
    val stocks = (this["stocks"] as? JsonObject).orEmpty().mapNotNull { (sku, value) ->
        val balance = (value as? JsonObject) ?: return@mapNotNull null
        sku to PresellStockBalance(
            sku = balance.string("sku") ?: sku,
            // An unknown status can't be sold; treat it as absent from the warehouse.
            status = PresellStockStatus.fromWire(balance.string("status")) ?: PresellStockStatus.NOT_FOUND,
            available = balance.double("available") ?: 0.0,
            actualAvailable = balance.double("actual_available"),
            expectedAvailable = balance.double("expected_available"),
            maxReservable = balance.double("max_reservable"),
            recommendedSource = ReservationSource.fromWire(balance.string("recommended_source")),
            expectedReceiptDate = balance.string("expected_receipt_date")
        )
    }.toMap()
    return PresellInventorySnapshot(
        config = config,
        fetchedAt = string("fetched_at"),
        isStale = boolean("is_stale") ?: true,
        stocks = stocks
    )
}

private fun JsonObject.primitive(key: String): JsonPrimitive? =
    (this[key] as? JsonPrimitive)?.takeUnless { it is JsonNull }

private fun JsonObject.string(key: String): String? = primitive(key)?.contentOrNull
private fun JsonObject.double(key: String): Double? = primitive(key)?.doubleOrNull
private fun JsonObject.boolean(key: String): Boolean? = primitive(key)?.booleanOrNull

