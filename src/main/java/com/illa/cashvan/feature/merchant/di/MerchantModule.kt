package com.illa.cashvan.feature.merchant.di

import com.illa.cashvan.feature.merchant.data.repository.MerchantRepositoryImpl
import com.illa.cashvan.feature.merchant.domain.repository.MerchantRepository
import com.illa.cashvan.feature.merchant.domain.usecase.CreateMerchantUseCase
import com.illa.cashvan.feature.merchant.domain.usecase.GetGovernoratesUseCase
import com.illa.cashvan.feature.merchant.domain.usecase.GetMerchantTypesUseCase
import com.illa.cashvan.feature.merchant.domain.usecase.GetReverseGeocodeUseCase
import com.illa.cashvan.feature.merchant.presentation.viewmodel.MerchantViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val merchantModule = module {

    single<MerchantRepository> {
        MerchantRepositoryImpl(httpClient = get())
    }

    single {
        CreateMerchantUseCase(merchantRepository = get())
    }

    single {
        GetGovernoratesUseCase(merchantRepository = get())
    }

    single {
        GetMerchantTypesUseCase(merchantRepository = get())
    }

    single {
        GetReverseGeocodeUseCase(merchantRepository = get())
    }

    viewModel {
        MerchantViewModel(
            createMerchantUseCase = get(),
            getPlansUseCase = get(),
            getGovernoratesUseCase = get(),
            getMerchantTypesUseCase = get(),
            getReverseGeocodeUseCase = get()
        )
    }
}
