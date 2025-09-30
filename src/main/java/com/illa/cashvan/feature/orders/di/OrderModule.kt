package com.illa.cashvan.feature.orders.di

import com.illa.cashvan.feature.orders.data.repository.OrderRepositoryImpl
import com.illa.cashvan.feature.orders.domain.repository.OrderRepository
import com.illa.cashvan.feature.orders.domain.usecase.GetOrdersUseCase
import com.illa.cashvan.feature.orders.domain.usecase.GetOrderByIdUseCase
import com.illa.cashvan.feature.orders.presentation.viewmodel.OrderViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val orderModule = module {
    // Repository
    single<OrderRepository> { OrderRepositoryImpl(get()) }

    // Use Cases
    factory { GetOrdersUseCase(get()) }
    factory { GetOrderByIdUseCase(get()) }

    // ViewModels
    viewModel { OrderViewModel(get(), get()) }
}