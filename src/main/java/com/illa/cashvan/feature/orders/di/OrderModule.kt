package com.illa.cashvan.feature.orders.di

import com.illa.cashvan.feature.orders.data.repository.OrderRepositoryImpl
import com.illa.cashvan.feature.orders.domain.repository.OrderRepository
import com.illa.cashvan.feature.orders.domain.usecase.CreateOrderUseCase
import com.illa.cashvan.feature.orders.domain.usecase.GetOngoingPlanUseCase
import com.illa.cashvan.feature.orders.domain.usecase.GetOrderByIdUseCase
import com.illa.cashvan.feature.orders.domain.usecase.GetOrdersUseCase
import com.illa.cashvan.feature.orders.domain.usecase.GetPlanProductsUseCase
import com.illa.cashvan.feature.orders.domain.usecase.SearchMerchantsUseCase
import com.illa.cashvan.feature.orders.presentation.viewmodel.CreateOrderViewModel
import com.illa.cashvan.feature.orders.presentation.viewmodel.OrderViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val orderModule = module {
    // Repository
    single<OrderRepository> { OrderRepositoryImpl(get()) }

    // Use Cases
    factory { GetOrdersUseCase(get()) }
    factory { GetOrderByIdUseCase(get()) }
    factory { CreateOrderUseCase(get()) }
    factory { GetOngoingPlanUseCase(get()) }
    factory { SearchMerchantsUseCase(get()) }
    factory { GetPlanProductsUseCase(get()) }

    // ViewModels
    viewModel { OrderViewModel(get(), get()) }
    viewModel { CreateOrderViewModel(get(), get(), get(), get()) }
}