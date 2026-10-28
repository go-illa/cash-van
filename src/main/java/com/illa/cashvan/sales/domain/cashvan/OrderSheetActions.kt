package com.illa.cashvan.sales.domain.cashvan

import com.illa.cashvan.sales.domain.orders.CancellablePresellOrder
import com.illa.cashvan.sales.domain.orders.PresellStatusGroup
import com.illa.cashvan.sales.domain.orders.canCancelPresellOrder
import com.illa.cashvan.sales.domain.orders.getPresellStatusGroup
import java.time.Instant
import java.time.ZoneId

/** Which order-history tab the detail sheet was opened from. */
enum class OrderSheetChannel(val wireValue: String) {
    PRESELL("presell"),
    RETAIL("retail"),
}

data class OrderSheetActions(val showCancel: Boolean, val showContact: Boolean)

/**
 * Buttons the order-history detail sheet offers. Cancel: only the agent's own
 * presell orders inside the cancel window (retail-app orders are cancellable
 * only by the merchant). Call-to-reorder: any expired order.
 */
fun getOrderSheetActions(
    order: CancellablePresellOrder,
    channel: OrderSheetChannel,
    now: Instant = Instant.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): OrderSheetActions = OrderSheetActions(
    showCancel = channel == OrderSheetChannel.PRESELL && canCancelPresellOrder(order, now, zone),
    showContact = getPresellStatusGroup(order.status) == PresellStatusGroup.EXPIRED,
)
