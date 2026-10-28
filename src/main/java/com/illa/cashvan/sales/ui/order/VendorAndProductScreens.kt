package com.illa.cashvan.sales.ui.order

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.illa.cashvan.sales.feature.order.presentation.viewmodel.PresellOrderUiState
import com.illa.cashvan.sales.feature.order.presentation.viewmodel.PresellOrderViewModel
import com.illa.cashvan.sales.ui.components.EmptyState
import com.illa.cashvan.sales.ui.components.LoadingBox
import com.illa.cashvan.sales.ui.components.SalesColors
import com.illa.cashvan.sales.ui.components.SalesTopBar
import com.illa.cashvan.sales.ui.components.ZainBold
import com.illa.cashvan.sales.ui.components.ZainRegular
import com.illa.cashvan.sales.ui.components.salesFieldColors
import com.illa.cashvan.sales.ui.route.priceTierLabel

@Composable
private fun OrderMessages(state: PresellOrderUiState, viewModel: PresellOrderViewModel, snackbarHostState: SnackbarHostState) {
    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHostState.showSnackbar(listOfNotNull(message.title, message.description).joinToString("\n"))
            viewModel.messageShown()
        }
    }
}

/**
 * Pick a vendor (principal). Leaving asks first when the baskets have something, since leaving empties them.
 */
@Composable
fun VendorSelectionScreen(
    viewModel: PresellOrderViewModel,
    onVendorClick: (String) -> Unit,
    onCheckout: () -> Unit,
    onExit: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmExit by remember { mutableStateOf(false) }
    OrderMessages(state, viewModel, snackbarHostState)

    val exit = {
        if (state.cart.isNotEmpty() || state.requests.isNotEmpty()) confirmExit = true else onExit()
    }
    BackHandler(onBack = exit)

    Scaffold(
        containerColor = SalesColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SalesTopBar(
                title = state.context?.merchantName.orEmpty(),
                subtitle = "اختار المورد • ${priceTierLabel(state.pricing?.priceTier ?: state.context?.priceTier)}",
                onBack = exit,
                actions = {
                    IconButton(onClick = { viewModel.refreshInventory() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "تحديث المخزون", tint = SalesColors.Primary)
                    }
                }
            )
        },
        bottomBar = {
            CartBar(state.cart.size, state.cartTotal, state.requests.size, onCheckout)
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                state.vendorsLoading -> LoadingBox()
                state.noRegion -> EmptyState(
                    icon = Icons.Outlined.Storefront,
                    title = "التاجر مش مربوط بمحافظة",
                    description = "مينفعش نعرض أسعار من غير محافظة التاجر. كلم المشرف عشان يحدثها."
                )
                state.loadFailed -> EmptyState(
                    icon = Icons.Outlined.Refresh,
                    title = "فشل تحميل المنتجات",
                    action = { TextButton(onClick = viewModel::loadVendors) { Text("حاول تاني", fontFamily = ZainBold) } }
                )
                state.visibleVendors.isEmpty() -> EmptyState(icon = Icons.Outlined.Inventory2, title = "مفيش منتجات متاحة للتاجر ده")
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    state.inventoryContext.blockMessage?.let { message ->
                        item { InventoryNotice(message) }
                    }
                    items(state.visibleVendors, key = { it.id }) { vendor ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onVendorClick(vendor.id) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Storefront, contentDescription = null, tint = SalesColors.Primary)
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 12.dp)
                                ) {
                                    Text(vendor.name, fontSize = 16.sp, fontFamily = ZainBold)
                                    Text("${vendor.productCount} منتج", fontSize = 12.sp, color = SalesColors.Muted, fontFamily = ZainRegular)
                                }
                                val inCart = state.cart.count { it.product.principal_id == vendor.id }
                                if (inCart > 0) {
                                    Text(
                                        "$inCart في السلة",
                                        fontSize = 11.sp,
                                        fontFamily = ZainBold,
                                        color = SalesColors.Success,
                                        modifier = Modifier
                                            .background(SalesColors.Success.copy(alpha = 0.1f), RoundedCornerShape(50))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = SalesColors.Muted)
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("الخروج من الطلب؟", fontFamily = ZainBold) },
            text = { Text("هيتم مسح السلة والطلبات المؤجلة للتاجر ده.", fontFamily = ZainRegular) },
            confirmButton = {
                TextButton(onClick = {
                    confirmExit = false
                    viewModel.discardOrder()
                    onExit()
                }) { Text("خروج", color = SalesColors.Danger, fontFamily = ZainBold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmExit = false }) { Text("كمل الطلب", fontFamily = ZainBold) }
            }
        )
    }
}

@Composable
fun InventoryNotice(message: String) {
    Text(
        message,
        fontSize = 13.sp,
        fontFamily = ZainRegular,
        color = SalesColors.Progress,
        modifier = Modifier
            .fillMaxWidth()
            .background(SalesColors.Progress.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
            .padding(12.dp)
    )
}

@Composable
fun ProductSelectionScreen(
    viewModel: PresellOrderViewModel,
    onBack: () -> Unit,
    onCheckout: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    OrderMessages(state, viewModel, snackbarHostState)
    val vendorName = state.vendors.firstOrNull { it.id == state.selectedVendorId }?.name

    Scaffold(
        containerColor = SalesColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(modifier = Modifier.background(Color.White)) {
                SalesTopBar(
                    title = vendorName ?: "المنتجات",
                    subtitle = state.context?.merchantName,
                    onBack = onBack,
                    actions = {
                        IconButton(onClick = { viewModel.refreshInventory() }) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "تحديث المخزون", tint = SalesColors.Primary)
                        }
                    }
                )
                OutlinedTextField(
                    value = state.productSearch,
                    onValueChange = viewModel::onProductSearch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true,
                    placeholder = { Text("ابحث عن منتج...", color = Color(0xFFBDBDBD)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = SalesColors.Muted) },
                    shape = RoundedCornerShape(10.dp),
                    colors = salesFieldColors()
                )
            }
        },
        bottomBar = { CartBar(state.cart.size, state.cartTotal, state.requests.size, onCheckout) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                state.productsLoading -> LoadingBox()
                state.visibleProducts.isEmpty() -> EmptyState(icon = Icons.Outlined.Inventory2, title = "مفيش منتجات")
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    state.inventoryContext.blockMessage?.let { message -> item { InventoryNotice(message) } }
                    items(state.visibleProducts, key = { it.id }) { product ->
                        ProductCard(
                            product = product,
                            policy = state.policyFor(product),
                            inventoryContext = state.inventoryContext,
                            cartQuantity = state.cartQuantity(product.id),
                            requestQuantity = state.requestQuantity(product.id),
                            onAdd = { viewModel.addToCart(product) },
                            onCartQuantityChange = { viewModel.setCartQuantity(product, it) },
                            onRequestQuantityChange = { viewModel.setRequestQuantity(product, it) }
                        )
                    }
                }
            }
        }
    }
}
