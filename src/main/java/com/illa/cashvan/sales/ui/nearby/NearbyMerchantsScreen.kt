package com.illa.cashvan.sales.ui.nearby

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.illa.cashvan.sales.domain.nearby.MerchantClassification
import com.illa.cashvan.sales.domain.nearby.NearbyMerchant
import com.illa.cashvan.sales.domain.route.formatDistance
import com.illa.cashvan.sales.feature.nearby.presentation.viewmodel.NearbyDialogKind
import com.illa.cashvan.sales.feature.nearby.presentation.viewmodel.NearbyEvent
import com.illa.cashvan.sales.feature.nearby.presentation.viewmodel.NearbyMerchantsViewModel
import com.illa.cashvan.sales.ui.components.EmptyState
import com.illa.cashvan.sales.ui.components.LoadingBox
import com.illa.cashvan.sales.ui.components.SalesColors
import com.illa.cashvan.sales.ui.components.SalesDropdownField
import com.illa.cashvan.sales.ui.components.SalesTopBar
import com.illa.cashvan.sales.ui.components.ZainBold
import com.illa.cashvan.sales.ui.components.ZainRegular
import com.illa.cashvan.sales.ui.route.Pill
import com.illa.cashvan.sales.ui.route.formatDateTime
import org.koin.androidx.compose.koinViewModel

private val WHATSAPP_GREEN = Color(0xFF25D366)
private val WHATSAPP_TEXT = Color(0xFF063D1B)

/** "تجار قريبون": merchants within 1 km that aren't on the agent's routes. */
@Composable
fun NearbyMerchantsScreen(
    flow: String,
    onBack: () -> Unit,
    viewModel: NearbyMerchantsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val host = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(flow) { viewModel.flow = flow }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is NearbyEvent.OpenWhatsApp -> try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(event.link)))
                } catch (e: ActivityNotFoundException) {
                    host.showSnackbar("واتساب غير مثبت على الجهاز")
                }
            }
        }
    }
    LaunchedEffect(state.message) {
        state.message?.let {
            host.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Scaffold(
        containerColor = SalesColors.Background,
        snackbarHost = { SnackbarHost(host) },
        topBar = {
            SalesTopBar(title = "تجار قريبون", onBack = onBack, actions = {
                IconButton(onClick = viewModel::openHistory) { Icon(Icons.Outlined.History, contentDescription = "طلباتي", tint = SalesColors.Primary) }
            })
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                state.isLoading -> LoadingBox()
                state.locationFailed -> EmptyState(
                    icon = Icons.Outlined.LocationOn,
                    title = "تعذّر تحديد موقعك. فعّل إذن الموقع.",
                    action = { TextButton(onClick = viewModel::retryLocation) { Text("أعد المحاولة", fontFamily = ZainBold) } }
                )
                state.loadFailed -> EmptyState(icon = Icons.Outlined.LocationOn, title = "تعذّر تحميل التجار القريبين. حاول تاني.")
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Text(
                            "دول تجار قريبين منك دلوقتي بره مسارك — لقيناهم بالـ GPS. ضيف اللي مش مُكلَّف بضغطة واحدة، أو اطللب نقل اللي على مسار تاني.",
                            fontFamily = ZainRegular,
                            color = SalesColors.Muted,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        )
                    }
                    if (state.merchants.isEmpty()) {
                        item { EmptyState(icon = Icons.Outlined.LocationOn, title = "مفيش تجار بره مسارك في نطاق 1 كم.") }
                    }
                    items(state.merchants, key = { it.id }) { merchant ->
                        NearbyCard(
                            merchant = merchant,
                            whatsAppReady = state.whatsAppNumber != null,
                            onAdd = { viewModel.openAdd(merchant) },
                            onTransfer = { viewModel.openTransfer(merchant) }
                        )
                    }
                }
            }
        }
    }

    state.dialog?.let { dialog ->
        val merchantName = dialog.merchant.signName?.takeIf { it.isNotBlank() } ?: dialog.merchant.name
        AlertDialog(
            onDismissRequest = { if (!dialog.submitting) viewModel.dismissDialog() },
            title = { Text(if (dialog.kind == NearbyDialogKind.ADD) "إضافة لمسارك" else "إرسال طلب النقل؟", fontFamily = ZainBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (dialog.kind == NearbyDialogKind.ADD) {
                        Text("$merchantName · ${formatDistance(dialog.merchant.distance)}", fontFamily = ZainRegular)
                    } else {
                        Text("راجع الطلب قبل الإرسال.", fontFamily = ZainRegular, color = SalesColors.Muted)
                        Text("$merchantName — ${dialog.merchant.routeNames.joinToString("، ").ifEmpty { "مسار غير معروف" }}", fontFamily = ZainBold)
                    }
                    if (state.myRoutes.isEmpty()) {
                        Text("مفيش مسار مُكلَّف ليك. كلّم المسؤول.", color = SalesColors.Danger)
                    } else {
                        SalesDropdownField(
                            label = "اختر المسار",
                            options = state.myRoutes,
                            selected = state.myRoutes.firstOrNull { it.id == dialog.routeId },
                            optionLabel = { it.name ?: it.id },
                            onSelected = { viewModel.selectRoute(it.id) },
                            placeholder = "اختار المسار"
                        )
                        if (state.myRoutes.size > 1) Text("تم اختيار أقرب مسار مسبقًا — تقدر تغيّره.", fontSize = 12.sp, color = SalesColors.Muted)
                    }
                    if (dialog.kind == NearbyDialogKind.TRANSFER) {
                        Text(
                            "التأكيد هيسجّل الطلب للعمليات للموافقة عليه، وبعدها هيفتح واتساب بالرسالة جاهزة. واتساب بيفتح بعد ما الطلب يتسجّل.",
                            fontSize = 12.sp,
                            color = SalesColors.Progress,
                            fontFamily = ZainRegular
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDialog, enabled = dialog.routeId != null && !dialog.submitting) {
                    Text(if (dialog.kind == NearbyDialogKind.ADD) "ضيف لمساري" else "تأكيد وفتح واتساب", fontFamily = ZainBold)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDialog, enabled = !dialog.submitting) { Text("إلغاء", fontFamily = ZainBold) }
            }
        )
    }

    state.history?.let { history ->
        AlertDialog(
            onDismissRequest = viewModel::closeHistory,
            title = { Text("طلبات النقل بتاعتي", fontFamily = ZainBold) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("حالة وأسباب رفض طلباتك السابقة.", color = SalesColors.Muted, fontSize = 12.sp)
                    if (history.isEmpty()) Text("مفيش طلبات لسه.", fontFamily = ZainRegular)
                    history.forEach { request ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SalesColors.Background, RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Row {
                                Text(request.merchant_name ?: request.merchant_code.orEmpty(), fontFamily = ZainBold, modifier = Modifier.weight(1f))
                                when (request.status) {
                                    "approved" -> Pill("تمت الموافقة", SalesColors.Success)
                                    "declined" -> Pill("مرفوض", SalesColors.Danger)
                                    else -> Pill("قيد المراجعة", SalesColors.Progress)
                                }
                            }
                            Text("${request.from_route_name ?: "—"} ← ${request.to_route_name ?: "—"}", fontSize = 12.sp, color = SalesColors.Muted)
                            if (request.status == "declined" && !request.decline_note.isNullOrBlank()) {
                                Text("سبب الرفض: ${request.decline_note}", fontSize = 12.sp, color = SalesColors.Danger)
                            }
                            Text(formatDateTime(request.requested_at), fontSize = 11.sp, color = SalesColors.Muted)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = viewModel::closeHistory) { Text("إغلاق", fontFamily = ZainBold) } }
        )
    }
}

@Composable
private fun NearbyCard(
    merchant: NearbyMerchant,
    whatsAppReady: Boolean,
    onAdd: () -> Unit,
    onTransfer: () -> Unit
) {
    val otherRoute = merchant.classification == MerchantClassification.OTHER_ROUTE
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row {
            Text(merchant.signName?.takeIf { it.isNotBlank() } ?: merchant.name, fontFamily = ZainBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            if (otherRoute) Pill("مسار تاني", SalesColors.Progress) else Pill("غير مُكلَّف", SalesColors.Muted)
        }
        Text(
            listOfNotNull(formatDistance(merchant.distance), merchant.location, merchant.routeNames.joinToString("، ").takeIf { otherRoute && it.isNotEmpty() })
                .joinToString(" · "),
            fontSize = 12.sp,
            color = SalesColors.Muted
        )
        if (merchant.lastDeclined && !merchant.lastDeclineNote.isNullOrBlank()) {
            Text("سبب الرفض: ${merchant.lastDeclineNote}", color = SalesColors.Danger, fontSize = 12.sp)
        }
        if (!otherRoute) {
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                Text("+ ضيف لمساري", fontFamily = ZainBold)
            }
        } else {
            Text("التاجر ده مُكلَّف لمسار تاني. ابعت طلب نقل.", fontSize = 12.sp, fontFamily = ZainRegular)
            val pending = merchant.pendingRequestId != null
            Button(
                onClick = onTransfer,
                enabled = whatsAppReady && !pending && !merchant.lastDeclined,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WHATSAPP_GREEN, contentColor = WHATSAPP_TEXT)
            ) {
                Text(
                    when {
                        pending -> "الطلب قيد المراجعة"
                        merchant.lastDeclined -> "تم رفض الطلب"
                        else -> "ابعت طلب النقل عبر واتساب"
                    },
                    fontFamily = ZainBold
                )
            }
            when {
                pending -> Text("فيه طلب نقل قائم لهذا التاجر بالفعل.", fontSize = 11.sp, color = SalesColors.Muted)
                merchant.lastDeclined -> Text("اتُرفض طلب نقل هذا التاجر. كلّم الإدارة لو محتاج إعادة.", fontSize = 11.sp, color = SalesColors.Muted)
            }
        }
    }
}
