package com.illa.cashvan.sales.ui.orders

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.illa.cashvan.sales.domain.checkout.paymentTypeLabel
import com.illa.cashvan.sales.domain.format.formatEGP
import com.illa.cashvan.sales.domain.orders.CancellablePresellOrder
import com.illa.cashvan.sales.domain.orders.PresellMerchantContact
import com.illa.cashvan.sales.domain.orders.PresellStatusGroup
import com.illa.cashvan.sales.domain.orders.canCancelPresellOrder
import com.illa.cashvan.sales.domain.orders.getPresellMerchantCallHref
import com.illa.cashvan.sales.domain.orders.getPresellStatusGroup
import com.illa.cashvan.sales.feature.orders.data.model.PresellHistoryOrder
import com.illa.cashvan.sales.feature.orders.presentation.viewmodel.OrderHistoryViewModel
import com.illa.cashvan.sales.ui.components.EmptyState
import com.illa.cashvan.sales.ui.components.LoadingBox
import com.illa.cashvan.sales.ui.components.SalesColors
import com.illa.cashvan.sales.ui.components.SalesTopBar
import com.illa.cashvan.sales.ui.components.ZainBold
import com.illa.cashvan.sales.ui.components.ZainRegular
import com.illa.cashvan.sales.ui.components.salesFieldColors
import com.illa.cashvan.sales.ui.route.Pill
import com.illa.cashvan.sales.ui.route.formatDateTime
import org.koin.androidx.compose.koinViewModel

/** This month's presell orders; unfulfilled orders can be cancelled within 4 hours. */
@Composable
fun OrderHistoryScreen(
    onBack: (() -> Unit)?,
    viewModel: OrderHistoryViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmCancel by remember { mutableStateOf<PresellHistoryOrder?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Scaffold(
        containerColor = SalesColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SalesTopBar(
                title = "سجل الطلبات",
                subtitle = "طلبات البري سيل الشهر ده",
                onBack = onBack,
                actions = {
                    IconButton(onClick = viewModel::load) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "تحديث", tint = SalesColors.Primary)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = state.search,
                onValueChange = viewModel::onSearch,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                singleLine = true,
                placeholder = { Text("ابحث باسم التاجر أو الكود", color = Color(0xFFBDBDBD)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = SalesColors.Muted) },
                shape = RoundedCornerShape(10.dp),
                colors = salesFieldColors()
            )
            when {
                state.isLoading -> LoadingBox()
                state.failed -> EmptyState(
                    icon = Icons.Outlined.Refresh,
                    title = "تعذر تحميل الطلبات",
                    action = { TextButton(onClick = viewModel::load) { Text("حاول تاني", fontFamily = ZainBold) } }
                )
                state.visibleOrders.isEmpty() -> EmptyState(icon = Icons.Outlined.ReceiptLong, title = "مفيش طلبات الشهر ده")
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.visibleOrders, key = { it.id }) { order ->
                        OrderCard(
                            order = order,
                            cancelling = state.cancellingId == order.id,
                            onCancel = { confirmCancel = order }
                        )
                    }
                }
            }
        }
    }

    confirmCancel?.let { order ->
        AlertDialog(
            onDismissRequest = { confirmCancel = null },
            title = { Text("إلغاء الطلب؟", fontFamily = ZainBold) },
            text = { Text("هيتم إلغاء طلب ${order.merchant?.name.orEmpty()} وتحرير الكميات المحجوزة.", fontFamily = ZainRegular) },
            confirmButton = {
                TextButton(onClick = {
                    confirmCancel = null
                    viewModel.cancel(order.id)
                }) { Text("إلغاء الطلب", color = SalesColors.Danger, fontFamily = ZainBold) }
            },
            dismissButton = { TextButton(onClick = { confirmCancel = null }) { Text("رجوع", fontFamily = ZainBold) } }
        )
    }
}

@Composable
private fun OrderCard(order: PresellHistoryOrder, cancelling: Boolean, onCancel: () -> Unit) {
    val context = LocalContext.current
    val group = getPresellStatusGroup(order.status)
    val statusColor = when (group) {
        PresellStatusGroup.FULFILLED, PresellStatusGroup.PARTIALLY_FULFILLED -> SalesColors.Success
        PresellStatusGroup.CANCELED, PresellStatusGroup.EXPIRED -> SalesColors.Danger
        else -> SalesColors.Progress
    }
    val cancellable = canCancelPresellOrder(object : CancellablePresellOrder {
        override val status = order.status
        override val orderDate = order.order_date
    })
    val callHref = getPresellMerchantCallHref(order.merchant?.let { merchant ->
        object : PresellMerchantContact {
            override val phoneNumber = merchant.phone_number
            override val secondaryPhone = merchant.secondary_phone
            override val phoneInvalid = merchant.phone_invalid == true
        }
    })

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(order.merchant?.name ?: "تاجر غير معروف", fontSize = 15.sp, fontFamily = ZainBold)
                Text(listOfNotNull(order.merchant?.merchant_code, formatDateTime(order.order_date)).joinToString(" • "), fontSize = 12.sp, color = SalesColors.Muted)
            }
            Pill(group.label, statusColor)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${order.total_items ?: order.sales_order_items.sumOf { it.quantity }} منتج • ${paymentTypeLabel(order.payment_type)}",
                fontSize = 13.sp,
                color = SalesColors.Muted,
                modifier = Modifier.weight(1f)
            )
            Text(formatEGP(order.total_value ?: 0.0, 2), fontSize = 15.sp, fontFamily = ZainBold, color = SalesColors.Primary)
        }
        order.cancellation_reason?.let { Text("سبب الإلغاء: $it", fontSize = 12.sp, color = SalesColors.Danger) }
        Row {
            if (callHref != null) {
                TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse(callHref))) }) {
                    Icon(Icons.Outlined.Phone, contentDescription = null, tint = SalesColors.Primary)
                    Text(" اتصل بالتاجر", fontFamily = ZainBold, color = SalesColors.Primary)
                }
            }
            if (cancellable) {
                TextButton(onClick = onCancel, enabled = !cancelling) {
                    Text(if (cancelling) "جاري الإلغاء..." else "إلغاء الطلب", fontFamily = ZainBold, color = SalesColors.Danger)
                }
            }
        }
    }
}
