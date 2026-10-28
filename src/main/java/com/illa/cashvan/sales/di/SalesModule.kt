package com.illa.cashvan.sales.di

import androidx.room.Room
import com.illa.cashvan.core.session.LogoutCleaner
import com.illa.cashvan.sales.data.CurrentAgentProvider
import com.illa.cashvan.sales.feature.cart.data.local.SalesDatabase
import com.illa.cashvan.sales.feature.cart.data.repository.PresellCartRepositoryImpl
import com.illa.cashvan.sales.feature.cart.domain.repository.PresellCartRepository
import com.illa.cashvan.sales.feature.cashvan.data.repository.CashVanRepositoryImpl
import com.illa.cashvan.sales.feature.cashvan.domain.repository.CashVanRepository
import com.illa.cashvan.sales.feature.cashvan.presentation.viewmodel.CashVanHomeViewModel
import com.illa.cashvan.sales.feature.cashvan.presentation.viewmodel.CashVanMerchantsViewModel
import com.illa.cashvan.sales.feature.cashvan.presentation.viewmodel.CashVanOrderViewModel
import com.illa.cashvan.sales.feature.cashvan.presentation.viewmodel.ReconciliationViewModel
import com.illa.cashvan.sales.feature.catalog.data.repository.CatalogRepositoryImpl
import com.illa.cashvan.sales.feature.checkout.data.repository.PresellCheckoutRepositoryImpl
import com.illa.cashvan.sales.feature.checkout.domain.repository.PresellCheckoutRepository
import com.illa.cashvan.sales.feature.catalog.domain.repository.CatalogRepository
import com.illa.cashvan.sales.feature.fawry.data.repository.FawryQuestionnaireRepositoryImpl
import com.illa.cashvan.sales.feature.fawry.domain.repository.FawryQuestionnaireRepository
import com.illa.cashvan.sales.feature.kpi.data.repository.SalesKpiRepositoryImpl
import com.illa.cashvan.sales.feature.kpi.domain.repository.SalesKpiRepository
import com.illa.cashvan.sales.feature.merchant.data.repository.PresellMerchantRepositoryImpl
import com.illa.cashvan.sales.feature.nearby.data.repository.NearbyRepositoryImpl
import com.illa.cashvan.sales.feature.nearby.domain.repository.NearbyRepository
import com.illa.cashvan.sales.feature.merchant.domain.repository.PresellMerchantRepository
import com.illa.cashvan.sales.feature.orders.data.repository.PresellOrderRepositoryImpl
import com.illa.cashvan.sales.feature.orders.domain.repository.PresellOrderRepository
import com.illa.cashvan.sales.feature.route.data.repository.RouteRepositoryImpl
import com.illa.cashvan.sales.feature.route.domain.repository.RouteRepository
import com.illa.cashvan.sales.feature.visit.data.repository.PresellVisitRepositoryImpl
import com.illa.cashvan.sales.feature.visit.domain.repository.PresellVisitRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.illa.cashvan.sales.feature.merchant.presentation.viewmodel.CreatePresellMerchantViewModel
import com.illa.cashvan.sales.feature.order.presentation.viewmodel.PresellOrderViewModel
import com.illa.cashvan.sales.feature.orders.presentation.viewmodel.OrderHistoryViewModel
import com.illa.cashvan.sales.feature.kpi.presentation.viewmodel.LeaderboardViewModel
import com.illa.cashvan.sales.feature.kpi.presentation.viewmodel.SalesKpiViewModel
import com.illa.cashvan.sales.feature.nearby.presentation.viewmodel.CoverageMapViewModel
import com.illa.cashvan.sales.feature.nearby.presentation.viewmodel.NearbyMerchantsViewModel
import com.illa.cashvan.sales.feature.profile.presentation.viewmodel.SalesProfileViewModel
import com.illa.cashvan.sales.feature.route.presentation.viewmodel.AdHocOrderViewModel
import com.illa.cashvan.sales.feature.route.presentation.viewmodel.RouteViewModel
import com.illa.cashvan.sales.feature.visit.presentation.viewmodel.MerchantDetailViewModel
import com.illa.cashvan.sales.feature.visit.presentation.viewmodel.VisitFlowViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val salesModule = module {
    single { CurrentAgentProvider(get()) }

    single {
        Room.databaseBuilder(androidContext(), SalesDatabase::class.java, SalesDatabase.NAME).build()
    }
    single { get<SalesDatabase>().cartDao() }
    single<LogoutCleaner>(named("salesDatabase")) {
        val database = get<SalesDatabase>()
        LogoutCleaner { withContext(Dispatchers.IO) { database.clearAllTables() } }
    }

    single<RouteRepository> { RouteRepositoryImpl(get(), get()) }
    single<PresellCartRepository> { PresellCartRepositoryImpl(get(), get()) }
    single<CatalogRepository> { CatalogRepositoryImpl(get(), get()) }
    single<PresellOrderRepository> { PresellOrderRepositoryImpl(get(), get()) }
    single<PresellCheckoutRepository> { PresellCheckoutRepositoryImpl(get(), get()) }
    single<PresellMerchantRepository> { PresellMerchantRepositoryImpl(get(), get()) }
    single<FawryQuestionnaireRepository> { FawryQuestionnaireRepositoryImpl(get()) }
    single<CashVanRepository> { CashVanRepositoryImpl(get(), get()) }
    single<SalesKpiRepository> { SalesKpiRepositoryImpl(get(), get()) }
    single<NearbyRepository> { NearbyRepositoryImpl(get(), get()) }

    viewModel { RouteViewModel(get(), get(), get(), get()) }
    viewModel { VisitFlowViewModel(get(), get(), get(), get()) }
    viewModel { MerchantDetailViewModel(get()) }
    viewModel { CreatePresellMerchantViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { PresellOrderViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { AdHocOrderViewModel(get(), get()) }
    viewModel { OrderHistoryViewModel(get(), get()) }
    viewModel { CashVanMerchantsViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { CashVanOrderViewModel(get(), get()) }
    viewModel { CashVanHomeViewModel(get()) }
    viewModel { ReconciliationViewModel(get()) }
    viewModel { SalesProfileViewModel(get(), get(), get()) }
    viewModel { SalesKpiViewModel(get(), get(), get()) }
    viewModel { LeaderboardViewModel(get()) }
    viewModel { CoverageMapViewModel(get(), get()) }
    viewModel { NearbyMerchantsViewModel(get(), get(), get(), get(), get()) }
    single<PresellVisitRepository> { PresellVisitRepositoryImpl(get(), get()) }
}
