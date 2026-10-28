package com.illa.cashvan.feature.auth.di

import com.illa.cashvan.core.analytics.CashVanAnalyticsHelper
import com.illa.cashvan.core.app_preferences.domain.use_case.auth.LogoutUseCase
import com.illa.cashvan.feature.auth.data.repository.AuthRepositoryImpl
import com.illa.cashvan.feature.auth.data.repository.PresellAgentRepositoryImpl
import com.illa.cashvan.feature.auth.domain.repository.AuthRepository
import com.illa.cashvan.feature.auth.domain.repository.PresellAgentRepository
import com.illa.cashvan.feature.auth.domain.usecase.LoginUseCase
import com.illa.cashvan.feature.auth.domain.usecase.RefreshAgentRolesUseCase
import com.illa.cashvan.feature.auth.domain.usecase.RefreshTokenUseCase
import com.illa.cashvan.feature.auth.domain.usecase.ResolveAgentSessionUseCase
import com.illa.cashvan.feature.auth.domain.usecase.SalesAgentLoginUseCase
import com.illa.cashvan.feature.auth.presentation.viewmodel.SignInViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val authModule = module {
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get(), get()) }
    single<PresellAgentRepository> { PresellAgentRepositoryImpl(get()) }
    single { LoginUseCase(get()) }
    single { ResolveAgentSessionUseCase(get()) }
    single { SalesAgentLoginUseCase(get(), get()) }
    single { RefreshAgentRolesUseCase(get(), get(), get(), get()) }
    single { LogoutUseCase(get(), get(), getAll()) }
    single { RefreshTokenUseCase(get(),get(), get()) }
    viewModel {
        SignInViewModel(
            loginUseCase = get(),
            resolveAgentSessionUseCase = get(),
            salesAgentLoginUseCase = get(),
            saveAccessTokenUseCase = get(),
            saveRefreshTokenUseCase = get(),
            saveUserUseCase = get(),
            saveAgentSessionUseCase = get(),
            analyticsHelper = get<CashVanAnalyticsHelper>(),
            salesAnalytics = get()
        )
    }
}