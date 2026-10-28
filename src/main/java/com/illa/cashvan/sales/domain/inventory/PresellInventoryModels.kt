package com.illa.cashvan.sales.domain.inventory

// Minimal domain mirror of the get_presell_inventory RPC payload
// (PWA: src/hooks/usePresellInventory.ts) — only the fields the policy reads.

enum class PresellStockStatus(val wireValue: String) {
    AVAILABLE("available"),
    OUT_OF_STOCK("out_of_stock"),
    NOT_FOUND("not_found"),
    ;

    companion object {
        /** Null for an absent or unrecognised status. */
        fun fromWire(value: String?): PresellStockStatus? = entries.firstOrNull { it.wireValue == value }
    }
}

enum class ReservationSource(val wireValue: String) {
    ACTUAL("actual"),
    EXPECTED("expected"),
    NONE("none"),
    ;

    companion object {
        /** Null for an absent or unrecognised source; the policy then treats it as [NONE]. */
        fun fromWire(value: String?): ReservationSource? = entries.firstOrNull { it.wireValue == value }
    }
}

enum class PresellInventoryRoutingStatus(val wireValue: String) {
    CONFIGURED("configured"),
    MERCHANT_NOT_FOUND("merchant_not_found"),
    MERCHANT_REGION_MISSING("merchant_region_missing"),
    WAREHOUSE_NOT_CONFIGURED("warehouse_not_configured"),
    ;

    companion object {
        /**
         * Null stays null (inventory state unknown). Any other unrecognised value
         * maps to [WAREHOUSE_NOT_CONFIGURED]: the PWA blocks such a value with the
         * same "routing" reason and fallback message, so ordering stays fail-closed.
         */
        fun fromWire(value: String?): PresellInventoryRoutingStatus? {
            if (value == null) return null
            return entries.firstOrNull { it.wireValue == value } ?: WAREHOUSE_NOT_CONFIGURED
        }
    }
}

data class PresellStockBalance(
    val sku: String,
    val status: PresellStockStatus,
    val available: Double,
    val actualAvailable: Double? = null,
    val expectedAvailable: Double? = null,
    val maxReservable: Double? = null,
    val recommendedSource: ReservationSource? = null,
    /** Date-only `yyyy-MM-dd` from the backend. */
    val expectedReceiptDate: String? = null,
)

data class PresellInventoryConfig(
    val enforcementEnabled: Boolean,
    val routingStatus: PresellInventoryRoutingStatus?,
)

data class PresellInventoryReadState(
    val config: PresellInventoryConfig?,
    val fetchedAt: String?,
    val isStale: Boolean,
    val loading: Boolean,
    val refreshing: Boolean,
    val error: String?,
)

enum class InventoryBlockReason(val wireValue: String) {
    LOADING("loading"),
    REFRESHING("refreshing"),
    ERROR("error"),
    ROUTING("routing"),
    STALE("stale"),
    UNKNOWN("unknown"),
}

data class PresellInventoryContext(
    val enforcementEnabled: Boolean?,
    val routingStatus: PresellInventoryRoutingStatus?,
    val controlsReady: Boolean,
    val orderBlocked: Boolean,
    val blockReason: InventoryBlockReason?,
    val blockMessage: String?,
)

enum class ProductInventoryKind(val wireValue: String) {
    AVAILABLE("available"),
    OUT_OF_STOCK("out_of_stock"),
    NOT_FOUND("not_found"),
    MISSING("missing"),
    UNAVAILABLE("unavailable"),
}

data class PresellProductInventoryPolicy(
    val kind: ProductInventoryKind,
    val availableUnits: Int,
    val actualAvailableUnits: Int,
    val expectedAvailableUnits: Int,
    val reservationSource: ReservationSource,
    val expectedReceiptDate: String?,
    val canAdd: Boolean,
    /** Null means "no cap" (the TS `undefined`). */
    val maxQuantity: Int?,
    val advisory: Boolean,
    val enforcedBlock: Boolean,
    val label: String,
)

/** Structural stand-in for the TS `{ quantity, product: { sku, description } }` cart line shape. */
interface InventoryCartItemLike {
    val quantity: Int
    val sku: String
    val description: String
}

data class InventoryCartLine(
    override val sku: String,
    override val description: String,
    override val quantity: Int,
) : InventoryCartItemLike

data class PresellCartInventoryIssue(
    val sku: String,
    val description: String,
    val requested: Int,
    val available: Int,
    val kind: ProductInventoryKind,
    val message: String,
)

data class PresellCartInventoryAssessment(
    val blocked: Boolean,
    val systemMessage: String?,
    val blockingIssues: List<PresellCartInventoryIssue>,
    val advisoryIssues: List<PresellCartInventoryIssue>,
)

enum class InventoryAnalyticsAvailability(val wireValue: String) {
    AVAILABLE("available"),
    EXPECTED("expected"),
    UNKNOWN("unknown"),
}
