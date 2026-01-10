package com.illa.cashvan.ui.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.illa.cashvan.R
import com.illa.cashvan.feature.orders.presentation.mapper.toOrderItem
import com.illa.cashvan.feature.orders.presentation.viewmodel.OrderType
import com.illa.cashvan.feature.orders.presentation.viewmodel.OrderViewModel
import com.illa.cashvan.ui.common.CashVanHeader
import com.illa.cashvan.ui.common.ErrorSnackbar
import com.illa.cashvan.core.analytics.CashVanAnalyticsHelper
import com.illa.cashvan.ui.home.ui_components.EmptyOrdersComponent
import com.illa.cashvan.ui.orders.ui_components.CancelOrderBottomSheet
import com.illa.cashvan.ui.orders.ui_components.OrderCardItem
import com.illa.cashvan.ui.orders.ui_components.OrderItem
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderScreen(
    onAddOrderClick: () -> Unit = {},
    onOrderClick: (OrderItem) -> Unit = {},
    viewModel: OrderViewModel = koinViewModel(),
    analyticsHelper: CashVanAnalyticsHelper = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val orderItems = uiState.orders.map { it.toOrderItem() }

    // Bottom sheet state
    var showCancelBottomSheet by remember { mutableStateOf(false) }
    var selectedOrderForCancel by remember { mutableStateOf<OrderItem?>(null) }
    val cancelBottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val scope = rememberCoroutineScope()


    // Show print status snackbar
    LaunchedEffect(uiState.printStatus) {
        uiState.printStatus?.let {
            // Print status will be shown, then cleared after a delay
            kotlinx.coroutines.delay(3000)
            viewModel.clearPrintStatus()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            CashVanHeader()

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "طلبات اليوم",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily(Font(R.font.zain_regular)),
                color = Color(0xFF1F252E),
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            val tabs = listOf(OrderType.PRE_SELL, OrderType.CASH_VAN)
            val selectedTabIndex = tabs.indexOf(uiState.selectedTab)

            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                containerColor = Color.Transparent,
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = Color(0xFF0D3773)
                        )
                    }
                },
                divider = {}
            ) {
                tabs.forEachIndexed { index, orderType ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { viewModel.selectTab(orderType) },
                        text = {
                            Text(
                                text = orderType.displayName,
                                fontSize = 16.sp,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily(Font(R.font.zain_regular)),
                                color = if (selectedTabIndex == index) Color(0xFF0D3773) else Color(0xFF9E9E9E)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF0D3773)
                        )
                    }
                }
                uiState.error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "حدث خطأ في تحميل الطلبات",
                                color = Color.Red,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            IconButton(
                                onClick = { viewModel.refresh() }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "إعادة تحميل",
                                    tint = Color(0xFF0D3773)
                                )
                            }
                        }
                    }
                }
                orderItems.isEmpty() -> {
                    EmptyOrdersComponent(
                        modifier = Modifier.weight(1f),
                        onCreateOrderClick = onAddOrderClick
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(orderItems) { order ->
                            OrderCardItem(
                                order = order,
                                onOrderClick = onOrderClick,
                                onCancelClick = { orderItem ->
                                    selectedOrderForCancel = orderItem
                                    showCancelBottomSheet = true
                                },
                                onSubmitClick = { orderItem ->
                                    // Find the full order object from uiState
                                    val fullOrder = uiState.orders.find { it.id == orderItem.id }
                                    fullOrder?.let { viewModel.submitOrder(it) }
                                },
                                onPrintClick = { orderItem ->
                                    analyticsHelper.logEvent("order_print_invoice_clicked")
                                    viewModel.printInvoice(orderItem.id)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // Only show FAB if there are orders
        if (orderItems.isNotEmpty() && !uiState.isLoading && uiState.error == null) {
            FloatingActionButton(
                onClick = {
                    analyticsHelper.logEvent("plus_icon")
                    onAddOrderClick()
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .size(64.dp),
                containerColor = Color(0xFF0D3773),
                shape = CircleShape,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "إضافة طلب جديد",
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Cancel order bottom sheet
        if (showCancelBottomSheet && selectedOrderForCancel != null) {
            CancelOrderBottomSheet(
                sheetState = cancelBottomSheetState,
                onDismiss = {
                    scope.launch {
                        cancelBottomSheetState.hide()
                    }.invokeOnCompletion {
                        showCancelBottomSheet = false
                        selectedOrderForCancel = null
                    }
                },
                onConfirm = { reason, note ->
                    selectedOrderForCancel?.let { order ->
                        viewModel.cancelOrder(
                            orderId = order.id,
                            reason = reason,
                            note = note,
                            onSuccess = {
                                scope.launch {
                                    cancelBottomSheetState.hide()
                                }.invokeOnCompletion {
                                    showCancelBottomSheet = false
                                    selectedOrderForCancel = null
                                }
                            }
                        )
                    }
                },
                orderNumber = selectedOrderForCancel?.orderNumber ?: ""
            )
        }

        // Show print status message
        if (uiState.printStatus != null) {
            ErrorSnackbar(
                message = uiState.printStatus ?: "",
                onDismiss = { viewModel.clearPrintStatus() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, locale = "ar")
@Composable
fun OrderScreenPreview() {
    OrderScreen()
}

@Preview(showBackground = true, locale = "ar")
@Composable
fun OrderScreenEmptyPreview() {
    OrderScreen()
}