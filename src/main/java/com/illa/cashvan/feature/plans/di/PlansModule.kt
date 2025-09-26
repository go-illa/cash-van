package com.illa.cashvan.feature.plans.di

import com.illa.cashvan.feature.plans.data.repository.PlansRepositoryImpl
import com.illa.cashvan.feature.plans.domain.repository.PlansRepository
import com.illa.cashvan.feature.plans.domain.usecase.GetPlansUseCase
import org.koin.dsl.module

val plansModule = module {
    single<PlansRepository> { PlansRepositoryImpl(get()) }
    single { GetPlansUseCase(get()) }
}