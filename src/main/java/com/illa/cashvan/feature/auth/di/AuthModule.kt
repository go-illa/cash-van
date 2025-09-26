
package com.illa.cashvan.feature.auth.di

import com.illa.cashvan.core.app_preferences.domain.use_case.auth.LogoutUseCase
import com.illa.cashvan.feature.auth.data.repository.AuthRepositoryImpl
import com.illa.cashvan.feature.auth.domain.repository.AuthRepository
import com.illa.cashvan.feature.auth.domain.usecase.LoginUseCase
import com.illa.cashvan.feature.auth.presentation.viewmodel.SignInViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val authModule = module {
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get()) }
    single { LoginUseCase(get()) }
    single { LogoutUseCase(get(), get()) }
    viewModel { SignInViewModel(get(), get(), get(), get()) }
}