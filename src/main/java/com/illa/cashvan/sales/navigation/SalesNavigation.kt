package com.illa.cashvan.sales.navigation

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.illa.cashvan.core.analytics.CashVanAnalyticsHelper
import com.illa.cashvan.core.analytics.SalesAnalytics
import com.illa.cashvan.core.app_preferences.domain.use_case.user.GetUserUseCase
import com.illa.cashvan.sales.data.CurrentAgentProvider
import com.illa.cashvan.sales.domain.model.PresellOrderContext
import com.illa.cashvan.sales.feature.cashvan.presentation.viewmodel.CashVanHomeViewModel
import com.illa.cashvan.sales.feature.cashvan.presentation.viewmodel.CashVanOrderViewModel
import com.illa.cashvan.sales.domain.kpi.KpiDestination
import com.illa.cashvan.sales.domain.route.MerchantFilterStatus
import com.illa.cashvan.sales.feature.kpi.presentation.viewmodel.SalesKpiViewModel
import com.illa.cashvan.sales.feature.order.presentation.viewmodel.PresellOrderViewModel
import com.illa.cashvan.sales.feature.route.presentation.viewmodel.RouteViewModel
import com.illa.cashvan.sales.ui.cashvan.CashVanCheckoutScreen
import com.illa.cashvan.sales.ui.cashvan.CashVanHomeScreen
import com.illa.cashvan.sales.ui.cashvan.CashVanMerchantsScreen
import com.illa.cashvan.sales.ui.cashvan.CashVanProductsScreen
import com.illa.cashvan.sales.ui.cashvan.ReconciliationScreen
import com.illa.cashvan.sales.ui.kpi.KpiConversionScreen
import com.illa.cashvan.sales.ui.kpi.KpiCoverageScreen
import com.illa.cashvan.sales.ui.kpi.KpiHubScreen
import com.illa.cashvan.sales.ui.kpi.KpiReportScreen
import com.illa.cashvan.sales.ui.kpi.KpiVisitsScreen
import com.illa.cashvan.sales.ui.kpi.LeaderboardScreen
import com.illa.cashvan.sales.ui.nearby.CoverageMapScreen
import com.illa.cashvan.sales.ui.nearby.NearbyMerchantsScreen
import com.illa.cashvan.sales.ui.merchant.CreatePresellMerchantScreen
import com.illa.cashvan.sales.ui.more.SalesMoreSheet
import com.illa.cashvan.sales.ui.order.CheckoutReviewScreen
import com.illa.cashvan.sales.ui.order.ProductSelectionScreen
import com.illa.cashvan.sales.ui.order.VendorSelectionScreen
import com.illa.cashvan.sales.ui.orders.OrderHistoryScreen
import com.illa.cashvan.sales.ui.profile.SalesProfileScreen
import com.illa.cashvan.sales.ui.route.AdHocOrderScreen
import com.illa.cashvan.sales.ui.route.RouteScreen
import com.illa.cashvan.shared.ui.merchant.CreateMerchantScreen
import com.illa.cashvan.shared.ui.orders.OrderDetailsScreen
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private data class SalesTab(
    val key: Any,
    val icon: ImageVector,
    val label: String,
    val analyticsEvent: String
)

/**
 * The sales module, with the PWA's bottom bar: كاش فان | بري سيل | المؤشرات | المزيد.
 *
 * @param onSwitchModule non-null for agents who also have the fulfillment role.
 */
@Composable
fun SalesNavigation(
    onLogout: () -> Unit,
    onSwitchModule: (() -> Unit)? = null,
    analyticsHelper: CashVanAnalyticsHelper = koinInject(),
    orderViewModel: PresellOrderViewModel = koinViewModel(),
    routeViewModel: RouteViewModel = koinViewModel(),
    cashVanHomeViewModel: CashVanHomeViewModel = koinViewModel(),
    cashVanOrderViewModel: CashVanOrderViewModel = koinViewModel(),
    kpiViewModel: SalesKpiViewModel = koinViewModel(),
    salesAnalytics: SalesAnalytics = koinInject(),
    currentAgentProvider: CurrentAgentProvider = koinInject(),
    getUserUseCase: GetUserUseCase = koinInject()
) {
    // Presell-only agents (no Rails account) get no cash van, as in the PWA.
    var hasRailsAccess by remember { mutableStateOf(true) }
    // Agents who were already signed in still need an identity in the sales project.
    LaunchedEffect(Unit) {
        runCatching { currentAgentProvider.require() }.getOrNull()?.let { agent ->
            hasRailsAccess = agent.hasRailsAccess
            val user = getUserUseCase().firstOrNull()
            salesAnalytics.identify(agent.agentCode ?: user?.phone_number.orEmpty(), agent.agentId, user?.name)
        }
    }
    val trackedLogout: () -> Unit = {
        salesAnalytics.track("logout")
        salesAnalytics.reset()
        onLogout()
    }
    val backStack = remember { mutableStateListOf<Any>(SalesPresellKey) }
    val currentKey = backStack.lastOrNull() ?: SalesPresellKey
    var showMoreSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val tabs = listOf(
        SalesTab(SalesCashVanKey, Icons.Outlined.ShoppingBasket, "كاش فان", "sales_cashvan_tab"),
        SalesTab(SalesPresellKey, Icons.Outlined.Storefront, "بري سيل", "sales_presell_tab"),
        SalesTab(SalesKpiKey, Icons.Outlined.BarChart, "المؤشرات", "sales_kpi_tab"),
    )

    fun showTopLevel(key: Any) {
        backStack.removeAll { it == key }
        backStack.add(key)
    }

    fun pop() {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }

    fun startOrder(context: PresellOrderContext) {
        orderViewModel.start(context)
        backStack.add(SalesVendorSelectionKey)
    }

    /** Back to the cash van home after an order or visit, with fresh stock, orders and visit progress. */
    fun returnToCashVanHome() {
        backStack.removeAll { it !in SALES_TOP_LEVEL_KEYS }
        showTopLevel(SalesCashVanKey)
        cashVanHomeViewModel.load()
        routeViewModel.refresh()
    }

    /** Back to the route after the order flow, with fresh visit progress. */
    fun returnToRoute() {
        backStack.removeAll { it !in SALES_TOP_LEVEL_KEYS }
        showTopLevel(SalesPresellKey)
        routeViewModel.refresh()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (currentKey in SALES_TOP_LEVEL_KEYS) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            enabled = tab.key != SalesCashVanKey || hasRailsAccess,
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            selected = currentKey == tab.key || (tab.key == SalesKpiKey && currentKey in SALES_KPI_KEYS),
                            onClick = {
                                analyticsHelper.logEvent(tab.analyticsEvent)
                                showTopLevel(tab.key)
                            }
                        )
                    }
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Menu, contentDescription = "المزيد") },
                        label = { Text("المزيد") },
                        selected = showMoreSheet,
                        onClick = {
                            analyticsHelper.logEvent("sales_more_tab")
                            showMoreSheet = true
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavDisplay(
            backStack = backStack,
            onBack = { pop() },
            // Each screen gets its own ViewModels, cleared when it leaves the back stack. The route and the
            // order session are passed in from here, so they outlive their screens.
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            // Screens have their own Scaffolds; consuming the insets here stops them padding the status bar twice.
            modifier = Modifier
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues),
            entryProvider = { key ->
                when (key) {
                    SalesCashVanKey -> NavEntry(key) {
                        CashVanHomeScreen(
                            viewModel = cashVanHomeViewModel,
                            onSelectMerchant = { backStack.add(SalesCashVanMerchantsKey) },
                            onOrderClick = { backStack.add(SalesCashVanOrderDetailKey(it)) }
                        )
                    }
                    SalesCashVanMerchantsKey -> NavEntry(key) {
                        CashVanMerchantsScreen(
                            onBack = { pop() },
                            onStartOrder = { context ->
                                cashVanOrderViewModel.start(context)
                                backStack.add(SalesCashVanProductsKey)
                            },
                            onCreateMerchant = { backStack.add(SalesCashVanCreateMerchantKey) },
                            onVisitLogged = ::returnToCashVanHome,
                            onNearbyMerchants = { backStack.add(SalesNearbyKey("cashvan")) }
                        )
                    }
                    SalesCashVanCreateMerchantKey -> NavEntry(key) {
                        CreateMerchantScreen(
                            onBackClick = { pop() },
                            onMerchantCreated = {
                                pop()
                                scope.launch { snackbarHostState.showSnackbar("تم إضافة التاجر بنجاح") }
                            }
                        )
                    }
                    SalesCashVanProductsKey -> NavEntry(key) {
                        CashVanProductsScreen(
                            viewModel = cashVanOrderViewModel,
                            onBack = { pop() },
                            onCheckout = { backStack.add(SalesCashVanCheckoutKey) }
                        )
                    }
                    SalesCashVanCheckoutKey -> NavEntry(key) {
                        CashVanCheckoutScreen(
                            viewModel = cashVanOrderViewModel,
                            onBack = { pop() },
                            onCompleted = ::returnToCashVanHome
                        )
                    }
                    is SalesCashVanOrderDetailKey -> NavEntry(key) {
                        OrderDetailsScreen(
                            orderId = key.orderId,
                            onBackClick = { pop() },
                            onConfirmOrder = ::returnToCashVanHome
                        )
                    }
                    SalesReconciliationKey -> NavEntry(key) {
                        ReconciliationScreen(onBack = { pop() })
                    }
                    SalesPresellKey -> NavEntry(key) {
                        RouteScreen(
                            onOpenOrder = ::startOrder,
                            onCreateMerchant = { backStack.add(SalesCreateMerchantKey) },
                            onAdHocOrder = { backStack.add(SalesAdHocKey) },
                            onNearbyMerchants = { backStack.add(SalesNearbyKey("presell")) },
                            routeViewModel = routeViewModel
                        )
                    }
                    SalesKpiKey -> NavEntry(key) {
                        KpiHubScreen(
                            viewModel = kpiViewModel,
                            onOpen = { destination ->
                                kpiViewModel.trackHubClick(
                                    when (destination) {
                                        KpiDestination.VISITS -> "kpi_visits_card_clicked"
                                        KpiDestination.COVERAGE -> "kpi_coverage_card_clicked"
                                        KpiDestination.CONVERSION -> "kpi_conversion_card_clicked"
                                    },
                                    destination.name.lowercase()
                                )
                                kpiViewModel.resetPeriod()
                                backStack.add(
                                    when (destination) {
                                        KpiDestination.VISITS -> SalesKpiVisitsKey
                                        KpiDestination.COVERAGE -> SalesKpiCoverageKey
                                        KpiDestination.CONVERSION -> SalesKpiConversionKey
                                    }
                                )
                            },
                            onReport = {
                                kpiViewModel.trackHubClick("kpi_report_button_clicked", "report")
                                kpiViewModel.resetPeriod()
                                backStack.add(SalesKpiReportKey)
                            },
                            onLeaderboard = {
                                kpiViewModel.trackHubClick("kpi_leaderboard_button_clicked", "leaderboard")
                                backStack.add(SalesLeaderboardKey)
                            }
                        )
                    }
                    SalesKpiVisitsKey -> NavEntry(key) { KpiVisitsScreen(kpiViewModel, onBack = { pop() }) }
                    SalesKpiCoverageKey -> NavEntry(key) {
                        KpiCoverageScreen(kpiViewModel, onBack = { pop() }, onShowUnvisited = {
                            routeViewModel.onFilterChange(MerchantFilterStatus.UNVISITED)
                            backStack.removeAll { it !in SALES_TOP_LEVEL_KEYS || it in SALES_KPI_KEYS }
                            showTopLevel(SalesPresellKey)
                        })
                    }
                    SalesKpiConversionKey -> NavEntry(key) { KpiConversionScreen(kpiViewModel, onBack = { pop() }) }
                    SalesKpiReportKey -> NavEntry(key) { KpiReportScreen(kpiViewModel, onBack = { pop() }) }
                    SalesLeaderboardKey -> NavEntry(key) { LeaderboardScreen(onBack = { pop() }) }
                    SalesCoverageMapKey -> NavEntry(key) { CoverageMapScreen(onBack = { pop() }) }
                    is SalesNearbyKey -> NavEntry(key) { NearbyMerchantsScreen(flow = key.flow, onBack = { pop() }) }
                    SalesProfileKey -> NavEntry(key) {
                        SalesProfileScreen(
                            onLogout = trackedLogout,
                            onSwitchModule = onSwitchModule
                        )
                    }
                    SalesOrderHistoryKey -> NavEntry(key) {
                        OrderHistoryScreen(onBack = null)
                    }
                    SalesCreateMerchantKey -> NavEntry(key) {
                        CreatePresellMerchantScreen(
                            onBack = { pop() },
                            onCreated = { message ->
                                pop()
                                routeViewModel.refresh()
                                scope.launch { snackbarHostState.showSnackbar(message) }
                            }
                        )
                    }
                    SalesAdHocKey -> NavEntry(key) {
                        AdHocOrderScreen(
                            onBack = { pop() },
                            onStartOrder = { context ->
                                pop()
                                startOrder(context)
                            }
                        )
                    }
                    SalesVendorSelectionKey -> NavEntry(key) {
                        VendorSelectionScreen(
                            viewModel = orderViewModel,
                            onVendorClick = { principalId ->
                                orderViewModel.openVendor(principalId)
                                backStack.add(SalesProductSelectionKey(principalId))
                            },
                            onCheckout = { backStack.add(SalesCheckoutKey) },
                            onExit = { pop() }
                        )
                    }
                    is SalesProductSelectionKey -> NavEntry(key) {
                        ProductSelectionScreen(
                            viewModel = orderViewModel,
                            onBack = { pop() },
                            onCheckout = { backStack.add(SalesCheckoutKey) }
                        )
                    }
                    SalesCheckoutKey -> NavEntry(key) {
                        CheckoutReviewScreen(
                            viewModel = orderViewModel,
                            onBack = { pop() },
                            onCompleted = ::returnToRoute
                        )
                    }
                    else -> NavEntry(Unit) {
                        Text("Unknown route")
                    }
                }
            }
        )
    }

    if (showMoreSheet) {
        SalesMoreSheet(
            onDismiss = { showMoreSheet = false },
            onProfileClick = {
                showMoreSheet = false
                showTopLevel(SalesProfileKey)
            },
            onCreateMerchant = {
                showMoreSheet = false
                backStack.add(SalesCreateMerchantKey)
            },
            onOrderHistory = {
                showMoreSheet = false
                showTopLevel(SalesOrderHistoryKey)
            },
            onReconciliation = {
                showMoreSheet = false
                backStack.add(SalesReconciliationKey)
            },
            reconciliationEnabled = hasRailsAccess,
            onCoverageMap = {
                showMoreSheet = false
                backStack.add(SalesCoverageMapKey)
            },
            onSwitchModule = onSwitchModule?.let { switch ->
                {
                    showMoreSheet = false
                    switch()
                }
            },
            onLogout = {
                showMoreSheet = false
                trackedLogout()
            }
        )
    }
}
