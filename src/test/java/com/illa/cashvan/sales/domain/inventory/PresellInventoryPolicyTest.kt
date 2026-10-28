package com.illa.cashvan.sales.domain.inventory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PresellInventoryPolicyTest {

    private fun config(enforcementEnabled: Boolean) = PresellInventoryConfig(
        enforcementEnabled = enforcementEnabled,
        routingStatus = PresellInventoryRoutingStatus.CONFIGURED,
    )

    private fun readState(
        enforcementEnabled: Boolean,
        config: PresellInventoryConfig? = config(enforcementEnabled),
        isStale: Boolean = false,
        loading: Boolean = false,
        refreshing: Boolean = false,
        error: String? = null,
    ) = PresellInventoryReadState(
        config = config,
        fetchedAt = "2026-07-13T12:00:00Z",
        isStale = isStale,
        loading = loading,
        refreshing = refreshing,
        error = error,
    )

    private fun context(enforcementEnabled: Boolean) = getPresellInventoryContext(readState(enforcementEnabled))

    private fun stock(status: PresellStockStatus, available: Double, sku: String = "SKU-1") =
        PresellStockBalance(sku = sku, status = status, available = available)

    private data class TestItem(val productId: String, val quantity: Int, val sku: String, val description: String)

    private data class SkuOnly(val sku: String)

    private data class Product(val id: String, val sku: String)

    // formatExpectedReceiptDate

    @Test
    fun `formats a date-only backend value in Arabic without changing its calendar day`() {
        assertEquals("١٤ أغسطس ٢٠٢٦", formatExpectedReceiptDate("2026-08-14"))
    }

    @Test
    fun `returns null for an absent or invalid date value`() {
        listOf(null, "", "2026-02-30", "14-08-2026").forEach { value ->
            assertNull("value=$value", formatExpectedReceiptDate(value))
        }
    }

    // inventory analytics classification

    @Test
    fun `maps actual, expected, and absent reservation sources to reporting values`() {
        assertEquals(
            InventoryAnalyticsAvailability.AVAILABLE,
            toPresellInventoryAnalyticsAvailability(ReservationSource.ACTUAL),
        )
        assertEquals(
            InventoryAnalyticsAvailability.EXPECTED,
            toPresellInventoryAnalyticsAvailability(ReservationSource.EXPECTED),
        )
        assertEquals(
            InventoryAnalyticsAvailability.UNKNOWN,
            toPresellInventoryAnalyticsAvailability(ReservationSource.NONE),
        )
    }

    // getPresellInventoryContext

    @Test
    fun `allows a fresh configured enforced snapshot`() {
        val context = getPresellInventoryContext(readState(true))
        assertEquals(true, context.enforcementEnabled)
        assertTrue(context.controlsReady)
        assertFalse(context.orderBlocked)
    }

    @Test
    fun `ignores stale inventory when enforcement is disabled`() {
        val context = getPresellInventoryContext(readState(false, isStale = true))
        assertEquals(false, context.enforcementEnabled)
        assertTrue(context.controlsReady)
        assertFalse(context.orderBlocked)
    }

    @Test
    fun `blocks controls while inventory is loading, refreshing, or errored`() {
        listOf(
            InventoryBlockReason.LOADING to readState(true, loading = true),
            InventoryBlockReason.REFRESHING to readState(true, refreshing = true),
            InventoryBlockReason.ERROR to readState(true, error = "network failed"),
        ).forEach { (reason, state) ->
            val context = getPresellInventoryContext(state)
            assertFalse("$reason controlsReady", context.controlsReady)
            assertTrue("$reason orderBlocked", context.orderBlocked)
            assertEquals(reason, context.blockReason)
        }
    }

    @Test
    fun `blocks enforced ordering on a stale snapshot`() {
        val context = getPresellInventoryContext(readState(true, isStale = true))
        assertFalse(context.controlsReady)
        assertTrue(context.orderBlocked)
        assertEquals(InventoryBlockReason.STALE, context.blockReason)
    }

    @Test
    fun `does not expose or enforce warehouse routing when enforcement is disabled`() {
        val context = getPresellInventoryContext(
            readState(
                false,
                config = config(false).copy(routingStatus = PresellInventoryRoutingStatus.WAREHOUSE_NOT_CONFIGURED),
            ),
        )
        assertTrue(context.controlsReady)
        assertFalse(context.orderBlocked)
        assertNull(context.blockReason)
    }

    // getPresellProductInventoryPolicy

    @Test
    fun `finds a normalized inventory key for a catalog SKU with surrounding whitespace`() {
        val balance = stock(PresellStockStatus.AVAILABLE, 5.0, "SKU-TRIMMED")
        assertSame(balance, getPresellStockBalance(mapOf("SKU-TRIMMED" to balance), "  SKU-TRIMMED  "))
    }

    @Test
    fun `caps an enforced available product at whole available units`() {
        val policy = getPresellProductInventoryPolicy(stock(PresellStockStatus.AVAILABLE, 8.9), context(true))

        assertEquals(ProductInventoryKind.AVAILABLE, policy.kind)
        assertTrue(policy.canAdd)
        assertEquals(8, policy.maxQuantity)
        assertEquals(8, policy.availableUnits)
        assertFalse(policy.advisory)
    }

    @Test
    fun `uses actual stock first and does not combine it with expected stock`() {
        val balance = stock(PresellStockStatus.AVAILABLE, 500.0).copy(
            actualAvailable = 500.0,
            expectedAvailable = 1000.0,
            maxReservable = 500.0,
            recommendedSource = ReservationSource.ACTUAL,
            expectedReceiptDate = "2026-08-04",
        )
        val policy = getPresellProductInventoryPolicy(balance, context(true))

        assertEquals(ReservationSource.ACTUAL, policy.reservationSource)
        assertEquals(500, policy.actualAvailableUnits)
        assertEquals(1000, policy.expectedAvailableUnits)
        assertEquals(500, policy.maxQuantity)
    }

    @Test
    fun `uses expected stock only after actual availability reaches zero`() {
        val balance = stock(PresellStockStatus.AVAILABLE, 1000.0).copy(
            actualAvailable = 0.0,
            expectedAvailable = 1000.0,
            maxReservable = 1000.0,
            recommendedSource = ReservationSource.EXPECTED,
            expectedReceiptDate = "2026-08-14",
        )
        val policy = getPresellProductInventoryPolicy(balance, context(true))

        assertEquals(ReservationSource.EXPECTED, policy.reservationSource)
        assertEquals(1000, policy.availableUnits)
        assertEquals(1000, policy.maxQuantity)
        assertTrue(policy.label, policy.label.contains("متوقع بتاريخ ١٤ أغسطس ٢٠٢٦"))
        assertFalse(policy.label.contains("غدًا"))
    }

    @Test
    fun `does not promise an expected receipt date when the backend omits it`() {
        val balance = stock(PresellStockStatus.AVAILABLE, 1000.0).copy(
            actualAvailable = 0.0,
            expectedAvailable = 1000.0,
            maxReservable = 1000.0,
            recommendedSource = ReservationSource.EXPECTED,
            expectedReceiptDate = null,
        )
        val policy = getPresellProductInventoryPolicy(balance, context(true))

        assertEquals("متوقع: ١٬٠٠٠ كرتونة", policy.label)
        assertFalse(policy.label.contains("غدًا"))
    }

    private val unavailableCases = listOf(
        ProductInventoryKind.OUT_OF_STOCK to stock(PresellStockStatus.OUT_OF_STOCK, 0.0),
        ProductInventoryKind.NOT_FOUND to stock(PresellStockStatus.NOT_FOUND, 0.0),
        ProductInventoryKind.MISSING to null,
    )

    @Test
    fun `blocks an enforced out_of_stock, not_found, or missing product`() {
        unavailableCases.forEach { (kind, balance) ->
            val policy = getPresellProductInventoryPolicy(balance, context(true))
            assertEquals(kind, policy.kind)
            assertFalse("$kind canAdd", policy.canAdd)
            assertEquals("$kind maxQuantity", 0, policy.maxQuantity)
            assertTrue("$kind enforcedBlock", policy.enforcedBlock)
        }
    }

    @Test
    fun `keeps an unenforced out_of_stock, not_found, or missing product orderable without advisory UI`() {
        unavailableCases.forEach { (kind, balance) ->
            val policy = getPresellProductInventoryPolicy(balance, context(false))
            assertEquals(kind, policy.kind)
            assertTrue("$kind canAdd", policy.canAdd)
            assertNull("$kind maxQuantity", policy.maxQuantity)
            assertFalse("$kind advisory", policy.advisory)
            assertFalse("$kind enforcedBlock", policy.enforcedBlock)
            assertEquals("$kind label", "", policy.label)
        }
    }

    // sortPresellProductsByAvailability

    @Test
    fun `keeps available SKUs first and moves unavailable warehouse SKUs to the bottom`() {
        val products = listOf(
            Product("out", "SKU-OUT"),
            Product("available-1", "SKU-AVAILABLE-1"),
            Product("not-found", "SKU-NOT-FOUND"),
            Product("missing", "SKU-MISSING"),
            Product("available-2", "SKU-AVAILABLE-2"),
        )
        val stocks = mapOf(
            "SKU-OUT" to stock(PresellStockStatus.OUT_OF_STOCK, 0.0, "SKU-OUT"),
            "SKU-AVAILABLE-1" to stock(PresellStockStatus.AVAILABLE, 4.0, "SKU-AVAILABLE-1"),
            "SKU-NOT-FOUND" to stock(PresellStockStatus.NOT_FOUND, 0.0, "SKU-NOT-FOUND"),
            "SKU-AVAILABLE-2" to stock(PresellStockStatus.AVAILABLE, 2.0, "SKU-AVAILABLE-2"),
        )

        assertEquals(
            listOf("available-1", "available-2", "out", "not-found", "missing"),
            sortPresellProductsByAvailability(products, stocks) { it.sku }.map { it.id },
        )
    }

    @Test
    fun `preserves the existing order inside each availability group`() {
        val products = listOf(
            Product("available-b", "SKU-B"),
            Product("available-a", "SKU-A"),
            Product("out-b", "SKU-OUT-B"),
            Product("out-a", "SKU-OUT-A"),
        )
        val stocks = mapOf(
            "SKU-B" to stock(PresellStockStatus.AVAILABLE, 1.0, "SKU-B"),
            "SKU-A" to stock(PresellStockStatus.AVAILABLE, 3.0, "SKU-A"),
            "SKU-OUT-B" to stock(PresellStockStatus.OUT_OF_STOCK, 0.0, "SKU-OUT-B"),
            "SKU-OUT-A" to stock(PresellStockStatus.NOT_FOUND, 0.0, "SKU-OUT-A"),
        )

        assertEquals(products, sortPresellProductsByAvailability(products, stocks) { it.sku })
    }

    // collectPresellItemsWithoutStockBalance

    private fun cartItem(productId: String, sku: String) =
        TestItem(productId = productId, quantity = 2, sku = sku, description = "Product $productId")

    @Test
    fun `collects cart items whose SKU has no published balance - warehouse not found`() {
        val items = listOf(
            cartItem("available", "SKU-AVAILABLE"),
            cartItem("not-found", "SKU-NOT-FOUND"),
            cartItem("no-row", "SKU-NO-ROW"),
        )
        // The RPC drops not_found balances entirely, so both SKU-NOT-FOUND and
        // SKU-NO-ROW are simply absent from stocks.
        val stocks = mapOf("SKU-AVAILABLE" to stock(PresellStockStatus.AVAILABLE, 3.0, "SKU-AVAILABLE"))

        assertEquals(
            listOf(items[1], items[2]),
            collectPresellItemsWithoutStockBalance(items, stocks, context(true)) { it.sku },
        )
    }

    @Test
    fun `keeps items with an available or out_of_stock balance`() {
        val items = listOf(cartItem("available", "SKU-AVAILABLE"), cartItem("out", "SKU-OUT"))
        val stocks = mapOf(
            "SKU-AVAILABLE" to stock(PresellStockStatus.AVAILABLE, 3.0, "SKU-AVAILABLE"),
            "SKU-OUT" to stock(PresellStockStatus.OUT_OF_STOCK, 0.0, "SKU-OUT"),
        )

        assertEquals(emptyList<TestItem>(), collectPresellItemsWithoutStockBalance(items, stocks, context(true)) { it.sku })
    }

    @Test
    fun `matches a balance through the trimmed-SKU fallback`() {
        val items = listOf(cartItem("padded", " SKU-PADDED "))
        val stocks = mapOf("SKU-PADDED" to stock(PresellStockStatus.AVAILABLE, 1.0, "SKU-PADDED"))

        assertEquals(emptyList<TestItem>(), collectPresellItemsWithoutStockBalance(items, stocks, context(true)) { it.sku })
    }

    @Test
    fun `reports nothing in advisory mode or while controls are not ready - advisory never removes anything`() {
        val items = listOf(cartItem("no-row", "SKU-NO-ROW"))
        val stocks = emptyMap<String, PresellStockBalance>()

        // Advisory mode: a valid product without a balance row must survive.
        assertEquals(emptyList<TestItem>(), collectPresellItemsWithoutStockBalance(items, stocks, context(false)) { it.sku })
        // Not ready (loading/routing/stale): balances are not trustworthy yet.
        val loading = getPresellInventoryContext(readState(true, loading = true))
        assertEquals(emptyList<TestItem>(), collectPresellItemsWithoutStockBalance(items, stocks, loading) { it.sku })
    }

    // collectPresellRequestItemsInStock

    private fun requestItems(vararg skus: String) = skus.map { SkuOnly(it) }

    private val actualStock = stock(PresellStockStatus.AVAILABLE, 5.0).copy(
        actualAvailable = 5.0,
        maxReservable = 5.0,
        recommendedSource = ReservationSource.ACTUAL,
    )

    @Test
    fun `collects deferred items whose SKU now has actual stock`() {
        val stocks = mapOf("SKU-1" to actualStock)
        assertEquals(
            listOf(SkuOnly("SKU-1")),
            collectPresellRequestItemsInStock(requestItems("SKU-1", "SKU-2"), stocks, context(true)) { it.sku },
        )
    }

    @Test
    fun `excludes expected-only balances - incoming receipts do not flip a deferred request`() {
        val stocks = mapOf(
            "SKU-1" to stock(PresellStockStatus.AVAILABLE, 1000.0).copy(
                actualAvailable = 0.0,
                expectedAvailable = 1000.0,
                maxReservable = 1000.0,
                recommendedSource = ReservationSource.EXPECTED,
            ),
        )
        assertEquals(
            emptyList<SkuOnly>(),
            collectPresellRequestItemsInStock(requestItems("SKU-1"), stocks, context(true)) { it.sku },
        )
    }

    @Test
    fun `excludes out-of-stock and missing balances`() {
        val stocks = mapOf("SKU-1" to stock(PresellStockStatus.OUT_OF_STOCK, 0.0))
        assertEquals(
            emptyList<SkuOnly>(),
            collectPresellRequestItemsInStock(requestItems("SKU-1", "SKU-2"), stocks, context(true)) { it.sku },
        )
    }

    @Test
    fun `reports no in-stock request items in advisory mode or while controls are not ready`() {
        val stocks = mapOf("SKU-1" to actualStock)
        val items = requestItems("SKU-1")
        assertEquals(emptyList<SkuOnly>(), collectPresellRequestItemsInStock(items, stocks, context(false)) { it.sku })
        val loading = getPresellInventoryContext(readState(true, loading = true))
        assertEquals(emptyList<SkuOnly>(), collectPresellRequestItemsInStock(items, stocks, loading) { it.sku })
    }

    // assessPresellCartInventory

    private val line = InventoryCartLine(sku = "SKU-1", description = "Test product", quantity = 10)

    @Test
    fun `keeps an excessive enforced line and blocks checkout for explicit correction`() {
        val assessment = assessPresellCartInventory(
            listOf(line),
            mapOf("SKU-1" to stock(PresellStockStatus.AVAILABLE, 6.0)),
            context(true),
        )

        assertTrue(assessment.blocked)
        assertEquals(1, assessment.blockingIssues.size)
        assertEquals(10, assessment.blockingIssues[0].requested)
        assertEquals(6, assessment.blockingIssues[0].available)
    }

    @Test
    fun `explains the forbidden actual-to-expected crossover`() {
        val assessment = assessPresellCartInventory(
            listOf(line),
            mapOf(
                "SKU-1" to stock(PresellStockStatus.AVAILABLE, 6.0).copy(
                    actualAvailable = 6.0,
                    expectedAvailable = 20.0,
                    maxReservable = 6.0,
                    recommendedSource = ReservationSource.ACTUAL,
                ),
            ),
            context(true),
        )
        val message = assessment.blockingIssues[0].message
        assertTrue(message, message.contains("يجب حجزها أولًا"))
        assertTrue(message, message.contains("المتاح والمتوقع"))
    }

    @Test
    fun `blocks a cart that mixes actual and expected SKUs`() {
        val assessment = assessPresellCartInventory(
            listOf(
                InventoryCartLine(sku = "ACTUAL", description = "Actual", quantity = 2),
                InventoryCartLine(sku = "EXPECTED", description = "Expected", quantity = 3),
            ),
            mapOf(
                "ACTUAL" to stock(PresellStockStatus.AVAILABLE, 10.0, "ACTUAL").copy(
                    actualAvailable = 10.0,
                    expectedAvailable = 0.0,
                    recommendedSource = ReservationSource.ACTUAL,
                ),
                "EXPECTED" to stock(PresellStockStatus.AVAILABLE, 8.0, "EXPECTED").copy(
                    actualAvailable = 0.0,
                    expectedAvailable = 8.0,
                    maxReservable = 8.0,
                    recommendedSource = ReservationSource.EXPECTED,
                ),
            ),
            context(true),
        )

        assertTrue(assessment.blocked)
        assertEquals(0, assessment.blockingIssues.size)
        assertTrue(assessment.systemMessage.orEmpty().contains("طلبًا منفصلًا"))
    }

    @Test
    fun `ignores stock differences when enforcement is disabled`() {
        val assessment = assessPresellCartInventory(
            listOf(line),
            mapOf("SKU-1" to stock(PresellStockStatus.AVAILABLE, 6.0)),
            context(false),
        )

        assertFalse(assessment.blocked)
        assertEquals(0, assessment.advisoryIssues.size)
        assertEquals(0, assessment.blockingIssues.size)
    }

    @Test
    fun `blocks checkout without mutating lines when inventory cannot be trusted`() {
        val assessment = assessPresellCartInventory(
            listOf(line),
            emptyMap(),
            getPresellInventoryContext(readState(true, refreshing = true)),
        )

        assertTrue(assessment.blocked)
        assertEquals(emptyList<PresellCartInventoryIssue>(), assessment.blockingIssues)
        assertEquals(emptyList<PresellCartInventoryIssue>(), assessment.advisoryIssues)
        assertTrue(assessment.systemMessage.orEmpty().contains("تحديث"))
    }

    // cartContainsExpectedReservations

    private val expectedLine = InventoryCartLine(sku = "SKU-1", description = "Test product", quantity = 5)

    private val expectedStock = stock(PresellStockStatus.AVAILABLE, 12.0).copy(
        actualAvailable = 0.0,
        expectedAvailable = 12.0,
        maxReservable = 12.0,
        recommendedSource = ReservationSource.EXPECTED,
        expectedReceiptDate = "2026-08-14",
    )

    @Test
    fun `detects a cart that reserves from expected stock`() {
        assertTrue(cartContainsExpectedReservations(listOf(expectedLine), mapOf("SKU-1" to expectedStock), context(true)))
    }

    @Test
    fun `returns false for an actual-available cart`() {
        assertFalse(
            cartContainsExpectedReservations(
                listOf(expectedLine),
                mapOf("SKU-1" to stock(PresellStockStatus.AVAILABLE, 12.0)),
                context(true),
            ),
        )
    }

    @Test
    fun `returns false for an empty cart and when enforcement is disabled`() {
        assertFalse(cartContainsExpectedReservations(emptyList(), mapOf("SKU-1" to expectedStock), context(true)))
        assertFalse(cartContainsExpectedReservations(listOf(expectedLine), mapOf("SKU-1" to expectedStock), context(false)))
    }

    @Test
    fun `detects a product that flipped to expected AFTER being added to the cart`() {
        // Added while actual, reviewed after the stock flipped to expected.
        assertTrue(cartContainsExpectedReservations(listOf(expectedLine), mapOf("SKU-1" to expectedStock), context(true)))
    }

    @Test
    fun `ignores out-of-stock or missing balances - they are not expected reservations`() {
        val context = context(true)
        assertFalse(
            cartContainsExpectedReservations(
                listOf(expectedLine),
                mapOf("SKU-1" to stock(PresellStockStatus.OUT_OF_STOCK, 0.0)),
                context,
            ),
        )
        assertFalse(cartContainsExpectedReservations(listOf(expectedLine), emptyMap(), context))
    }
}
