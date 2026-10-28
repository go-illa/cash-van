package com.illa.cashvan.sales.domain.outofstock

// DIS-446: out-of-stock demand requests — the pure halves of the PWA's
// submitOutOfStockRequest. The submission itself is a single call to the
// SECURITY DEFINER RPC public.submit_out_of_stock_request (owned by the
// repository); these functions prepare its payload and map its response.

data class OutOfStockRequestItemInput(
    val productId: String,
    /** Int by type, so the PWA's Number.isInteger check reduces to "positive". */
    val quantity: Int,
)

data class SubmitOutOfStockRequestInput(
    val requestId: String,
    val agentId: String,
    val merchantId: String,
    val items: List<OutOfStockRequestItemInput>,
)

sealed interface SubmitOutOfStockRequestResult {
    data object Submitted : SubmitOutOfStockRequestResult

    data object IdempotentReplay : SubmitOutOfStockRequestResult

    data object RoutingFailed : SubmitOutOfStockRequestResult

    /**
     * Already placed/rejected by telesales: retrying the same persisted
     * requestId can never succeed, so the caller must clear the local basket
     * instead of offering a retry (otherwise the agent loops forever).
     */
    data object TerminalStatus : SubmitOutOfStockRequestResult

    data class Failed(val message: String) : SubmitOutOfStockRequestResult
}

const val OUT_OF_STOCK_NO_VALID_ITEMS_MESSAGE = "لا توجد منتجات صالحة في الطلب المؤجل"
const val OUT_OF_STOCK_GENERIC_FAILURE_MESSAGE = "تعذر إرسال الطلب المؤجل، حاول مرة أخرى"

/**
 * Client-side fast path (the RPC re-validates everything): positive
 * quantities only, one line per product — the LAST entry's quantity wins while
 * the product keeps the position of its first entry (JS Map semantics).
 * An empty result means: return `Failed(OUT_OF_STOCK_NO_VALID_ITEMS_MESSAGE)`
 * without calling the RPC.
 */
fun collectValidOutOfStockItems(items: List<OutOfStockRequestItemInput>): List<OutOfStockRequestItemInput> {
    val quantitiesByProduct = LinkedHashMap<String, Int>()
    for (item in items) {
        if (item.quantity > 0) quantitiesByProduct[item.productId] = item.quantity
    }
    return quantitiesByProduct.map { (productId, quantity) -> OutOfStockRequestItemInput(productId, quantity) }
}

/**
 * Server status → page-facing result. 'no_valid_items', 'forbidden', the RPC's
 * own 'failed' and any unknown status degrade to [SubmitOutOfStockRequestResult.Failed]
 * with an agent-readable Arabic message.
 */
fun mapOutOfStockRpcStatus(status: String?): SubmitOutOfStockRequestResult = when (status) {
    "submitted" -> SubmitOutOfStockRequestResult.Submitted
    "idempotent_replay" -> SubmitOutOfStockRequestResult.IdempotentReplay
    "routing_failed" -> SubmitOutOfStockRequestResult.RoutingFailed
    "no_valid_items" -> SubmitOutOfStockRequestResult.Failed(OUT_OF_STOCK_NO_VALID_ITEMS_MESSAGE)
    "terminal_status" -> SubmitOutOfStockRequestResult.TerminalStatus
    else -> SubmitOutOfStockRequestResult.Failed(OUT_OF_STOCK_GENERIC_FAILURE_MESSAGE)
}

/**
 * Maps the RPC outcome: a transport/PostgREST error wins with its own message
 * ([errorMessage] non-null means an error object was returned); a response
 * without data fails generically; otherwise the `status` field is mapped.
 */
fun resolveOutOfStockRpcResponse(
    errorMessage: String?,
    hasData: Boolean,
    status: String?,
): SubmitOutOfStockRequestResult = when {
    errorMessage != null -> SubmitOutOfStockRequestResult.Failed(errorMessage)
    !hasData -> SubmitOutOfStockRequestResult.Failed(OUT_OF_STOCK_GENERIC_FAILURE_MESSAGE)
    else -> mapOutOfStockRpcStatus(status)
}
