package com.illa.cashvan.sales.ui.cashvan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.illa.cashvan.sales.domain.format.formatEGP
import com.illa.cashvan.sales.feature.cashvan.data.model.CashVanOrderRow
import com.illa.cashvan.sales.feature.cashvan.presentation.viewmodel.CashVanHomeUiState
import com.illa.cashvan.sales.feature.cashvan.presentation.viewmodel.CashVanHomeViewModel
import com.illa.cashvan.sales.ui.components.EmptyState
import com.illa.cashvan.sales.ui.components.LoadingBox
import com.illa.cashvan.sales.ui.components.SalesColors
import com.illa.cashvan.sales.ui.components.ZainBold
import com.illa.cashvan.sales.ui.components.ZainRegular
import com.illa.cashvan.sales.ui.route.Pill
import com.illa.cashvan.sales.ui.route.formatDateTime
import com.illa.cashvan.ui.common.CashVanHeader

private const val RECENT_ORDERS = 10

/** Cash van status labels, as the PWA's dashboard shows them. */
internal fun cashVanStatusLabel(status: String?): Pair<String, Color> = when (status) {
    "submitted", "fulfilled" -> "مكتمل" to SalesColors.Success
    "partially_fulfilled" -> "مكتمل جزئياً" to SalesColors.Success
    "canceled" -> "ملغي" to SalesColors.Danger
    else -> "قيد التنفيذ" to SalesColors.Progress
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashVanHomeScreen(
    viewModel: CashVanHomeViewModel,
    onSelectMerchant: () -> Unit,
    onOrderClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showInventory by remember { mutableStateOf(false) }
    var showAllOrders by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = SalesColors.Background,
        topBar = { CashVanHeader() },
        floatingActionButton = {
            if (state.plan != null) {
                FloatingActionButton(onClick = onSelectMerchant, containerColor = SalesColors.Primary, contentColor = Color.White) {
                    Icon(Icons.Default.Add, contentDescription = "طلب جديد")
                }
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading && state.plan != null,
            onRefresh = viewModel::load,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading && state.plan == null -> LoadingBox()
                state.failed -> EmptyState(
                    icon = Icons.Outlined.Inventory2,
                    title = "تعذر تحميل بيانات الكاش فان",
                    action = { TextButton(onClick = viewModel::load) { Text("حاول تاني", fontFamily = ZainBold) } }
                )
                state.noPlan -> EmptyState(icon = Icons.Outlined.Inventory2, title = "لا يوجد خطة نشطة")
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { StartOrderCard(onSelectMerchant) }
                    item { InventoryCard(state, onClick = { showInventory = true }) }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("الطلبات الأخيرة", fontSize = 18.sp, fontFamily = ZainBold, modifier = Modifier.weight(1f))
                            Pill("${state.orders.size}", SalesColors.Primary)
                            if (state.orders.size > RECENT_ORDERS && !showAllOrders) {
                                TextButton(onClick = { showAllOrders = true }) { Text("عرض الكل", fontFamily = ZainBold) }
                            }
                        }
                    }
                    if (state.orders.isEmpty()) {
                        item { EmptyState(icon = Icons.Outlined.ReceiptLong, title = "لا توجد طلبات سابقة") }
                    }
                    items(if (showAllOrders) state.orders else state.orders.take(RECENT_ORDERS), key = { it.id }) { order ->
                        OrderRow(order, onClick = { onOrderClick(order.id) })
                    }
                }
            }
        }
    }

    if (showInventory) {
        ModalBottomSheet(
            onDismissRequest = { showInventory = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                Text("تفاصيل المخزون", fontSize = 20.sp, fontFamily = ZainBold)
                Text("${state.products.size} منتج في الخطة الحالية", fontSize = 13.sp, color = SalesColors.Muted)
                if (state.products.isEmpty()) {
                    EmptyState(icon = Icons.Outlined.Inventory2, title = "لا توجد منتجات في الخطة")
                }
                LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
                    items(state.products, key = { it.id }) { product ->
                        val available = maxOf(product.cash_van_available_quantity, 0)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(product.product?.name.orEmpty(), fontFamily = ZainBold, fontSize = 14.sp, color = if (available == 0) SalesColors.Muted else Color.Black)
                                Text(product.product?.sku.orEmpty(), fontSize = 11.sp, color = SalesColors.Muted)
                            }
                            Pill(if (available == 0) "نفد" else "$available متاح", if (available == 0) SalesColors.Muted else SalesColors.Success)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StartOrderCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SalesColors.Primary, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Storefront, contentDescription = null, tint = Color.White)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text("اختيار تاجر", fontSize = 18.sp, fontFamily = ZainBold, color = Color.White)
            Text("ابدأ طلب جديد لتاجر", fontSize = 13.sp, fontFamily = ZainRegular, color = Color.White.copy(alpha = 0.8f))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.White)
    }
}

@Composable
private fun InventoryCard(state: CashVanHomeUiState, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("نظرة عامة على المخزون", fontSize = 16.sp, fontFamily = ZainBold, modifier = Modifier.weight(1f))
            Pill("نشطة", SalesColors.Success)
        }
        val cells = listOf(
            "تاريخ الخطة" to formatDateTime(state.plan?.created_at).substringBefore("،"),
            "المنتجات" to "${state.products.size}",
            "المتاحة" to "${state.availableProductCount}",
            "المُخصص" to "${state.assignedTotal}",
            "تم البيع" to "${state.soldTotal}",
            "الرصيد" to "${state.availableTotal}"
        )
        cells.chunked(3).forEach { row ->
            Row(modifier = Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, value) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(SalesColors.Background, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(value, fontSize = 15.sp, fontFamily = ZainBold, maxLines = 1)
                        Text(label, fontSize = 11.sp, color = SalesColors.Muted, fontFamily = ZainRegular)
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderRow(order: CashVanOrderRow, onClick: () -> Unit) {
    val (label, color) = cashVanStatusLabel(order.status)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(order.merchant?.sign_name ?: order.merchant?.name ?: "تاجر", fontSize = 14.sp, fontFamily = ZainBold, maxLines = 1)
            Text(
                listOfNotNull(order.formatted_code, formatDateTime(order.created_at)).joinToString(" • "),
                fontSize = 12.sp,
                color = SalesColors.Muted
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Pill(label, color)
            order.total_income?.let { Text(formatEGP(it, 2), fontSize = 13.sp, fontFamily = ZainBold, color = SalesColors.Primary) }
        }
        Box(Modifier.padding(start = 4.dp)) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = SalesColors.Muted)
        }
    }
}
