package com.illa.cashvan.sales.ui.order

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.illa.cashvan.sales.domain.format.formatEGP
import com.illa.cashvan.sales.domain.inventory.PresellInventoryContext
import com.illa.cashvan.sales.domain.inventory.PresellProductInventoryPolicy
import com.illa.cashvan.sales.domain.inventory.ProductInventoryKind
import com.illa.cashvan.sales.domain.inventory.ReservationSource
import com.illa.cashvan.sales.domain.inventory.formatArabicEgyptInteger
import com.illa.cashvan.sales.domain.inventory.formatExpectedReceiptDate
import com.illa.cashvan.sales.feature.cart.data.model.StoredProduct
import com.illa.cashvan.sales.ui.components.SalesColors
import com.illa.cashvan.sales.ui.components.SalesPrimaryButton
import com.illa.cashvan.sales.ui.components.ZainBold
import com.illa.cashvan.sales.ui.components.ZainRegular

/**
 * A product with its stock badge, cart stepper, and — for out-of-stock products — the deferred request
 * stepper ("طلب مؤجل"), as the PWA's `ProductCard`.
 */
@Composable
fun ProductCard(
    product: StoredProduct,
    policy: PresellProductInventoryPolicy,
    inventoryContext: PresellInventoryContext,
    cartQuantity: Int,
    requestQuantity: Int,
    onAdd: () -> Unit,
    onCartQuantityChange: (Int) -> Unit,
    onRequestQuantityChange: (Int) -> Unit
) {
    val controlsReady = inventoryContext.controlsReady
    val enforced = inventoryContext.enforcementEnabled == true && controlsReady
    val maxQuantity = if (controlsReady) policy.maxQuantity else cartQuantity
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(product.description, fontSize = 15.sp, fontFamily = ZainBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(product.sku, fontSize = 11.sp, color = SalesColors.Muted)
                }
                Spacer(Modifier.width(8.dp))
                Text(formatEGP(product.price, 2), fontSize = 15.sp, fontFamily = ZainBold, color = SalesColors.Primary)
            }
            if (enforced) StockBadge(policy)

            when {
                cartQuantity > 0 -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("في السلة", fontSize = 13.sp, color = SalesColors.Success, fontFamily = ZainBold, modifier = Modifier.weight(1f))
                    QuantityStepper(
                        value = cartQuantity,
                        max = maxQuantity,
                        enabled = controlsReady,
                        onValueChange = onCartQuantityChange
                    )
                }
                enforced && policy.kind == ProductInventoryKind.OUT_OF_STOCK -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("طلب مؤجل", fontSize = 13.sp, color = SalesColors.Progress, fontFamily = ZainBold, modifier = Modifier.weight(1f))
                    if (requestQuantity > 0) {
                        QuantityStepper(value = requestQuantity, max = null, enabled = true, onValueChange = onRequestQuantityChange)
                    } else {
                        TextButton(onClick = { onRequestQuantityChange(1) }) {
                            Text("أضف للطلب المؤجل", fontFamily = ZainBold, color = SalesColors.Progress)
                        }
                    }
                }
                else -> FilledTonalButton(
                    onClick = onAdd,
                    enabled = controlsReady || inventoryContext.enforcementEnabled != true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Outlined.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("أضف للسلة", fontFamily = ZainBold)
                }
            }
        }
    }
}

@Composable
private fun StockBadge(policy: PresellProductInventoryPolicy) {
    val expected = policy.reservationSource == ReservationSource.EXPECTED
    val (text, color) = when {
        policy.kind == ProductInventoryKind.AVAILABLE && expected -> {
            val date = formatExpectedReceiptDate(policy.expectedReceiptDate)
            "${arabicNumber(policy.availableUnits)} كرتونة متوقعة${date?.let { " بتاريخ $it" }.orEmpty()}" to SalesColors.Progress
        }
        policy.kind == ProductInventoryKind.AVAILABLE -> "${arabicNumber(policy.availableUnits)} كرتونة متاحة الآن" to SalesColors.Success
        policy.kind == ProductInventoryKind.OUT_OF_STOCK -> "نفد المخزون" to SalesColors.Danger
        policy.kind == ProductInventoryKind.NOT_FOUND -> "غير موجود بالمخزن" to SalesColors.Danger
        policy.kind == ProductInventoryKind.MISSING -> "رصيد غير منشور" to SalesColors.Muted
        else -> "الرصيد غير متاح" to SalesColors.Muted
    }
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (expected) Icon(Icons.Outlined.Schedule, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(text, fontSize = 12.sp, fontFamily = ZainBold, color = color)
    }
}

private fun arabicNumber(value: Int): String = formatArabicEgyptInteger(value)

@Composable
fun QuantityStepper(
    value: Int,
    max: Int?,
    enabled: Boolean,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .border(1.dp, SalesColors.Border, RoundedCornerShape(8.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onValueChange(value - 1) }, enabled = enabled, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Remove, contentDescription = "تقليل", tint = SalesColors.Primary)
        }
        Text("$value", fontSize = 16.sp, fontFamily = ZainBold, modifier = Modifier.padding(horizontal = 10.dp))
        IconButton(
            onClick = { onValueChange(value + 1) },
            enabled = enabled && (max == null || value < max),
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "زيادة", tint = SalesColors.Primary)
        }
    }
}

@Composable
fun CartBar(
    itemCount: Int,
    total: Double,
    requestCount: Int,
    onCheckout: () -> Unit
) {
    if (itemCount == 0 && requestCount == 0) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(formatEGP(total, 2), fontSize = 18.sp, fontFamily = ZainBold, color = SalesColors.Primary)
            Text(
                buildString {
                    append("$itemCount منتج")
                    if (requestCount > 0) append(" • $requestCount طلب مؤجل")
                },
                fontSize = 12.sp,
                color = SalesColors.Muted,
                fontFamily = ZainRegular
            )
        }
        SalesPrimaryButton(
            text = "مراجعة الطلب",
            onClick = onCheckout,
            modifier = Modifier.width(170.dp)
        )
    }
}
