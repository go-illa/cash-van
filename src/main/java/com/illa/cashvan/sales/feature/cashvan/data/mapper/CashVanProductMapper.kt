package com.illa.cashvan.sales.feature.cashvan.data.mapper

import com.illa.cashvan.sales.domain.cashvan.CashvanPriceDetails
import com.illa.cashvan.sales.domain.cashvan.CashvanProduct
import com.illa.cashvan.sales.feature.cashvan.data.model.CashVanPlanProductRow
import com.illa.cashvan.sales.feature.cashvan.data.model.CashVanProductPriceRow

private val EMPTY_PRICE = CashvanPriceDetails(
    basePrice = 0.0,
    finalPrice = 0.0,
    vatPercentage = 0.0,
    vatAmount = 0.0,
    vatIsExclusive = false,
    discountAmount = 0.0,
    cashDiscount = 0.0
)

/**
 * Port of the PWA's `resolveCashvanPrice`: only a price row for exactly the merchant's tier and governorate
 * counts. Without a governorate there is no price.
 */
internal fun resolveCashVanPrice(
    prices: List<CashVanProductPriceRow>,
    merchantGovernorateId: String?,
    merchantPriceTier: String
): CashvanPriceDetails? {
    if (merchantGovernorateId.isNullOrEmpty()) return null
    val exact = prices.firstOrNull { it.price_tier == merchantPriceTier && it.governorate_id == merchantGovernorateId }
        ?: return null
    val finalPrice = exact.final_price.finite()
    val basePrice = exact.base_price.finite().takeIf { it != 0.0 } ?: finalPrice
    val vatPercentage = exact.vat_percentage.finite()
    val unit = exact.price_details?.unit
    return CashvanPriceDetails(
        basePrice = basePrice,
        finalPrice = finalPrice,
        vatPercentage = vatPercentage,
        vatAmount = if (unit != null) unit.vat_amount.finite()
        else if (exact.vat_application == "exclusive") finalPrice * vatPercentage / 100 else 0.0,
        vatIsExclusive = exact.vat_application == "exclusive",
        discountAmount = if (unit != null) unit.discount_amount.finite()
        else if (exact.discounted == true) basePrice - finalPrice else 0.0,
        cashDiscount = exact.cash_discount.finite()
    )
}

/**
 * Plan products the merchant can be sold: priced for their tier and governorate, and either on the truck or
 * with a non-zero price (the PWA's `useCashvanProducts` filter).
 */
fun List<CashVanPlanProductRow>.toCashVanProducts(governorateId: String?, priceTier: String): List<CashvanProduct> =
    map { row ->
        val price = resolveCashVanPrice(row.product_prices, governorateId, priceTier)
        val details = price ?: EMPTY_PRICE
        CashvanProduct(
            planProductId = row.id,
            productId = row.product_id ?: row.product?.id.orEmpty(),
            productName = row.product?.name.orEmpty(),
            sku = row.product?.sku.orEmpty(),
            imageUrl = row.product?.image_url,
            assignedQuantity = row.assigned_quantity,
            soldQuantity = row.sold_quantity,
            cashVanAvailable = maxOf(row.cash_van_available_quantity, 0),
            price = details.finalPrice - details.cashDiscount,
            hasValidPrice = price != null,
            priceDetails = details
        )
    }.filter { it.hasValidPrice && (it.cashVanAvailable > 0 || it.price > 0) }

private fun Double?.finite(): Double = this?.takeIf { it.isFinite() } ?: 0.0
