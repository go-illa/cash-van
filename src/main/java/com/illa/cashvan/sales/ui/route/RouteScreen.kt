package com.illa.cashvan.sales.ui.route

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.illa.cashvan.core.location.LocationPermissionHandler
import com.illa.cashvan.sales.domain.model.PresellOrderContext
import com.illa.cashvan.sales.domain.route.MerchantFilterStatus
import com.illa.cashvan.sales.feature.route.presentation.viewmodel.LocationStatus
import com.illa.cashvan.sales.feature.route.presentation.viewmodel.RouteViewModel
import com.illa.cashvan.sales.feature.visit.presentation.viewmodel.MerchantDetailViewModel
import com.illa.cashvan.sales.feature.visit.presentation.viewmodel.VisitFlowEvent
import com.illa.cashvan.sales.feature.visit.presentation.viewmodel.VisitFlowViewModel
import com.illa.cashvan.sales.ui.components.EmptyState
import com.illa.cashvan.sales.ui.fawry.FawryQuestionnaireDialog
import com.illa.cashvan.sales.ui.components.LoadingBox
import com.illa.cashvan.sales.ui.components.SalesColors
import com.illa.cashvan.sales.ui.components.ZainBold
import com.illa.cashvan.sales.ui.components.ZainRegular
import com.illa.cashvan.sales.ui.components.salesFieldColors
import org.koin.androidx.compose.koinViewModel

private val FILTERS = listOf(
    MerchantFilterStatus.ALL to "الكل",
    MerchantFilterStatus.UNVISITED to "غير مزار",
    MerchantFilterStatus.VISITED to "تمت اول زيارة",
    MerchantFilterStatus.COMPLETED to "تم زيارته مرتين"
)

/**
 * The presell home: the agent's monthly journey plan, sorted by distance once GPS is available.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteScreen(
    onOpenOrder: (PresellOrderContext) -> Unit,
    onCreateMerchant: () -> Unit,
    onAdHocOrder: () -> Unit,
    onNearbyMerchants: () -> Unit,
    routeViewModel: RouteViewModel = koinViewModel(),
    visitFlowViewModel: VisitFlowViewModel = koinViewModel(),
    merchantDetailViewModel: MerchantDetailViewModel = koinViewModel()
) {
    val state by routeViewModel.uiState.collectAsStateWithLifecycle()
    val visitState by visitFlowViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedMerchantId by rememberSaveable { mutableStateOf<String?>(null) }
    var searchText by rememberSaveable { mutableStateOf(state.searchQuery) }
    val selectedMerchant = state.merchants.firstOrNull { it.merchantId == selectedMerchantId }

    LaunchedEffect(Unit) {
        visitFlowViewModel.events.collect { event ->
            when (event) {
                is VisitFlowEvent.OpenOrder -> {
                    selectedMerchantId = null
                    onOpenOrder(event.context)
                }
                VisitFlowEvent.VisitLogged -> {
                    selectedMerchantId = null
                    merchantDetailViewModel.invalidate()
                    routeViewModel.refresh()
                }
            }
        }
    }
    LaunchedEffect(visitState.message) {
        visitState.message?.let { message ->
            snackbarHostState.showSnackbar(listOfNotNull(message.title, message.description).joinToString("\n"))
            visitFlowViewModel.messageShown()
        }
    }
    DisposableEffect(Unit) {
        onDispose { routeViewModel.stopLocationUpdates() }
    }

    LocationPermissionHandler(
        onPermissionGranted = routeViewModel::startLocationUpdates,
        onPermissionDenied = routeViewModel::onLocationUnavailable
    ) { requestPermission, isPermissionGranted, _ ->
        LaunchedEffect(isPermissionGranted) {
            if (!isPermissionGranted) requestPermission()
        }

        Scaffold(
            containerColor = SalesColors.Background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = onAdHocOrder,
                    containerColor = SalesColors.Primary,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Outlined.ShoppingCart, contentDescription = null) },
                    text = { Text("طلب خارج المسار", fontFamily = ZainBold) }
                )
            },
            topBar = {
                RouteHeader(agentCode = state.agentCode, onCreateMerchant = onCreateMerchant)
            }
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = routeViewModel::refresh,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when {
                    state.isLoading -> LoadingBox()
                    state.error != null && state.merchants.isEmpty() -> EmptyState(
                        icon = Icons.Outlined.Refresh,
                        title = state.error.orEmpty(),
                        action = { TextButton(onClick = { routeViewModel.load() }) { Text("حاول تاني", fontFamily = ZainBold) } }
                    )
                    state.merchants.isEmpty() -> EmptyState(
                        icon = Icons.Outlined.LocationOn,
                        title = "لم يتم تعيين مسار لك لسه",
                        description = "تواصل مع المشرف بتاعك عشان يخصص لك خط سير",
                        action = { TextButton(onClick = onCreateMerchant) { Text("إضافة تاجر", fontFamily = ZainBold) } }
                    )
                    else -> LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            PlanTitle(
                                merchantCount = state.merchants.size,
                                routeNames = state.routeNames,
                                locationStatus = state.locationStatus,
                                onRetryLocation = {
                                    if (isPermissionGranted) routeViewModel.startLocationUpdates() else requestPermission()
                                }
                            )
                        }
                        item {
                            SearchAndFilters(
                                query = searchText,
                                onQueryChange = {
                                    searchText = it
                                    routeViewModel.onSearchChange(it)
                                },
                                filter = state.filter,
                                onFilterChange = routeViewModel::onFilterChange
                            )
                        }
                        if (state.isFiltering && state.filteredCount > 0) {
                            item {
                                val total = if (state.filteredCount != state.merchants.size) " · ${state.merchants.size} المجموع" else ""
                                Text(
                                    "${state.filteredCount} تجار$total",
                                    fontSize = 12.sp,
                                    color = SalesColors.Muted,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                        if (state.isFiltering && state.filteredCount == 0) {
                            item {
                                EmptyState(
                                    icon = Icons.Outlined.Search,
                                    title = "مفيش نتائج مطابقة",
                                    action = {
                                        TextButton(onClick = {
                                            searchText = ""
                                            routeViewModel.clearFilters()
                                        }) { Text("مسح الفلاتر", fontFamily = ZainBold) }
                                        TextButton(onClick = onNearbyMerchants) { Text("فيه تاجر قريب بره مسارك؟ دوّر عليه", fontFamily = ZainBold) }
                                    }
                                )
                            }
                        }
                        state.groups.forEach { group ->
                            item(key = "header-${group.routeName}") {
                                GroupHeader(name = group.routeName ?: "مرتبة بالمسافة", count = group.items.size, sortedByDistance = group.routeName == null)
                            }
                            items(group.items, key = { it.merchant.merchantId }) { item ->
                                RouteMerchantCard(
                                    merchant = item.merchant,
                                    distanceMeters = item.distanceMeters,
                                    remoteOrder = state.remoteOrders[item.merchant.merchantId],
                                    isCheckingLocation = visitState.checkingMerchantId == item.merchant.merchantId,
                                    onClick = {
                                        selectedMerchantId = item.merchant.merchantId
                                        visitFlowViewModel.onMerchantOpened(item.merchant)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (selectedMerchant != null) {
            MerchantDetailSheet(
                merchant = selectedMerchant,
                agentLocation = state.agentLocation,
                isCheckingLocation = visitState.checkingMerchantId == selectedMerchant.merchantId,
                onStartVisit = { visitFlowViewModel.startVisit(selectedMerchant, state.merchants) },
                onDismiss = { selectedMerchantId = null },
                viewModel = merchantDetailViewModel
            )
        }
        val fawryState by visitFlowViewModel.fawryGate.state.collectAsStateWithLifecycle()
        FawryQuestionnaireDialog(state = fawryState, gate = visitFlowViewModel.fawryGate)
        VisitFlowSheets(
            sheet = visitState.sheet,
            isCheckingLocation = visitState.checkingMerchantId != null,
            onConfirmRevisit = visitFlowViewModel::confirmRevisit,
            onWithOrder = visitFlowViewModel::selectWithOrder,
            onWithoutOrder = visitFlowViewModel::selectWithoutOrder,
            onReasonSelected = visitFlowViewModel::submitNoOrder,
            onDismiss = visitFlowViewModel::dismiss
        )
    }
}

@Composable
private fun RouteHeader(agentCode: String?, onCreateMerchant: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .background(SalesColors.Background, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Outlined.Person, contentDescription = null, tint = SalesColors.Muted, modifier = Modifier.size(16.dp))
            Text(agentCode ?: "—", fontSize = 14.sp, fontFamily = ZainBold)
        }
        Box(modifier = Modifier.weight(1f))
        TextButton(onClick = onCreateMerchant) {
            Icon(Icons.Default.Add, contentDescription = null, tint = SalesColors.Primary, modifier = Modifier.size(18.dp))
            Text("إنشاء تاجر", fontFamily = ZainBold, color = SalesColors.Primary)
        }
    }
}

@Composable
private fun PlanTitle(
    merchantCount: Int,
    routeNames: List<String>,
    locationStatus: LocationStatus,
    onRetryLocation: () -> Unit
) {
    Row(verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Text("خطة الزيارات الشهرية ($merchantCount)", fontSize = 22.sp, fontFamily = ZainBold)
            if (routeNames.isNotEmpty()) {
                Text(routeNames.joinToString("، "), fontSize = 14.sp, color = SalesColors.Muted, fontFamily = ZainRegular)
            }
        }
        val (text, color) = when (locationStatus) {
            LocationStatus.READY -> "مرتبة بالمسافة" to SalesColors.Success
            LocationStatus.FAILED -> "فشل تحديد الموقع" to SalesColors.Danger
            LocationStatus.DETECTING -> "جاري تحديد مكانك..." to SalesColors.Muted
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill(text, color)
            if (locationStatus == LocationStatus.FAILED) {
                IconButton(onClick = onRetryLocation) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "حاول تاني", tint = SalesColors.Danger)
                }
            }
        }
    }
}

@Composable
private fun SearchAndFilters(
    query: String,
    onQueryChange: (String) -> Unit,
    filter: MerchantFilterStatus,
    onFilterChange: (MerchantFilterStatus) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("ابحث في التجار...", color = Color(0xFFBDBDBD)) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = SalesColors.Muted) },
            trailingIcon = if (query.isNotEmpty()) {
                { IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Outlined.Clear, contentDescription = "مسح") } }
            } else null,
            shape = RoundedCornerShape(10.dp),
            colors = salesFieldColors()
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FILTERS.forEach { (status, label) ->
                FilterChip(
                    selected = filter == status,
                    onClick = { onFilterChange(status) },
                    label = { Text(label, fontFamily = ZainRegular, fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SalesColors.Primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
    }
}

@Composable
private fun GroupHeader(name: String, count: Int, sortedByDistance: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SalesColors.Border.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            if (sortedByDistance) Icons.Outlined.Navigation else Icons.Outlined.LocationOn,
            contentDescription = null,
            tint = SalesColors.Muted,
            modifier = Modifier.size(16.dp)
        )
        Text(name, fontSize = 14.sp, fontFamily = ZainBold, color = SalesColors.Muted)
        Text("($count)", fontSize = 12.sp, color = SalesColors.Muted)
    }
}
