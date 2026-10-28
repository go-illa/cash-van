package com.illa.cashvan.sales.ui.cashvan

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.illa.cashvan.sales.domain.cashvan.CashvanProduct
import com.illa.cashvan.sales.domain.checkout.PaymentType
import com.illa.cashvan.sales.domain.format.formatEGP
import com.illa.cashvan.sales.feature.cashvan.presentation.viewmodel.CashVanOrderUiState
import com.illa.cashvan.sales.feature.cashvan.presentation.viewmodel.CashVanOrderViewModel
import com.illa.cashvan.sales.ui.components.EmptyState
import com.illa.cashvan.sales.ui.components.LoadingBox
import com.illa.cashvan.sales.ui.components.SalesColors
import com.illa.cashvan.sales.ui.components.SalesPrimaryButton
import com.illa.cashvan.sales.ui.components.SalesTopBar
import com.illa.cashvan.sales.ui.components.ZainBold
import com.illa.cashvan.sales.ui.components.ZainRegular
import com.illa.cashvan.sales.ui.components.playVictoryFanfare
import com.illa.cashvan.sales.ui.components.salesFieldColors
import com.illa.cashvan.sales.ui.order.InvoiceRead
import com.illa.cashvan.sales.ui.order.QuantityStepper
import com.illa.cashvan.sales.ui.order.readInvoice
import com.illa.cashvan.sales.ui.route.Pill
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
private fun CashVanMessages(state: CashVanOrderUiState, viewModel: CashVanOrderViewModel, host: SnackbarHostState) {
    LaunchedEffect(state.message) {
        state.message?.let {
            host.showSnackbar(it)
            viewModel.messageShown()
        }
    }
}

/** The PWA's `CashvanProductSelection`. Leaving drops the cart. */
@Composable
fun CashVanProductsScreen(
    viewModel: CashVanOrderViewModel,
    onBack: () -> Unit,
    onCheckout: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val host = remember { SnackbarHostState() }
    CashVanMessages(state, viewModel, host)
    val back = {
        viewModel.discard()
        onBack()
    }
    BackHandler(onBack = back)

    Scaffold(
        containerColor = SalesColors.Background,
        snackbarHost = { SnackbarHost(host) },
        topBar = {
            Column(modifier = Modifier.background(Color.White)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SalesTopBar(title = state.merchant?.merchantName ?: "اختر المنتجات", onBack = back, modifier = Modifier.weight(1f))
                    SpendGauge(state)
                }
                SpendTags(state)
                OutlinedTextField(
                    value = state.search,
                    onValueChange = viewModel::onSearch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true,
                    placeholder = { Text("بحث بالاسم أو الكود", color = Color(0xFFBDBDBD)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = SalesColors.Muted) },
                    shape = RoundedCornerShape(10.dp),
                    colors = salesFieldColors()
                )
            }
        },
        bottomBar = {
            if (state.cart.isNotEmpty()) {
                Box(modifier = Modifier
                    .background(Color.White)
                    .padding(16.dp)) {
                    SalesPrimaryButton(
                        text = "السلة (${state.cartUnits} منتج) - ${formatEGP(state.cartValue, 2)}",
                        onClick = onCheckout
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                state.isLoading -> LoadingBox()
                state.loadError != null -> EmptyState(icon = Icons.Outlined.Inventory2, title = state.loadError.orEmpty())
                state.products.isEmpty() -> EmptyState(icon = Icons.Outlined.Inventory2, title = "لا توجد منتجات متاحة")
                else -> {
                    val section = state.sectionCards
                    val grid = state.gridProducts
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (section.isNotEmpty()) {
                            item(span = { GridItemSpan(2) }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = SalesColors.Progress)
                                    Text(" موصى به لهذه الزيارة", fontSize = 16.sp, fontFamily = ZainBold)
                                }
                            }
                            items(section, key = { "rec-${it.product.planProductId}" }) { card ->
                                CashVanProductCard(
                                    product = card.product,
                                    quantity = state.cartQuantity(card.product.planProductId),
                                    recommended = card.isRecommended,
                                    recommendedQuantity = card.recommendedQuantity,
                                    onAdd = { viewModel.add(card.product) },
                                    onQuantityChange = { viewModel.setQuantity(card.product, it) }
                                )
                            }
                            item(span = { GridItemSpan(2) }) { HorizontalDivider(color = SalesColors.Border) }
                        }
                        if (grid.isEmpty() && section.isEmpty()) {
                            item(span = { GridItemSpan(2) }) { EmptyState(icon = Icons.Outlined.Search, title = "لا توجد نتائج") }
                        }
                        items(grid, key = { it.planProductId }) { product ->
                            CashVanProductCard(
                                product = product,
                                quantity = state.cartQuantity(product.planProductId),
                                recommended = false,
                                recommendedQuantity = state.recommendedQuantity(product.productId),
                                onAdd = { viewModel.add(product) },
                                onQuantityChange = { viewModel.setQuantity(product, it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpendGauge(state: CashVanOrderUiState) {
    val percent = state.spendPercent
    val recommendations = state.recommendations ?: return
    Box(modifier = Modifier.padding(end = 12.dp), contentAlignment = Alignment.Center) {
        when {
            percent != null -> {
                CircularProgressIndicator(
                    progress = { percent / 100f },
                    modifier = Modifier.size(52.dp),
                    color = if (percent >= 100) SalesColors.Success else SalesColors.Primary,
                    trackColor = SalesColors.Border,
                    strokeWidth = 5.dp
                )
                Text("$percent%", fontSize = 12.sp, fontFamily = ZainBold)
            }
            recommendations.last_visit_date == null -> Pill("أول زيارة", SalesColors.Primary)
        }
    }
}

@Composable
private fun SpendTags(state: CashVanOrderUiState) {
    val recommendations = state.recommendations ?: return
    if (recommendations.last_visit_date == null) return
    Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        recommendations.last_visit_total_value?.let { Pill("آخر زيارة: ${formatEGP(it)}", SalesColors.Muted) }
        recommendations.predicted_spending?.let { Pill("التقديري: ${formatEGP(it)}", SalesColors.Primary) }
    }
}

@Composable
private fun CashVanProductCard(
    product: CashvanProduct,
    quantity: Int,
    recommended: Boolean,
    recommendedQuantity: Int?,
    onAdd: () -> Unit,
    onQuantityChange: (Int) -> Unit
) {
    val soldOut = product.cashVanAvailable <= 0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(if (recommended) 2.dp else 1.dp, if (recommended) SalesColors.Progress else SalesColors.Border, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(product.productName, fontSize = 13.sp, fontFamily = ZainBold, maxLines = 2, color = if (soldOut) SalesColors.Muted else Color.Black)
        Text(product.sku, fontSize = 10.sp, color = SalesColors.Muted)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Pill(if (soldOut) "نفذ" else "متاح ${product.cashVanAvailable}", if (soldOut) SalesColors.Muted else SalesColors.Success)
            recommendedQuantity?.let { Pill("المقترح: $it", SalesColors.Progress) }
        }
        Text(
            when {
                !product.hasValidPrice -> "السعر غير متاح"
                quantity > 0 -> "${formatEGP(product.price, 2)} × $quantity = ${formatEGP(product.price * quantity, 2)}"
                else -> formatEGP(product.price, 2)
            },
            fontSize = 13.sp,
            fontFamily = ZainBold,
            color = SalesColors.Primary
        )
        if (quantity > 0) {
            QuantityStepper(value = quantity, max = product.cashVanAvailable, enabled = true, onValueChange = onQuantityChange)
        } else {
            IconButton(
                onClick = onAdd,
                enabled = !soldOut,
                modifier = Modifier
                    .size(36.dp)
                    .background(if (soldOut) SalesColors.Border else SalesColors.Primary, CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة", tint = Color.White)
            }
        }
    }
}

/** The PWA's `CashvanCheckout`. */
@Composable
fun CashVanCheckoutScreen(
    viewModel: CashVanOrderViewModel,
    onBack: () -> Unit,
    onCompleted: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val host = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    CashVanMessages(state, viewModel, host)
    BackHandler(enabled = state.placing) {}

    LaunchedEffect(state.completedTotal) {
        if (state.completedTotal != null) {
            delay(CELEBRATION_MS)
            viewModel.discard()
            onCompleted()
        }
    }
    val invoicePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            when (val result = readInvoice(context, uri)) {
                is InvoiceRead.Ok -> viewModel.setFawryInvoice(result.invoice)
                is InvoiceRead.Rejected -> host.showSnackbar(result.message)
            }
        }
    }

    Scaffold(
        containerColor = SalesColors.Background,
        snackbarHost = { SnackbarHost(host) },
        topBar = { SalesTopBar(title = "ملخص الطلب", subtitle = state.merchant?.merchantName, onBack = { if (!state.placing) onBack() }) },
        bottomBar = {
            Column(modifier = Modifier
                .background(Color.White)
                .padding(16.dp)) {
                Row {
                    Text("الإجمالي", fontFamily = ZainRegular, modifier = Modifier.weight(1f))
                    Text(formatEGP(state.total, 2), fontSize = 18.sp, fontFamily = ZainBold, color = SalesColors.Primary)
                }
                Spacer(Modifier.height(10.dp))
                SalesPrimaryButton(
                    text = if (state.placing) "جاري تأكيد الطلب..." else "تأكيد الطلب",
                    onClick = viewModel::placeOrder,
                    loading = state.placing,
                    enabled = state.cart.isNotEmpty() && state.rebate.errorMessage == null &&
                        !(state.paymentType == PaymentType.FAWRY && state.fawryInvoice == null)
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.cart, key = { it.planProductId }) { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.productName, fontFamily = ZainBold, fontSize = 14.sp)
                            Text(item.sku, fontSize = 11.sp, color = SalesColors.Muted)
                        }
                        IconButton(onClick = { viewModel.remove(item.planProductId) }, enabled = !state.placing) {
                            Icon(Icons.Outlined.Close, contentDescription = "حذف", tint = SalesColors.Danger)
                        }
                    }
                    SummaryLine("الكمية", "${item.quantity}")
                    SummaryLine("سعر الوحدة", formatEGP(item.priceDetails.finalPrice, 2))
                    if (item.priceDetails.cashDiscount > 0) {
                        SummaryLine(
                            "خصم نقدي",
                            "-${formatEGP(item.priceDetails.cashDiscount, 2)} × ${item.quantity} = -${formatEGP(item.priceDetails.cashDiscount * item.quantity, 2)}",
                            SalesColors.Success
                        )
                    }
                    SummaryLine("الإجمالي", formatEGP(item.unitPrice * item.quantity, 2), bold = true)
                }
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SummaryLine("المجموع الفرعي بالضريبة", formatEGP(state.subtotalWithVat, 2))
                    SummaryLine("الضريبة", formatEGP(state.vatTotal, 2))
                    if (state.cashDiscountTotal > 0) SummaryLine("مجموع الخصم النقدي", "-${formatEGP(state.cashDiscountTotal, 2)}", SalesColors.Success)
                    OutlinedTextField(
                        value = state.rebateInput,
                        onValueChange = viewModel::setRebate,
                        label = { Text("خصم مؤجل (اختياري)") },
                        singleLine = true,
                        isError = state.rebate.errorMessage != null,
                        supportingText = state.rebate.errorMessage?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        colors = salesFieldColors()
                    )
                }
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("طريقة الدفع", fontFamily = ZainBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(PaymentType.CASH to "نقدي", PaymentType.CREDIT to "آجل", PaymentType.FAWRY to "فوري").forEach { (type, label) ->
                            val selected = state.paymentType == type
                            Text(
                                label,
                                textAlign = TextAlign.Center,
                                fontFamily = ZainBold,
                                color = if (selected) Color.White else SalesColors.Primary,
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (selected) SalesColors.Primary else SalesColors.Primary.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                    .clickable(enabled = !state.placing) { viewModel.setPaymentType(type) }
                                    .padding(vertical = 12.dp)
                            )
                        }
                    }
                    if (state.paymentType == PaymentType.FAWRY) {
                        val invoice = state.fawryInvoice
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, if (invoice == null) SalesColors.Danger else SalesColors.Border, RoundedCornerShape(10.dp))
                                .clickable { invoicePicker.launch(arrayOf("image/*", "application/pdf")) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(if (invoice == null) Icons.Outlined.Upload else Icons.Outlined.Receipt, contentDescription = null, tint = SalesColors.Primary)
                            Text(
                                invoice?.fileName ?: "صورة الفاتورة / اختر صورة أو ملف PDF",
                                fontFamily = ZainBold,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                maxLines = 1
                            )
                            if (invoice != null) {
                                IconButton(onClick = { viewModel.setFawryInvoice(null) }) {
                                    Icon(Icons.Outlined.Close, contentDescription = "حذف", tint = SalesColors.Danger)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.placing) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(enabled = false) {},
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = SalesColors.Primary)
                Text("جاري تسجيل الطلب...", fontFamily = ZainBold, modifier = Modifier.padding(top = 12.dp))
                Text("يرجى الانتظار وعدم مغادرة الصفحة", fontSize = 12.sp, color = SalesColors.Muted)
            }
        }
    }
    state.completedTotal?.let { total ->
        LaunchedEffect(Unit) { playVictoryFanfare() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = SalesColors.Success, modifier = Modifier.size(56.dp))
                Text("طلبية جديدة مضافة بنجاح!", fontSize = 18.sp, fontFamily = ZainBold)
                Text("إجمالي قيمة الطلب", fontSize = 13.sp, color = SalesColors.Muted)
                Text(formatEGP(total, 2), fontSize = 22.sp, fontFamily = ZainBold, color = SalesColors.Primary)
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String, color: Color = Color.Black, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, fontSize = 13.sp, color = SalesColors.Muted, fontFamily = ZainRegular, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, color = color, fontFamily = if (bold) ZainBold else ZainRegular)
    }
}

private const val CELEBRATION_MS = 2_000L
