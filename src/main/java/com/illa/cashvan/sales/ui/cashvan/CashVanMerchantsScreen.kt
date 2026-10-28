package com.illa.cashvan.sales.ui.cashvan

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.illa.cashvan.sales.domain.model.VISITS_PER_MERCHANT_PER_MONTH
import com.illa.cashvan.sales.domain.route.buildGoogleMapsSearchUrl
import com.illa.cashvan.sales.domain.route.formatDistance
import com.illa.cashvan.sales.feature.cashvan.data.model.CashVanMerchantContext
import com.illa.cashvan.sales.feature.cashvan.presentation.viewmodel.CashVanMerchantItem
import com.illa.cashvan.sales.feature.cashvan.presentation.viewmodel.CashVanMerchantsEvent
import com.illa.cashvan.sales.feature.cashvan.presentation.viewmodel.CashVanMerchantsViewModel
import com.illa.cashvan.sales.feature.cashvan.presentation.viewmodel.CashVanVisitSheet
import com.illa.cashvan.sales.feature.cashvan.presentation.viewmodel.MerchantListMode
import com.illa.cashvan.sales.ui.components.EmptyState
import com.illa.cashvan.sales.ui.components.LoadingBox
import com.illa.cashvan.sales.ui.components.SalesColors
import com.illa.cashvan.sales.ui.components.SalesPrimaryButton
import com.illa.cashvan.sales.ui.components.SalesTopBar
import com.illa.cashvan.sales.ui.components.ZainBold
import com.illa.cashvan.sales.ui.components.ZainRegular
import com.illa.cashvan.sales.ui.components.salesFieldColors
import com.illa.cashvan.sales.ui.fawry.FawryQuestionnaireDialog
import com.illa.cashvan.sales.ui.route.FawryBadge
import com.illa.cashvan.sales.ui.route.Pill
import org.koin.androidx.compose.koinViewModel

@Composable
fun CashVanMerchantsScreen(
    onBack: () -> Unit,
    onStartOrder: (CashVanMerchantContext) -> Unit,
    onCreateMerchant: () -> Unit,
    onVisitLogged: () -> Unit,
    onNearbyMerchants: () -> Unit,
    viewModel: CashVanMerchantsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val fawryState by viewModel.fawryGate.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CashVanMerchantsEvent.StartOrder -> onStartOrder(event.context)
                CashVanMerchantsEvent.VisitLogged -> onVisitLogged()
            }
        }
    }
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
            Column(modifier = Modifier.background(Color.White)) {
                SalesTopBar(
                    title = "اختيار تاجر",
                    onBack = onBack,
                    actions = {
                        TextButton(onClick = onCreateMerchant) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = SalesColors.Primary, modifier = Modifier.size(18.dp))
                            Text("إضافة تاجر", fontFamily = ZainBold, color = SalesColors.Primary)
                        }
                    }
                )
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    singleLine = true,
                    placeholder = { Text("ابحث باسم التاجر أو الكود...", color = Color(0xFFBDBDBD)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = SalesColors.Muted) },
                    shape = RoundedCornerShape(10.dp),
                    colors = salesFieldColors()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(MerchantListMode.NEARBY to "القريبون مني", MerchantListMode.ALL to "كل تجار المسار").forEach { (mode, label) ->
                        val selected = state.mode == mode
                        Text(
                            label,
                            fontFamily = ZainBold,
                            textAlign = TextAlign.Center,
                            color = if (selected) Color.White else SalesColors.Primary,
                            modifier = Modifier
                                .weight(1f)
                                .background(if (selected) SalesColors.Primary else SalesColors.Primary.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                .clickable { viewModel.setMode(mode) }
                                .padding(vertical = 10.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            val items = state.items
            when {
                state.isLoading -> LoadingBox()
                state.error != null && items.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.Storefront,
                    title = state.error.orEmpty(),
                    action = { TextButton(onClick = viewModel::load) { Text("إعادة المحاولة", fontFamily = ZainBold) } }
                )
                items.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.Storefront,
                    title = "لم يتم العثور على تجار",
                    description = when {
                        state.query.isNotBlank() -> "جرب البحث بكلمة أخرى"
                        state.mode == MerchantListMode.NEARBY -> "لا يوجد تجار في نطاق 1 كم"
                        else -> "لا يوجد تجار مسندون لك"
                    },
                    action = { TextButton(onClick = onNearbyMerchants) { Text("فيه تاجر قريب بره مسارك؟ دوّر عليه", fontFamily = ZainBold) } }
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item(key = "nearby-cta") {
                        TextButton(onClick = onNearbyMerchants, modifier = Modifier.fillMaxWidth()) {
                            Text("فيه تاجر قريب بره مسارك؟ دوّر عليه", fontFamily = ZainBold)
                        }
                    }
                    items(items, key = { it.id }) { merchant ->
                        MerchantRow(
                            merchant = merchant,
                            checking = state.checkingMerchantId == merchant.id,
                            onClick = { viewModel.selectMerchant(merchant) },
                            onMap = {
                                if (merchant.latitude != null && merchant.longitude != null) {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(buildGoogleMapsSearchUrl(merchant.latitude, merchant.longitude))))
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    FawryQuestionnaireDialog(state = fawryState, gate = viewModel.fawryGate)
    CashVanVisitSheets(
        sheet = state.sheet,
        onCreateOrder = viewModel::createOrder,
        onVisitOnly = viewModel::visitOnly,
        onConfirmReason = viewModel::confirmVisit,
        onDismiss = viewModel::dismissSheet
    )
}

@Composable
private fun MerchantRow(
    merchant: CashVanMerchantItem,
    checking: Boolean,
    onClick: () -> Unit,
    onMap: () -> Unit
) {
    val visits = merchant.visitCount
    val progressColor = when {
        visits == null || visits == 0 -> SalesColors.Muted
        merchant.targetCompleted -> SalesColors.Success
        else -> SalesColors.Progress
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(10.dp))
            .border(1.dp, if (merchant.targetCompleted) SalesColors.Success.copy(alpha = 0.3f) else SalesColors.Border, RoundedCornerShape(10.dp))
            .clickable(enabled = !checking, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(merchant.name, fontSize = 15.sp, fontFamily = ZainBold, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                FawryBadge(merchant.fawryDeviceNumber, merchant.wantsFawryDevice)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                merchant.code?.let { Pill(it, SalesColors.Muted) }
                if (visits != null) Pill("$visits/$VISITS_PER_MERCHANT_PER_MONTH زيارات", progressColor)
                merchant.distanceMeters?.let { Pill(formatDistance(it), SalesColors.Primary) }
            }
            listOfNotNull(merchant.phone, merchant.address).forEach {
                Text(it, fontSize = 12.sp, color = SalesColors.Muted, fontFamily = ZainRegular, maxLines = 1)
            }
        }
        if (checking) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = SalesColors.Primary)
        } else {
            IconButton(onClick = onMap, enabled = merchant.latitude != null) {
                Icon(Icons.Outlined.Map, contentDescription = "الخريطة", tint = SalesColors.Primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CashVanVisitSheets(
    sheet: CashVanVisitSheet,
    onCreateOrder: () -> Unit,
    onVisitOnly: () -> Unit,
    onConfirmReason: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (sheet == CashVanVisitSheet.None) return
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (sheet) {
                is CashVanVisitSheet.VisitType -> {
                    Text(sheet.merchant.name, fontSize = 22.sp, fontFamily = ZainBold)
                    Text("اختر نوع العملية", fontSize = 14.sp, color = SalesColors.Muted, fontFamily = ZainRegular)
                    SalesPrimaryButton(text = "إنشاء طلب", onClick = onCreateOrder, icon = Icons.Outlined.ShoppingCart)
                    SalesPrimaryButton(
                        text = "زيارة فقط",
                        onClick = onVisitOnly,
                        icon = Icons.Outlined.Assignment,
                        containerColor = SalesColors.Muted
                    )
                }
                is CashVanVisitSheet.Reasons -> {
                    var selectedId by remember { mutableStateOf<String?>(null) }
                    Text("سبب عدم الطلب", fontSize = 22.sp, fontFamily = ZainBold)
                    Text("اختر سبب عدم تسجيل طلب", fontSize = 14.sp, color = SalesColors.Muted, fontFamily = ZainRegular)
                    if (sheet.isLoading) {
                        Text("جاري تحميل الأسباب...", color = SalesColors.Muted)
                    } else {
                        Column(
                            modifier = Modifier
                                .heightIn(max = 360.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            sheet.reasons.forEach { reason ->
                                val selected = selectedId == reason.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(if (selected) 2.dp else 1.dp, if (selected) SalesColors.Primary else SalesColors.Border, RoundedCornerShape(10.dp))
                                        .clickable { selectedId = reason.id }
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(reason.reason_ar, fontFamily = ZainBold, modifier = Modifier.weight(1f))
                                    if (selected) Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = SalesColors.Primary)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    SalesPrimaryButton(
                        text = if (sheet.isSubmitting) "جاري التأكيد..." else "تأكيد الزيارة",
                        onClick = { selectedId?.let(onConfirmReason) },
                        enabled = selectedId != null,
                        loading = sheet.isSubmitting
                    )
                }
                CashVanVisitSheet.None -> Unit
            }
        }
    }
}
