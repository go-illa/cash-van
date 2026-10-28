package com.illa.cashvan.fulfillment.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.*
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.illa.cashvan.fulfillment.ui.home.HomeScreen
import com.illa.cashvan.fulfillment.ui.inventory.InventoryScreen
import com.illa.cashvan.shared.ui.merchant.CreateMerchantScreen
import com.illa.cashvan.shared.ui.orders.CreateOrderScreen
import com.illa.cashvan.shared.ui.orders.OrderDetailsScreen
import com.illa.cashvan.core.analytics.CashVanAnalyticsHelper
import com.illa.cashvan.shared.feature.orders.presentation.viewmodel.CreateOrderViewModel
import com.illa.cashvan.shared.ui.orders.ui_components.OrderConfirmationBottomSheet
import com.illa.cashvan.shared.ui.orders.ui_components.PaymentTypeDialog
import com.illa.cashvan.shared.ui.profile.PersonalProfileScreen
import com.illa.cashvan.fulfillment.ui.visit.CreateVisitWithoutOrderScreen
import com.illa.cashvan.fulfillment.ui.visit.VisitWithoutOrderDetailsScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

data class BottomNavItem(
    val key: Any,
    val icon: ImageVector,
    val label: String,
    val analyticsEvent: String
)

/**
 * The fulfillment module: today's Cash Van screens and bottom bar.
 *
 * @param onSwitchModule shown in Profile for agents who also have the sales role.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FulfillmentNavigation(
    onLogout: () -> Unit,
    onSwitchModule: (() -> Unit)? = null,
    createOrderViewModel: CreateOrderViewModel = koinViewModel(),
    analyticsHelper: CashVanAnalyticsHelper = koinInject()
) {
    val createOrderUiState by createOrderViewModel.uiState.collectAsStateWithLifecycle()
    val backStack = remember { mutableStateListOf<Any>(HomeKey) }
    val currentKey = backStack.lastOrNull() ?: HomeKey
    var merchantCreatedSignal by remember { mutableStateOf(0) }
    var showOrderResultSheet by remember { mutableStateOf(false) }
    val orderResultSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showPaymentTypeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(createOrderUiState.orderCreated, createOrderUiState.orderCreationError) {
        if ((createOrderUiState.orderCreated || createOrderUiState.orderCreationError != null) &&
            currentKey == HomeKey
        ) {
            showOrderResultSheet = true
        }
    }

    val bottomNavItems = listOf(
        BottomNavItem(HomeKey, Icons.Default.Home, "الاوردرات", "orders_tab"),
        BottomNavItem(InventoryKey, Icons.Default.Inventory, "مخزون", "stock_tab"),
        BottomNavItem(ProfileKey, Icons.Default.Person, "الملف الشخصي", "profile_tab"),
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentKey == item.key,
                        onClick = {
                            analyticsHelper.logEvent(item.analyticsEvent)
                            backStack.removeAll { it == item.key }
                            backStack.add(item.key)
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavDisplay(
            backStack = backStack,
            onBack = {
                if (backStack.size > 1) {
                    backStack.removeLastOrNull()
                }
            },
            modifier = Modifier.padding(paddingValues),
            entryProvider = { key ->
                when (key) {
                    HomeKey -> NavEntry(key) {
                        HomeScreen(
                            onCreateOrderClick = {
                                backStack.add(CreateOrderKey())
                            },
                            onCreateVisitWithOrder = {
                                showPaymentTypeDialog = true
                            },
                            onCreateVisitWithoutOrder = {
                                backStack.add(CreateVisitWithoutOrderKey)
                            },
                            onOrderDetailsClick = { orderId ->
                                backStack.add(OrderDetailsKey(orderId))
                            },
                            onVisitDetailsClick = { visitId ->
                                backStack.add(VisitWithoutOrderDetailsKey(visitId))
                            }
                        )
                    }
                    ProfileKey -> NavEntry(key) {
                        PersonalProfileScreen(
                            onLogout = onLogout,
                            onSwitchModule = onSwitchModule
                        )
                    }
                    InventoryKey -> NavEntry(key) {
                        InventoryScreen(
                            onAddOrderClick = {
                                backStack.add(CreateOrderKey())
                            },
                        )
                    }
                    is CreateOrderKey -> NavEntry(key) {
                        CreateOrderScreen(
                            viewModel = createOrderViewModel,
                            paymentType = key.paymentType,
                            merchantCreatedSignal = merchantCreatedSignal,
                            onAddMerchantClick = {
                                backStack.add(CreateMerchantKey)
                            },
                            onBackClick = {
                                if (backStack.size > 1) {
                                    backStack.removeLastOrNull()
                                }
                            },
                            onOrderCreated = {
                                if (backStack.size > 1) {
                                    backStack.removeLastOrNull()
                                }
                            }
                        )
                    }
                    CreateVisitWithoutOrderKey -> NavEntry(key) {
                        CreateVisitWithoutOrderScreen(
                            visitViewModel = koinViewModel(),
                            onBackClick = {
                                if (backStack.size > 1) {
                                    backStack.removeLastOrNull()
                                }
                            },
                            onVisitCreated = {
                                if (backStack.size > 1) {
                                    backStack.removeLastOrNull()
                                }
                            }
                        )
                    }
                    CreateMerchantKey -> NavEntry(key) {
                        CreateMerchantScreen(
                            onBackClick = {
                                if (backStack.size > 1) {
                                    backStack.removeLastOrNull()
                                }
                            },
                            onMerchantCreated = {
                                merchantCreatedSignal++
                                if (backStack.size > 1) {
                                    backStack.removeLastOrNull()
                                }
                            }
                        )
                    }
                    is OrderDetailsKey -> NavEntry(key) {
                        OrderDetailsScreen(
                            orderId = key.orderId,
                            onBackClick = {
                                if (backStack.size > 1) {
                                    backStack.removeLastOrNull()
                                }
                            },
                            onConfirmOrder = {
                                backStack.clear()
                                backStack.add(HomeKey)
                            }
                        )
                    }
                    is VisitWithoutOrderDetailsKey -> NavEntry(key) {
                        VisitWithoutOrderDetailsScreen(
                            visitId = key.visitId,
                            onBackClick = {
                                if (backStack.size > 1) {
                                    backStack.removeLastOrNull()
                                }
                            }
                        )
                    }
                    else -> NavEntry(Unit) {
                        Text("Unknown route")
                    }
                }
            }
        )
    }

    if (showOrderResultSheet) {
        OrderConfirmationBottomSheet(
            sheetState = orderResultSheetState,
            isSuccess = createOrderUiState.orderCreated,
            errorMessage = createOrderUiState.orderCreationError,
            onDismiss = {
                showOrderResultSheet = false
                createOrderViewModel.resetOrderCreated()
                createOrderViewModel.clearOrderCreationError()
            },
            onBackToHome = {
                showOrderResultSheet = false
                createOrderViewModel.resetOrderCreated()
                createOrderViewModel.clearOrderCreationError()
            }
        )
    }

    if (showPaymentTypeDialog) {
        PaymentTypeDialog(
            onDismiss = { showPaymentTypeDialog = false },
            onPaymentTypeSelected = { paymentType ->
                showPaymentTypeDialog = false
                backStack.add(CreateOrderKey(paymentType = paymentType))
            }
        )
    }

}
