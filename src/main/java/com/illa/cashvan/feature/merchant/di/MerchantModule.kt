package com.illa.cashvan.feature.merchant.di

import com.illa.cashvan.feature.merchant.data.repository.MerchantRepositoryImpl
import com.illa.cashvan.feature.merchant.domain.repository.MerchantRepository
import com.illa.cashvan.feature.merchant.domain.usecase.CreateMerchantUseCase
import com.illa.cashvan.feature.merchant.presentation.viewmodel.MerchantViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val merchantModule = module {

    single<MerchantRepository> {
        MerchantRepositoryImpl(
            httpClient = get()
        )
    }

    single {
        CreateMerchantUseCase(
            merchantRepository = get()
        )
    }

    viewModel {
        MerchantViewModel(
            createMerchantUseCase = get(),
            getPlansUseCase = get()
        )
    }
}