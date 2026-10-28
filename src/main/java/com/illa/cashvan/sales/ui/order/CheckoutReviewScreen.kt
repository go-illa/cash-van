package com.illa.cashvan.sales.ui.order

import android.net.Uri
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.illa.cashvan.sales.domain.checkout.ConfirmDisabledInput
import com.illa.cashvan.sales.domain.checkout.PaymentType
import com.illa.cashvan.sales.domain.checkout.isConfirmDisabled
import com.illa.cashvan.sales.domain.format.formatEGP
import com.illa.cashvan.sales.feature.order.presentation.viewmodel.FawryInvoice
import com.illa.cashvan.sales.feature.order.presentation.viewmodel.OrderCompletion
import com.illa.cashvan.sales.feature.order.presentation.viewmodel.PresellOrderUiState
import com.illa.cashvan.sales.feature.order.presentation.viewmodel.PresellOrderViewModel
import com.illa.cashvan.sales.ui.components.EmptyState
import com.illa.cashvan.sales.ui.components.SalesColors
import com.illa.cashvan.sales.ui.components.SalesPrimaryButton
import com.illa.cashvan.sales.ui.components.SalesTopBar
import com.illa.cashvan.sales.ui.components.ZainBold
import com.illa.cashvan.sales.ui.components.ZainRegular
import com.illa.cashvan.sales.ui.components.playVictoryFanfare
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val CELEBRATION_MS = 2_000L
private const val FANFARE_DELAY_MS = 300L
/** The PWA waits this long before leaving an empty checkout, so a basket still loading can cancel it. */
private const val EMPTY_CHECKOUT_EXIT_DELAY_MS = 200L

/**
 * The PWA's `PresellCheckoutReview`: priced cart lines (the only lines that create an order), deferred
 * out-of-stock requests (no prices), payment, and confirm.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutReviewScreen(
    viewModel: PresellOrderViewModel,
    onBack: () -> Unit,
    onCompleted: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDatePicker by remember { mutableStateOf(false) }

    // As the PWA: once both baskets are empty there is nothing to review, so go back to the products.
    LaunchedEffect(state.lanes.hasItems, state.placing, state.completion) {
        if (!state.lanes.hasItems && !state.placing && state.completion == null) {
            delay(EMPTY_CHECKOUT_EXIT_DELAY_MS)
            onBack()
        }
    }

    LaunchedEffect(Unit) { viewModel.onCheckoutOpened() }
    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHostState.showSnackbar(listOfNotNull(message.title, message.description).joinToString("\n"))
            viewModel.messageShown()
        }
    }
    LaunchedEffect(state.completion) {
        if (state.completion == OrderCompletion.CELEBRATE) {
            delay(CELEBRATION_MS)
            onCompleted()
        }
    }

    val invoicePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            when (val result = readInvoice(context, uri)) {
                is InvoiceRead.Ok -> viewModel.setFawryInvoice(result.invoice)
                is InvoiceRead.Rejected -> snackbarHostState.showSnackbar(result.message)
            }
        }
    }

    val confirmDisabled = isConfirmDisabled(
        ConfirmDisabledInput(
            submitting = state.placing,
            fawrySubmitting = false,
            requestSubmitting = false,
            reviewLocked = !state.inventoryContext.controlsReady && state.inventoryContext.enforcementEnabled == true,
            conflictDetected = state.conflictDetected,
            lanes = state.lanes,
            cartBlocked = state.cartAssessment.blocked,
            requestBlocked = state.lanes.hasDeferredItems && state.requestBlocked,
            locationReady = true,
            paymentType = state.paymentType,
            hasCollectionDate = state.collectionDate != null,
            hasFawryInvoice = state.fawryInvoice != null
        )
    )

    Scaffold(
        containerColor = SalesColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { SalesTopBar(title = "مراجعة الطلب", subtitle = state.context?.merchantName, onBack = onBack) },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                if (state.lanes.hasCartItems) {
                    Row {
                        Text("الإجمالي", fontSize = 15.sp, fontFamily = ZainRegular, modifier = Modifier.weight(1f))
                        Text(formatEGP(state.cartTotal, 2), fontSize = 18.sp, fontFamily = ZainBold, color = SalesColors.Primary)
                    }
                    Spacer(Modifier.height(10.dp))
                }
                SalesPrimaryButton(
                    text = when {
                        state.lanes.hasCartItems -> "تأكيد الطلب"
                        else -> "تسجيل الطلب المؤجل"
                    },
                    onClick = viewModel::confirmCheckout,
                    enabled = !confirmDisabled,
                    loading = state.placing
                )
            }
        }
    ) { padding ->
        if (!state.lanes.hasItems) {
            EmptyState(icon = Icons.Outlined.Receipt, title = "السلة فاضية", modifier = Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            state.inventoryContext.blockMessage?.let { message -> item { InventoryNotice(message) } }
            state.cartAssessment.systemMessage?.let { message -> item { InventoryNotice(message) } }
            if (state.conflictDetected) {
                item {
                    InventoryNotice("الطلبات المؤجلة تُسجل مع المنتجات المتاحة فقط أو في طلب منفصل. احذف أحد النوعين للمتابعة.")
                }
            }

            if (state.lanes.hasCartItems) {
                item { SectionTitle("المنتجات المتاحة (${state.cart.size})") }
                items(state.cart, key = { "cart-${it.product.id}" }) { line ->
                    val issue = (state.cartAssessment.blockingIssues + state.cartAssessment.advisoryIssues)
                        .firstOrNull { it.sku == line.product.sku }
                    LineCard(
                        title = line.product.description,
                        subtitle = "${formatEGP(line.product.price, 2)} × ${line.quantity} = ${formatEGP(line.product.price * line.quantity, 2)}",
                        warning = issue?.message,
                        quantity = line.quantity,
                        max = state.policyFor(line.product).maxQuantity,
                        enabled = !state.placing,
                        onQuantityChange = { viewModel.setCartQuantity(line.product, it) },
                        onRemove = { viewModel.setCartQuantity(line.product, 0) },
                        secondaryAction = if (issue != null) ("حوّل لطلب مؤجل" to { viewModel.convertToRequest(line.product) }) else null
                    )
                }
            }

            if (state.lanes.hasDeferredItems) {
                item { SectionTitle("طلبات مؤجلة (${state.requests.size})") }
                item {
                    Text(
                        "منتجات نفدت من المخزن. هتتسجل للمتابعة عند توفرها، من غير سعر ومن غير أوردر.",
                        fontSize = 12.sp,
                        color = SalesColors.Muted,
                        fontFamily = ZainRegular
                    )
                }
                items(state.requests, key = { "request-${it.product.id}" }) { line ->
                    LineCard(
                        title = line.product.description,
                        subtitle = line.product.sku,
                        warning = null,
                        quantity = line.quantity,
                        max = null,
                        enabled = !state.placing,
                        onQuantityChange = { viewModel.setRequestQuantity(line.product, it) },
                        onRemove = { viewModel.setRequestQuantity(line.product, 0) },
                        secondaryAction = null,
                        icon = Icons.Outlined.Schedule
                    )
                }
            }

            if (state.lanes.hasCartItems) {
                item { SectionTitle("طريقة الدفع") }
                item {
                    PaymentSection(
                        state = state,
                        onPaymentType = viewModel::setPaymentType,
                        onPickDate = { showDatePicker = true },
                        onPickInvoice = { invoicePicker.launch(arrayOf("image/*", "application/pdf")) },
                        onRemoveInvoice = { viewModel.setFawryInvoice(null) }
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val today = LocalDate.now()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.collectionDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isBefore(today)
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        viewModel.setCollectionDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showDatePicker = false
                }) { Text("تأكيد", fontFamily = ZainBold) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("إلغاء", fontFamily = ZainBold) } }
        ) {
            DatePicker(state = pickerState)
        }
    }

    when (state.completion) {
        OrderCompletion.CELEBRATE -> Celebration()
        OrderCompletion.REQUEST_SUBMITTED -> AlertDialog(
            onDismissRequest = onCompleted,
            title = { Text("تم تسجيل الطلب المؤجل", fontFamily = ZainBold) },
            text = { Text("هنتابع المنتجات دي وتتبلغ لما المخزون يتوفر.", fontFamily = ZainRegular) },
            confirmButton = { TextButton(onClick = onCompleted) { Text("تمام", fontFamily = ZainBold) } }
        )
        null -> Unit
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 16.sp, fontFamily = ZainBold, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun LineCard(
    title: String,
    subtitle: String,
    warning: String?,
    quantity: Int,
    max: Int?,
    enabled: Boolean,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit,
    secondaryAction: Pair<String, () -> Unit>?,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, if (warning != null) SalesColors.Danger.copy(alpha = 0.4f) else SalesColors.Border, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = SalesColors.Progress, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontFamily = ZainBold, maxLines = 2)
                Text(subtitle, fontSize = 12.sp, color = SalesColors.Muted)
            }
            IconButton(onClick = onRemove, enabled = enabled) {
                Icon(Icons.Outlined.Delete, contentDescription = "حذف", tint = SalesColors.Danger)
            }
        }
        warning?.let { Text(it, fontSize = 12.sp, color = SalesColors.Danger, fontFamily = ZainRegular) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                secondaryAction?.let { (label, action) ->
                    TextButton(onClick = action, enabled = enabled) { Text(label, fontFamily = ZainBold, color = SalesColors.Progress) }
                }
            }
            QuantityStepper(value = quantity, max = max, enabled = enabled, onValueChange = onQuantityChange)
        }
    }
}

@Composable
private fun PaymentSection(
    state: PresellOrderUiState,
    onPaymentType: (PaymentType) -> Unit,
    onPickDate: () -> Unit,
    onPickInvoice: () -> Unit,
    onRemoveInvoice: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(PaymentType.CASH to "كاش", PaymentType.CREDIT to "آجل", PaymentType.FAWRY to "فوري").forEach { (type, label) ->
                val selected = state.paymentType == type
                Text(
                    label,
                    fontFamily = ZainBold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    color = if (selected) Color.White else SalesColors.Primary,
                    modifier = Modifier
                        .weight(1f)
                        .background(if (selected) SalesColors.Primary else SalesColors.Primary.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                        .clickable(enabled = !state.placing) { onPaymentType(type) }
                        .padding(vertical = 12.dp)
                )
            }
        }
        when (state.paymentType) {
            PaymentType.CREDIT -> Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (state.collectionDate == null) SalesColors.Danger else SalesColors.Border, RoundedCornerShape(10.dp))
                    .clickable(onClick = onPickDate)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = SalesColors.Primary)
                Spacer(Modifier.size(8.dp))
                Text(
                    state.collectionDate?.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ar", "EG"))) ?: "اختر تاريخ التحصيل",
                    fontFamily = ZainBold,
                    color = if (state.collectionDate == null) SalesColors.Muted else Color.Black
                )
            }
            PaymentType.FAWRY -> {
                val invoice = state.fawryInvoice
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (invoice == null) SalesColors.Danger else SalesColors.Border, RoundedCornerShape(10.dp))
                        .clickable(onClick = onPickInvoice)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(if (invoice == null) Icons.Outlined.Upload else Icons.Outlined.Receipt, contentDescription = null, tint = SalesColors.Primary)
                    Spacer(Modifier.size(8.dp))
                    Text(invoice?.fileName ?: "ارفع صورة فاتورة فوري", fontFamily = ZainBold, modifier = Modifier.weight(1f), maxLines = 1)
                    if (invoice != null) {
                        IconButton(onClick = onRemoveInvoice) { Icon(Icons.Outlined.Delete, contentDescription = "حذف", tint = SalesColors.Danger) }
                    }
                }
            }
            PaymentType.CASH -> Unit
        }
    }
}

@Composable
private fun Celebration() {
    LaunchedEffect(Unit) {
        delay(FANFARE_DELAY_MS)
        playVictoryFanfare()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(20.dp))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(SalesColors.Success.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = SalesColors.Success, modifier = Modifier.size(44.dp))
            }
            Text("تم تسجيل الطلب بنجاح!", fontSize = 20.sp, fontFamily = ZainBold)
            HorizontalDivider(color = SalesColors.Border)
            Text("برافو عليك 👏", fontSize = 15.sp, fontFamily = ZainRegular, color = SalesColors.Muted)
        }
    }
}
