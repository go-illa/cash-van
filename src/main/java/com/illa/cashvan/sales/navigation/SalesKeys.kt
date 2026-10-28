package com.illa.cashvan.sales.navigation

data object SalesCashVanKey
data object SalesPresellKey
data object SalesKpiKey
data object SalesProfileKey
data object SalesOrderHistoryKey
data object SalesCreateMerchantKey
data object SalesAdHocKey
data object SalesVendorSelectionKey
data class SalesProductSelectionKey(val principalId: String?)
data object SalesCheckoutKey
data object SalesCashVanMerchantsKey
data object SalesCashVanCreateMerchantKey
data object SalesCashVanProductsKey
data object SalesCashVanCheckoutKey
data class SalesCashVanOrderDetailKey(val orderId: String)
data object SalesReconciliationKey
data object SalesKpiVisitsKey
data object SalesKpiCoverageKey
data object SalesKpiConversionKey
data object SalesKpiReportKey
data object SalesLeaderboardKey
data object SalesCoverageMapKey
data class SalesNearbyKey(val flow: String)

/** Screens that show the bottom bar, like the PWA's `TOP_LEVEL_ROUTES`. */
val SALES_TOP_LEVEL_KEYS = setOf<Any>(
    SalesCashVanKey, SalesPresellKey, SalesKpiKey, SalesProfileKey, SalesOrderHistoryKey,
    SalesKpiVisitsKey, SalesKpiCoverageKey, SalesKpiConversionKey, SalesKpiReportKey, SalesLeaderboardKey
)

/** Keys that highlight the المؤشرات tab: the PWA's KPI pages and the leaderboard. */
val SALES_KPI_KEYS = setOf<Any>(SalesKpiKey, SalesKpiVisitsKey, SalesKpiCoverageKey, SalesKpiConversionKey, SalesKpiReportKey, SalesLeaderboardKey)
