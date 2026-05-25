package com.illa.cashvan.feature.kpi.di

import com.illa.cashvan.feature.kpi.data.repository.KpiRepositoryImpl
import com.illa.cashvan.feature.kpi.domain.repository.KpiRepository
import com.illa.cashvan.feature.kpi.domain.usecase.GetAgentKpiUseCase
import com.illa.cashvan.feature.kpi.presentation.viewmodel.KpiViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val kpiModule = module {
    single<KpiRepository> { KpiRepositoryImpl(get(named("supabase"))) }
    factory { GetAgentKpiUseCase(get()) }
    viewModel { KpiViewModel(get(), get()) }
}
