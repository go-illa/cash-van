package com.illa.cashvan.sales.domain.cashvan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

// Ported from the PWA's src/lib/cvRecommendedSection.test.ts.
class CvRecommendedSectionTest {

    private val priceDetails = CashvanPriceDetails(
        basePrice = 25.0,
        finalPrice = 25.0,
        vatPercentage = 14.0,
        vatAmount = 3.08,
        vatIsExclusive = true,
        discountAmount = 0.0,
        cashDiscount = 0.0,
    )

    private fun makeProduct(
        productId: String,
        cashVanAvailable: Int = 10,
        price: Double = 25.0,
        hasValidPrice: Boolean = true,
    ) = CashvanProduct(
        planProductId = "plan-$productId",
        productId = productId,
        productName = "Product $productId",
        sku = "SKU-$productId",
        imageUrl = null,
        assignedQuantity = 50,
        soldQuantity = 0,
        cashVanAvailable = cashVanAvailable,
        price = price,
        hasValidPrice = hasValidPrice,
        priceDetails = priceDetails,
    )

    private fun rec(productId: String, score: Double, defaultQty: Double? = null) =
        RecommendedProduct(productId = productId, sku = "SKU-$productId", name = "Product $productId", score = score, defaultQty = defaultQty)

    @Test
    fun `returns empty when the product list is unavailable or empty`() {
        val recommendations = VisitRecommendationsResult(recommendedProducts = listOf(rec("p1", 0.9, 2.0)))

        assertEquals(emptyList<RecommendedSectionCard>(), buildRecommendedSectionCards(null, recommendations, ""))
        assertEquals(emptyList<RecommendedSectionCard>(), buildRecommendedSectionCards(emptyList(), recommendations, ""))
    }

    @Test
    fun `returns empty while searching, even with joinable recommendations`() {
        val products = listOf(makeProduct("p1"))
        val recommendations = VisitRecommendationsResult(recommendedProducts = listOf(rec("p1", 0.9, 2.0)))

        assertEquals(emptyList<RecommendedSectionCard>(), buildRecommendedSectionCards(products, recommendations, "شيبسي"))
        assertEquals(1, buildRecommendedSectionCards(products, recommendations, "   ").size)
    }

    @Test
    fun `merges available and off-truck entries ordered by score DESC across both lists`() {
        val products = listOf(
            makeProduct("p1"),
            makeProduct("p2"),
            makeProduct("p3", cashVanAvailable = 0), // sold-out plan row
        )
        // Available entries deliberately out of score order, and below the off-truck entry's score.
        val recommendations = VisitRecommendationsResult(
            recommendedProducts = listOf(rec("p2", 0.5, 1.0), rec("p1", 0.7, 3.0)),
            offTruckProducts = listOf(rec("p3", 0.9, 4.0)),
        )

        val cards = buildRecommendedSectionCards(products, recommendations, "")

        assertEquals(listOf("p3", "p1", "p2"), cards.map { it.product.productId })
        // Off-truck entries are NOT flagged as recommended; sold-out rendering comes from the product row.
        assertEquals(listOf(false, true, true), cards.map { it.isRecommended })
        assertEquals(0, cards[0].product.cashVanAvailable)
    }

    @Test
    fun `breaks score ties by product_id ASC for a deterministic order`() {
        val products = listOf(makeProduct("p2"), makeProduct("p1"))
        val recommendations = VisitRecommendationsResult(
            recommendedProducts = listOf(rec("p2", 0.5)),
            offTruckProducts = listOf(rec("p1", 0.5)),
        )

        val cards = buildRecommendedSectionCards(products, recommendations, "")

        assertEquals(listOf("p1", "p2"), cards.map { it.product.productId })
    }

    @Test
    fun `filters entries absent from the truck plan or without a valid price`() {
        val products = listOf(
            makeProduct("p1"),
            makeProduct("p2", hasValidPrice = false, price = 0.0),
            // p3 exists only in the payload — not a plan row on this truck.
        )
        val recommendations = VisitRecommendationsResult(
            recommendedProducts = listOf(rec("p1", 0.7, 2.0), rec("p3", 0.9, 5.0)),
            offTruckProducts = listOf(rec("p2", 0.8), rec("p4", 0.6)),
        )

        val cards = buildRecommendedSectionCards(products, recommendations, "")

        assertEquals(listOf("p1"), cards.map { it.product.productId })
    }

    @Test
    fun `floors usable default_qty and drops invalid ones`() {
        val products = listOf(makeProduct("p1"), makeProduct("p2"), makeProduct("p3"), makeProduct("p4"))
        val recommendations = VisitRecommendationsResult(
            recommendedProducts = listOf(
                rec("p1", 0.4, 2.9),
                rec("p2", 0.3, null),
                rec("p3", 0.2, 0.0),
                rec("p4", 0.1), // absent
            ),
        )

        val cards = buildRecommendedSectionCards(products, recommendations, "")

        assertEquals(listOf(2, null, null, null), cards.map { it.recommendedQuantity })
    }

    @Test
    fun `builds an all-off-truck section when the 6-cap is hit before any stocked product`() {
        // recommended_products = [] while off_truck_products is populated: the section still renders,
        // entirely informational sold-out cards.
        val products = listOf(makeProduct("p1", cashVanAvailable = 0), makeProduct("p2", cashVanAvailable = 0))
        val recommendations = VisitRecommendationsResult(
            recommendedProducts = emptyList(),
            offTruckProducts = listOf(rec("p2", 0.8, 1.0), rec("p1", 0.9, 2.0)),
        )

        val cards = buildRecommendedSectionCards(products, recommendations, "")

        assertEquals(listOf("p1", "p2"), cards.map { it.product.productId })
        assertTrue(cards.none { it.isRecommended })
    }

    @Test
    fun `removes section products from the grid`() {
        val grid = listOf(makeProduct("p1"), makeProduct("p2"))
        val sectionCards = listOf(
            RecommendedSectionCard(product = makeProduct("p1", cashVanAvailable = 0), isRecommended = false, recommendedQuantity = null),
        )

        assertEquals(listOf("p2"), excludeSectionProducts(grid, sectionCards) { it.productId }.map { it.productId })
    }

    @Test
    fun `returns the grid unchanged (same reference) when the section is empty`() {
        val grid = listOf(makeProduct("p1"))
        assertSame(grid, excludeSectionProducts(grid, emptyList()) { it.productId })
    }
}
