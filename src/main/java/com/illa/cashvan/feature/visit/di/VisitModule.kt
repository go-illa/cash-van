package com.illa.cashvan.feature.visit.di

import com.illa.cashvan.feature.visit.data.repository.VisitRepositoryImpl
import com.illa.cashvan.feature.visit.domain.repository.VisitRepository
import com.illa.cashvan.feature.visit.domain.usecase.CreateVisitUseCase
import com.illa.cashvan.feature.visit.domain.usecase.GetNoOrderReasonsUseCase
import com.illa.cashvan.feature.visit.domain.usecase.GetVisitByIdUseCase
import com.illa.cashvan.feature.visit.domain.usecase.GetVisitsUseCase
import com.illa.cashvan.feature.visit.presentation.viewmodel.CreateVisitViewModel
import com.illa.cashvan.feature.visit.presentation.viewmodel.VisitListViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val visitModule = module {
    single<VisitRepository> { VisitRepositoryImpl(get()) }
    factory { GetNoOrderReasonsUseCase(get()) }
    factory { CreateVisitUseCase(get()) }
    factory { GetVisitsUseCase(get()) }
    factory { GetVisitByIdUseCase(get()) }
    viewModel { CreateVisitViewModel(get(), get(), get(), get()) }
    viewModel { VisitListViewModel(get(), get()) }
}
