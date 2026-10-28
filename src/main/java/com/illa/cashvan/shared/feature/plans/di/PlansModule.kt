package com.illa.cashvan.shared.feature.plans.di

import com.illa.cashvan.shared.feature.plans.data.repository.PlansRepositoryImpl
import com.illa.cashvan.shared.feature.plans.domain.repository.PlansRepository
import com.illa.cashvan.shared.feature.plans.domain.usecase.GetPlanProductsUseCase
import com.illa.cashvan.shared.feature.plans.domain.usecase.GetPlansUseCase
import com.illa.cashvan.shared.feature.plans.presentation.viewmodel.InventoryViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val plansModule = module {
    single<PlansRepository> { PlansRepositoryImpl(get()) }
    single { GetPlansUseCase(get()) }
    single { GetPlanProductsUseCase(get()) }
    viewModel { InventoryViewModel(get(), get()) }
}