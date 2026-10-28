package com.illa.cashvan.shared.feature.profile.di

import com.illa.cashvan.shared.feature.profile.data.repository.ProfileRepositoryImpl
import com.illa.cashvan.shared.feature.profile.domain.repository.ProfileRepository
import com.illa.cashvan.shared.feature.profile.domain.usecase.GetProfileUseCase
import com.illa.cashvan.shared.feature.profile.presentation.viewmodel.ProfileViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val profileModule = module {
    single<ProfileRepository> { ProfileRepositoryImpl(get()) }
    single { GetProfileUseCase(get()) }
    viewModel { ProfileViewModel(get(), get()) }
}