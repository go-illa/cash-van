package com.illa.cashvan.feature.commission.di

import com.illa.cashvan.feature.commission.data.repository.CommissionRepository
import com.illa.cashvan.feature.commission.data.repository.CommissionRepositoryImpl
import com.illa.cashvan.feature.commission.domain.usecase.GetFulfillmentCommissionUseCase
import com.illa.cashvan.feature.commission.presentation.viewmodel.CommissionViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val commissionModule = module {
    single<CommissionRepository> { CommissionRepositoryImpl(get(named("supabase"))) }
    factory { GetFulfillmentCommissionUseCase(get()) }
    viewModel { CommissionViewModel(get(), get()) }
}
