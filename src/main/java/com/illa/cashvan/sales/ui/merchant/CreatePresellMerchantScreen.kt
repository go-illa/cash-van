package com.illa.cashvan.sales.ui.merchant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.illa.cashvan.core.location.LocationPermissionHandler
import com.illa.cashvan.sales.domain.merchant.GOVERNORATES
import com.illa.cashvan.sales.feature.merchant.presentation.viewmodel.CreatePresellMerchantViewModel
import com.illa.cashvan.sales.feature.merchant.presentation.viewmodel.PRICE_TIERS
import com.illa.cashvan.sales.feature.merchant.presentation.viewmodel.WEEKDAYS
import com.illa.cashvan.sales.ui.components.LoadingBox
import com.illa.cashvan.sales.ui.components.SalesColors
import com.illa.cashvan.sales.ui.components.SalesDropdownField
import com.illa.cashvan.sales.ui.components.SalesPrimaryButton
import com.illa.cashvan.sales.ui.components.SalesSecondaryButton
import com.illa.cashvan.sales.ui.components.SalesTextField
import com.illa.cashvan.sales.ui.components.SalesTopBar
import com.illa.cashvan.sales.ui.components.ZainBold
import com.illa.cashvan.sales.ui.components.ZainRegular
import org.koin.androidx.compose.koinViewModel

/**
 * Two-step merchant creation, as the PWA's `CreateMerchant` page: merchant details, then location and
 * visit plan. A tier other than retail is saved as a pending tier request.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreatePresellMerchantScreen(
    onBack: () -> Unit,
    onCreated: (message: String) -> Unit,
    viewModel: CreatePresellMerchantViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val form = state.form

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.messageShown()
        }
    }
    LaunchedEffect(state.createdMessage) {
        state.createdMessage?.let(onCreated)
    }

    LocationPermissionHandler(
        onPermissionGranted = {},
        onPermissionDenied = {}
    ) { requestPermission, isPermissionGranted, _ ->
        LaunchedEffect(state.step, isPermissionGranted) {
            if (state.step == 2 && !isPermissionGranted) requestPermission()
            if (state.step == 2 && isPermissionGranted && form.latitude == null) viewModel.detectLocation()
        }

        Scaffold(
            containerColor = SalesColors.Background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column {
                    SalesTopBar(
                        title = "إضافة تاجر جديد",
                        subtitle = "الخطوة ${state.step} من 2",
                        onBack = { if (state.step == 2) viewModel.previousStep() else onBack() }
                    )
                    LinearProgressIndicator(
                        progress = { state.step / 2f },
                        modifier = Modifier.fillMaxWidth(),
                        color = SalesColors.Primary,
                        trackColor = SalesColors.Border
                    )
                }
            },
            bottomBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.step == 1) {
                        SalesPrimaryButton(text = "التالي", onClick = viewModel::nextStep)
                    } else {
                        SalesSecondaryButton(
                            text = "السابق",
                            onClick = viewModel::previousStep,
                            modifier = Modifier.weight(1f)
                        )
                        SalesPrimaryButton(
                            text = "إضافة التاجر",
                            onClick = viewModel::submit,
                            loading = state.isSubmitting,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        ) { padding ->
            if (state.isLoading) {
                LoadingBox(Modifier.padding(padding))
                return@Scaffold
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (state.step == 1) {
                    SalesTextField(
                        label = "اسم التاجر",
                        value = form.merchantName,
                        onValueChange = { value -> viewModel.updateForm { it.copy(merchantName = value) } },
                        required = true
                    )
                    SalesTextField(
                        label = "اسم اللافتة",
                        value = form.signName,
                        onValueChange = { value -> viewModel.updateForm { it.copy(signName = value) } },
                        required = true
                    )
                    SalesDropdownField(
                        label = "نوع التاجر",
                        options = state.merchantTypes,
                        selected = state.merchantTypes.firstOrNull { it.id == form.merchantTypeId },
                        optionLabel = { it.name },
                        onSelected = { type -> viewModel.updateForm { it.copy(merchantTypeId = type.id) } },
                        required = true
                    )
                    SalesTextField(
                        label = "رقم الهاتف",
                        value = form.phoneNumber,
                        onValueChange = { value -> viewModel.updateForm { it.copy(phoneNumber = value) } },
                        required = true,
                        placeholder = "01xxxxxxxxx",
                        keyboardType = KeyboardType.Phone
                    )
                    SalesTextField(
                        label = "رقم هاتف إضافي",
                        value = form.secondaryPhone,
                        onValueChange = { value -> viewModel.updateForm { it.copy(secondaryPhone = value) } },
                        placeholder = "01xxxxxxxxx",
                        keyboardType = KeyboardType.Phone
                    )
                    SalesDropdownField(
                        label = "فئة السعر",
                        options = PRICE_TIERS,
                        selected = PRICE_TIERS.firstOrNull { it.first == form.priceTier },
                        optionLabel = { it.second },
                        onSelected = { tier -> viewModel.updateForm { it.copy(priceTier = tier.first) } },
                        required = true
                    )
                    if (form.priceTier != "retail") {
                        Text(
                            text = "التاجر هيتسجل بسعر القطاعي لحد ما المشرف يوافق على الفئة المطلوبة.",
                            fontSize = 13.sp,
                            fontFamily = ZainRegular,
                            color = SalesColors.Progress
                        )
                    }
                } else {
                    LocationCard(
                        isLocating = state.isLocating,
                        hasLocation = form.latitude != null,
                        onDetect = {
                            if (isPermissionGranted) viewModel.detectLocation() else requestPermission()
                        }
                    )
                    SalesDropdownField(
                        label = "المحافظة",
                        options = GOVERNORATES,
                        selected = form.governorate.ifEmpty { null },
                        optionLabel = { it },
                        onSelected = viewModel::selectGovernorate,
                        required = true
                    )
                    SalesTextField(
                        label = "العنوان التفصيلي",
                        value = form.detailedAddress,
                        onValueChange = { value -> viewModel.updateForm { it.copy(detailedAddress = value) } },
                        required = true,
                        singleLine = false
                    )
                    if (state.routes.size > 1) {
                        SalesDropdownField(
                            label = "المسار",
                            options = state.routes,
                            selected = state.routes.firstOrNull { it.route_id == form.routeId },
                            optionLabel = { it.routes?.name ?: it.route_id },
                            onSelected = { route -> viewModel.updateForm { it.copy(routeId = route.route_id) } },
                            required = true
                        )
                    }
                    Column {
                        Text(text = "أيام الزيارة *", fontSize = 16.sp, fontFamily = ZainRegular)
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            WEEKDAYS.forEach { (day, label) ->
                                FilterChip(
                                    selected = day in form.visitDays,
                                    onClick = { viewModel.toggleVisitDay(day) },
                                    label = { Text(label, fontFamily = ZainRegular) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SalesColors.Primary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationCard(
    isLocating: Boolean,
    hasLocation: Boolean,
    onDetect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLocating) {
            CircularProgressIndicator(color = SalesColors.Primary, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        } else {
            Icon(
                Icons.Outlined.MyLocation,
                contentDescription = null,
                tint = if (hasLocation) SalesColors.Success else SalesColors.Muted
            )
        }
        Text(
            text = when {
                isLocating -> "جاري تحديد الموقع..."
                hasLocation -> "تم تحديد موقع التاجر"
                else -> "حدد موقع التاجر وانت عنده"
            },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            fontFamily = ZainBold,
            fontSize = 15.sp
        )
        TextButton(onClick = onDetect, enabled = !isLocating) {
            Text(if (hasLocation) "تحديث" else "تحديد", fontFamily = ZainBold, color = SalesColors.Primary)
        }
    }
}
