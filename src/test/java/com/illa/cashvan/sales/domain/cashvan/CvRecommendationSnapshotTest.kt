package com.illa.cashvan.sales.domain.cashvan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Ported from the PWA's src/lib/cvRecommendationSnapshot.test.ts.
class CvRecommendationSnapshotTest {

    private val priceDetails = CashvanPriceDetails(
        basePrice = 25.0,
        finalPrice = 25.0,
        vatPercentage = 14.0,
        vatAmount = 3.08,
        vatIsExclusive = true,
        discountAmount = 0.0,
        cashDiscount = 0.0,
    )

    private fun makeItem(productId: String, quantity: Int = 1, unitPrice: Double = 25.0) = CashvanCartItem(
        planProductId = "plan-$productId",
        productId = productId,
        productName = "Product $productId",
        sku = "SKU-$productId",
        imageUrl = null,
        quantity = quantity,
        maxQuantity = 50,
        unitPrice = unitPrice,
        priceDetails = priceDetails,
    )

    private fun rec(productId: String, score: Double, defaultQty: Double? = null) =
        RecommendedProduct(productId = productId, sku = "SKU-$productId", name = "Product $productId", score = score, defaultQty = defaultQty)

    private fun selected(productId: String, quantity: Int, unitPrice: Double, isRecommended: Boolean) =
        CvSelectedProductSnapshot(productId, "SKU-$productId", "Product $productId", quantity, unitPrice, isRecommended)

    @Test
    fun `joins offered vs taken quantities per recommended product`() {
        val recommendations = VisitRecommendationsResult(
            predictedSpending = 1500.0,
            recommendedProducts = listOf(rec("p1", 0.9, 6.9), rec("p2", 0.8, null), rec("p3", 0.7, 2.0)),
        )
        val items = listOf(makeItem("p1", quantity = 4), makeItem("p2", quantity = 1), makeItem("p9", quantity = 2))

        val snapshot = buildCvRecommendationSnapshot(items, recommendations, 151.0)

        assertEquals(
            listOf(
                // default_qty is floored; selected quantity comes from the live cart.
                CvRecommendedProductSnapshot("p1", "SKU-p1", "Product p1", recommendedQuantity = 6, selectedQuantity = 4),
                // null default_qty → 0 suggested, but the taken quantity is still joined.
                CvRecommendedProductSnapshot("p2", "SKU-p2", "Product p2", recommendedQuantity = 0, selectedQuantity = 1),
                // Recommended but never added → 0 taken.
                CvRecommendedProductSnapshot("p3", "SKU-p3", "Product p3", recommendedQuantity = 2, selectedQuantity = 0),
            ),
            snapshot.recommendedProducts,
        )
        assertEquals(1500.0, snapshot.recommendedTotalSpending ?: Double.NaN, 0.0)
        assertEquals(151.0, snapshot.merchantTotalAmount, 0.0)
    }

    @Test
    fun `maps every cart line with its recommendation flag`() {
        val recommendations = VisitRecommendationsResult(recommendedProducts = listOf(rec("p1", 0.9)))
        val items = listOf(makeItem("p1", quantity = 4, unitPrice = 25.5), makeItem("p9", quantity = 1, unitPrice = 10.0))

        val snapshot = buildCvRecommendationSnapshot(items, recommendations, 112.0)

        assertEquals(
            listOf(selected("p1", 4, 25.5, isRecommended = true), selected("p9", 1, 10.0, isRecommended = false)),
            snapshot.selectedProducts,
        )
    }

    @Test
    fun `returns null recommendation fields when there is no payload`() {
        val snapshot = buildCvRecommendationSnapshot(listOf(makeItem("p9")), null, 25.0)

        assertNull(snapshot.recommendedProducts)
        assertNull(snapshot.recommendedTotalSpending)
        assertEquals(listOf(selected("p9", 1, 25.0, isRecommended = false)), snapshot.selectedProducts)
    }

    @Test
    fun `treats a payload without a prediction as null spending, and an empty offer list as empty`() {
        val emptyOffer = VisitRecommendationsResult(recommendedProducts = emptyList())

        // [] means "engine offered nothing"; null means "no payload at all".
        assertEquals(emptyList<CvRecommendedProductSnapshot>(), buildCvRecommendationSnapshot(emptyList(), emptyOffer, 0.0).recommendedProducts)
        assertNull(buildCvRecommendationSnapshot(emptyList(), emptyOffer, 0.0).recommendedTotalSpending)
    }

    // Kotlin-only: the analytics property keeps the PWA's snake_case shape.
    @Test
    fun `serializes to the PWA's event property shape`() {
        val snapshot = buildCvRecommendationSnapshot(
            listOf(makeItem("p1", quantity = 4, unitPrice = 25.5)),
            VisitRecommendationsResult(predictedSpending = 1500.0, recommendedProducts = listOf(rec("p1", 0.9, 6.9))),
            102.0,
        )

        assertEquals(
            mapOf(
                "recommended_products" to listOf(
                    mapOf(
                        "product_id" to "p1",
                        "sku" to "SKU-p1",
                        "name" to "Product p1",
                        "recommended_quantity" to 6,
                        "selected_quantity" to 4,
                    ),
                ),
                "recommended_total_spending" to 1500.0,
                "selected_products" to listOf(
                    mapOf(
                        "product_id" to "p1",
                        "sku" to "SKU-p1",
                        "name" to "Product p1",
                        "quantity" to 4,
                        "unit_price" to 25.5,
                        "is_recommended" to true,
                    ),
                ),
                "merchant_total_amount" to 102.0,
            ),
            snapshot.toEventProperty(),
        )
        assertEquals(null, buildCvRecommendationSnapshot(emptyList(), null, 0.0).toEventProperty()["recommended_products"])
    }
}
