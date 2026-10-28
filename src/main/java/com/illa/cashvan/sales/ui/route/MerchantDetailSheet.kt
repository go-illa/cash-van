package com.illa.cashvan.sales.ui.route

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.illa.cashvan.core.location.LocationData
import com.illa.cashvan.core.utils.makePhoneCall
import com.illa.cashvan.sales.domain.model.RouteMerchant
import com.illa.cashvan.sales.domain.model.VISITS_PER_MERCHANT_PER_MONTH
import com.illa.cashvan.sales.domain.route.Coordinates
import com.illa.cashvan.sales.domain.route.buildGoogleMapsDirectionsUrl
import com.illa.cashvan.sales.domain.route.formatDistance
import com.illa.cashvan.sales.domain.route.getDistanceToMerchant
import com.illa.cashvan.sales.feature.visit.data.model.VisitHistoryItem
import com.illa.cashvan.sales.feature.visit.presentation.viewmodel.MerchantDetailViewModel
import com.illa.cashvan.sales.ui.components.SalesColors
import com.illa.cashvan.sales.ui.components.SalesPrimaryButton
import com.illa.cashvan.sales.ui.components.SalesSecondaryButton
import com.illa.cashvan.sales.ui.components.ZainBold
import com.illa.cashvan.sales.ui.components.ZainRegular
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantDetailSheet(
    merchant: RouteMerchant,
    agentLocation: LocationData?,
    isCheckingLocation: Boolean,
    onStartVisit: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: MerchantDetailViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val history by viewModel.history.collectAsStateWithLifecycle()
    LaunchedEffect(merchant.merchantId) { viewModel.load(merchant.merchantId) }

    val distance = agentLocation?.let {
        getDistanceToMerchant(it.latitude, it.longitude, merchant.latitude, merchant.longitude)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SalesColors.Surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column {
                    Text(merchant.displayName, fontSize = 22.sp, fontFamily = ZainBold)
                    if (merchant.displayName != merchant.merchantName) {
                        Text(merchant.merchantName, fontSize = 14.sp, color = SalesColors.Muted, fontFamily = ZainRegular)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Pill(merchant.merchantCode, SalesColors.Muted)
                        Pill(priceTierLabel(merchant.priceTier), SalesColors.Primary)
                        if (merchant.routeName.isNotEmpty()) Pill(merchant.routeName, SalesColors.Muted)
                    }
                }

                InfoBlock(icon = Icons.Outlined.LocationOn) {
                    Text(
                        merchant.detailedAddress ?: merchant.location ?: "موقع التاجر مش متوفر، هتقدر تزوره عادي",
                        fontSize = 14.sp,
                        fontFamily = ZainBold
                    )
                    merchant.governorate?.let { Text(it, fontSize = 12.sp, color = SalesColors.Muted) }
                    distance?.let { Text("${formatDistance(it)} بعيد", fontSize = 12.sp, color = SalesColors.Muted) }
                }

                listOfNotNull(merchant.phoneNumber, merchant.secondaryPhone).forEach { phone ->
                    InfoBlock(icon = Icons.Outlined.Phone, onClick = { makePhoneCall(context, phone) }) {
                        Text(phone, fontSize = 15.sp, fontFamily = ZainBold, color = SalesColors.Primary)
                    }
                }

                merchant.frontdoorCode?.let { code -> FrontdoorCode(code) }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatBox("${merchant.visitCount}/$VISITS_PER_MERCHANT_PER_MONTH", "زيارات الشهر ده", Modifier.weight(1f))
                    StatBox(formatLastVisit(merchant.lastVisitDate) ?: "لم يُزور", "آخر زيارة", Modifier.weight(1f))
                    StatBox(if (merchant.hasOrder) "تم الطلب" else "لم يطلب بعد", "حالة الطلب", Modifier.weight(1f))
                }

                Text("سجل الزيارات", fontSize = 14.sp, fontFamily = ZainBold, color = SalesColors.Muted)
                when {
                    history.isLoading -> CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.CenterHorizontally),
                        color = SalesColors.Primary,
                        strokeWidth = 2.dp
                    )
                    history.failed -> Text("تعذر تحميل سجل الزيارات", fontSize = 13.sp, color = SalesColors.Danger)
                    history.visits.isEmpty() -> Text("مفيش زيارات سابقة", fontSize = 13.sp, color = SalesColors.Muted)
                    else -> history.visits.forEach { VisitHistoryEntry(it) }
                }
                Spacer(Modifier.height(4.dp))
            }

            Spacer(Modifier.height(12.dp))
            SalesPrimaryButton(
                text = if (isCheckingLocation) "جاري إنشاء الزيارة..." else "ابدأ زيارة",
                onClick = onStartVisit,
                loading = isCheckingLocation,
                icon = Icons.Outlined.PlayArrow
            )
            Spacer(Modifier.height(8.dp))
            val directionsUrl = buildGoogleMapsDirectionsUrl(
                destination = Coordinates(merchant.latitude, merchant.longitude),
                origin = agentLocation?.let { Coordinates(it.latitude, it.longitude) }
            )
            SalesSecondaryButton(
                text = "اتجاهات خرائط",
                icon = Icons.Outlined.Directions,
                enabled = directionsUrl != null,
                onClick = {
                    directionsUrl?.let { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
                }
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun InfoBlock(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SalesColors.Background, RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = SalesColors.Primary, modifier = Modifier.size(20.dp))
        Column { content() }
    }
}

@Composable
private fun FrontdoorCode(code: String) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SalesColors.Background, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("كود الباب", fontSize = 12.sp, color = SalesColors.Muted)
            Text(code, fontSize = 18.sp, fontFamily = ZainBold, letterSpacing = 2.sp)
        }
        IconButton(onClick = {
            clipboard.setText(AnnotatedString(code))
            copied = true
        }) {
            Icon(Icons.Outlined.ContentCopy, contentDescription = "نسخ", tint = if (copied) SalesColors.Success else SalesColors.Primary)
        }
    }
}

@Composable
private fun StatBox(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(SalesColors.Background, RoundedCornerShape(10.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 15.sp, fontFamily = ZainBold, maxLines = 1)
        Text(label, fontSize = 11.sp, color = SalesColors.Muted, fontFamily = ZainRegular, maxLines = 1)
    }
}

@Composable
private fun VisitHistoryEntry(visit: VisitHistoryItem) {
    var expanded by remember { mutableStateOf(false) }
    val items = visit.orders.flatMap { it.items }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SalesColors.Background, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(formatDateTime(visit.visitDate), fontSize = 13.sp, fontFamily = ZainBold, modifier = Modifier.weight(1f))
            Pill(
                if (visit.withOrder) "بطلب" else "بدون طلب",
                if (visit.withOrder) SalesColors.Success else SalesColors.Muted
            )
        }
        visit.noOrderReason?.let { Text("السبب: $it", fontSize = 12.sp, color = SalesColors.Muted) }
        visit.orders.forEach { order ->
            Text(
                "${order.totalItems} منتج • ${"%.2f".format(java.util.Locale.US, order.totalValue)} ج.م",
                fontSize = 12.sp,
                color = SalesColors.Muted
            )
        }
        if (items.isNotEmpty()) {
            Text(
                if (expanded) "إخفاء التفاصيل" else "عرض التفاصيل",
                fontSize = 12.sp,
                color = SalesColors.Primary,
                fontFamily = ZainBold,
                modifier = Modifier.clickable { expanded = !expanded }
            )
            if (expanded) {
                HorizontalDivider(color = SalesColors.Border)
                items.forEach { item ->
                    Row {
                        Text(item.productName ?: "منتج غير معروف", fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Text("× ${item.quantity}", fontSize = 12.sp, color = SalesColors.Muted)
                    }
                }
            }
        }
    }
}
