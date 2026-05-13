package com.illa.cashvan.ui.visit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.illa.cashvan.R
import com.illa.cashvan.core.location.LocationPermissionHandler
import com.illa.cashvan.feature.visit.presentation.viewmodel.CreateVisitViewModel
import com.illa.cashvan.ui.common.ErrorSnackbar
import com.illa.cashvan.ui.common.ProximityBannerVariant
import com.illa.cashvan.ui.common.ProximityInfoBanner
import com.illa.cashvan.ui.orders.ui_components.SearchableDropdown
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateVisitWithoutOrderScreen(
    visitViewModel: CreateVisitViewModel = koinViewModel(),
    onBackClick: () -> Unit = {},
    onVisitCreated: () -> Unit = {}
) {
    val uiState by visitViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var visitSubmitting by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.visitCreated) {
        if (uiState.visitCreated) {
            visitSubmitting = false
            onVisitCreated()
            visitViewModel.resetVisitCreated()
        }
    }

    LaunchedEffect(uiState.visitCreationError) {
        uiState.visitCreationError?.let {
            snackbarHostState.showSnackbar(it)
            visitViewModel.clearVisitCreationError()
            visitSubmitting = false
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            visitViewModel.clearError()
        }
    }

    LocationPermissionHandler(
        onPermissionGranted = { visitViewModel.onLocationPermissionGranted() },
        onPermissionDenied = { visitViewModel.onLocationPermissionDenied() }
    ) { requestPermission, isPermissionGranted, _ ->
        val context = LocalContext.current
        var showLocationDialog by remember { mutableStateOf(false) }
        var navigatedToSettings by rememberSaveable { mutableStateOf(false) }

        LaunchedEffect(isPermissionGranted) {
            if (!isPermissionGranted && !navigatedToSettings) showLocationDialog = true
        }

        if (showLocationDialog && !uiState.locationGranted) {
            AlertDialog(
                onDismissRequest = { showLocationDialog = false },
                title = {
                    Text(
                        text = "تحديد الموقع مطلوب",
                        fontFamily = FontFamily(Font(R.font.zain_regular)),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = if (!isPermissionGranted)
                            "يجب السماح بالوصول للموقع لعرض التجار القريبين. يرجى تفعيل صلاحية الموقع."
                        else
                            "تعذر تحديد موقعك. تأكد من تفعيل خدمة الموقع في جهازك وحاول مرة أخرى.",
                        fontFamily = FontFamily(Font(R.font.zain_regular))
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (!isPermissionGranted) requestPermission()
                            else visitViewModel.onLocationPermissionGranted()
                            showLocationDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D3773))
                    ) {
                        Text(
                            text = if (!isPermissionGranted) "السماح" else "إعادة المحاولة",
                            fontFamily = FontFamily(Font(R.font.zain_regular)),
                            color = Color.White
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            navigatedToSettings = true
                            val intent = if (isPermissionGranted) {
                                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                            } else {
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                            }
                            context.startActivity(intent)
                            showLocationDialog = false
                        }
                    ) {
                        Text(
                            text = "فتح الإعدادات",
                            fontFamily = FontFamily(Font(R.font.zain_regular)),
                            color = Color(0xFF0D3773)
                        )
                    }
                }
            )
        }

        Scaffold(
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    ErrorSnackbar(
                        message = data.visuals.message,
                        onDismiss = { data.dismiss() },
                        modifier = Modifier.padding(16.dp)
                    )
                }
            },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "إنشاء زيارة بدون طلب",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily(Font(R.font.zain_regular)),
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0D3773)
                    )
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            visitSubmitting = true
                            visitViewModel.createVisit()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0D3773),
                            disabledContainerColor = Color(0xFFE0E0E0),
                            disabledContentColor = Color(0xFF9E9E9E)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !visitSubmitting &&
                                uiState.selectedMerchant != null &&
                                uiState.selectedReason != null &&
                                !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "تأكيد الزيارة",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily(Font(R.font.zain_regular)),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8F9FA))
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "اختيار التاجر *",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily(Font(R.font.zain_regular)),
                        color = Color(0xFF1F252E)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    when {
                        uiState.isGettingLocation -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF0D3773),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        !uiState.locationGranted -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "يرجى السماح بالوصول إلى الموقع لعرض التجار القريبين",
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily(Font(R.font.zain_regular)),
                                    color = Color(0xFF9E9E9E),
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = {
                                        navigatedToSettings = false
                                        showLocationDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D3773)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "تفعيل الموقع",
                                        fontFamily = FontFamily(Font(R.font.zain_regular)),
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                        else -> {
                            SearchableDropdown(
                                label = "",
                                placeholder = "أبحث بالاسم أو رقم التليفون",
                                searchQuery = uiState.merchantSearchQuery,
                                onSearchQueryChange = { visitViewModel.searchMerchants(it) },
                                items = uiState.merchants,
                                selectedItem = uiState.selectedMerchant,
                                onItemSelected = { visitViewModel.selectMerchant(it) },
                                itemText = { it.displayName },
                                isLoading = uiState.isSearchingMerchants,
                                onExpanded = {
                                    if (uiState.merchantSearchQuery.isEmpty()) {
                                        visitViewModel.searchMerchants("")
                                    }
                                },
                                onClear = { visitViewModel.clearMerchant() },
                                onLoadMore = { visitViewModel.loadMoreMerchants() },
                                isLoadingMore = uiState.isLoadingMoreMerchants
                            )
                            val showEmptyResults = uiState.merchantSearchQuery.isNotEmpty()
                                    && uiState.merchants.isEmpty()
                                    && !uiState.isSearchingMerchants
                            Spacer(modifier = Modifier.height(8.dp))
                            if (showEmptyResults) {
                                ProximityInfoBanner(variant = ProximityBannerVariant.EMPTY_RESULTS)
                            } else if (uiState.selectedMerchant == null) {
                                ProximityInfoBanner(variant = ProximityBannerVariant.INITIAL_HINT)
                            }
                        }
                    }
                }

                if (uiState.selectedMerchant != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "سبب عدم الطلب *",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily(Font(R.font.zain_regular)),
                            color = Color(0xFF1F252E)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        when {
                            uiState.isLoadingReasons -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF0D3773),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            uiState.noOrderReasons.isEmpty() -> {
                                Text(
                                    text = "لا توجد أسباب متاحة",
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily(Font(R.font.zain_regular)),
                                    color = Color(0xFF9E9E9E),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            else -> {
                                uiState.noOrderReasons.forEachIndexed { index, reason ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { visitViewModel.selectReason(reason) }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = reason.reason_ar,
                                            fontSize = 15.sp,
                                            fontFamily = FontFamily(Font(R.font.zain_regular)),
                                            color = Color(0xFF1F252E),
                                            modifier = Modifier.weight(1f)
                                        )
                                        RadioButton(
                                            selected = uiState.selectedReason?.id == reason.id,
                                            onClick = { visitViewModel.selectReason(reason) },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = Color(0xFF0D3773),
                                                unselectedColor = Color(0xFFBDBDBD)
                                            )
                                        )
                                    }
                                    if (index < uiState.noOrderReasons.size - 1) {
                                        Divider(color = Color(0xFFF0F0F0))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
