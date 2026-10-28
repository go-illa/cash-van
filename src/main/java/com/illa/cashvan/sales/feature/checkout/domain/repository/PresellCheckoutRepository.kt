package com.illa.cashvan.sales.feature.checkout.domain.repository

import com.illa.cashvan.sales.domain.outofstock.SubmitOutOfStockRequestInput
import com.illa.cashvan.sales.domain.outofstock.SubmitOutOfStockRequestResult
import com.illa.cashvan.sales.feature.checkout.data.model.InventoryShortage
import com.illa.cashvan.sales.feature.checkout.data.model.PlaceOrderRequest
import com.illa.cashvan.sales.feature.checkout.data.model.PlacedOrder

interface PresellCheckoutRepository {
    /**
     * Creates the visit, order, lines and inventory reservations in one database transaction.
     * @throws OrderSubmissionException with a normalised [OrderSubmissionException.code].
     */
    suspend fun placeOrder(request: PlaceOrderRequest): PlacedOrder

    /** Uploads the Fawry receipt (a `data:` URL) and returns its public URL. */
    suspend fun uploadFawryInvoice(dataUrl: String, contentType: String): String

    /** Records the deferred out-of-stock request. Never throws; failures come back as a result. */
    suspend fun submitOutOfStockRequest(input: SubmitOutOfStockRequestInput): SubmitOutOfStockRequestResult
}

class OrderSubmissionException(
    val code: String,
    val shortages: List<InventoryShortage> = emptyList(),
    val reason: String? = null
) : Exception(reason ?: code)

object OrderSubmissionCodes {
    const val DUPLICATE_VISIT = "DUPLICATE_VISIT"
    const val MIXED_INVENTORY_SOURCES = "MIXED_INVENTORY_SOURCES_NOT_ALLOWED"
    const val EXPECTED_INVENTORY_UNAVAILABLE = "EXPECTED_INVENTORY_DISABLED_OR_STALE"
    const val INSUFFICIENT_STOCK = "INSUFFICIENT_STOCK"
    const val SUBMISSION_FAILED = "ORDER_SUBMISSION_FAILED"
    const val INVALID_RESPONSE = "INVALID_ORDER_RESPONSE"
}

/**
 * Maps a Postgres error from the order RPC to a stable code, as `normalizeOrderBackendError` does.
 */
fun normalizeOrderBackendError(code: String?, message: String?): OrderSubmissionException {
    val text = message.orEmpty()
    val lower = text.lowercase()
    val isDuplicateVisit = code == "P0001" && "visit" in lower && "merchant" in lower &&
        ("5 minutes" in lower || "already exists" in lower)
    val normalized = when {
        isDuplicateVisit -> OrderSubmissionCodes.DUPLICATE_VISIT
        OrderSubmissionCodes.MIXED_INVENTORY_SOURCES in text -> OrderSubmissionCodes.MIXED_INVENTORY_SOURCES
        OrderSubmissionCodes.EXPECTED_INVENTORY_UNAVAILABLE in text -> OrderSubmissionCodes.EXPECTED_INVENTORY_UNAVAILABLE
        !code.isNullOrBlank() -> code
        else -> OrderSubmissionCodes.SUBMISSION_FAILED
    }
    return OrderSubmissionException(normalized, emptyList(), text.trim().ifEmpty { null })
}
