package com.illa.cashvan.core.app_preferences.di

import com.illa.cashvan.core.app_preferences.data.repository.PreferencesRepoImpl
import com.illa.cashvan.core.app_preferences.domain.repository.PreferencesRepo
import com.illa.cashvan.core.app_preferences.domain.use_case.app_cache.ClearAppDataUseCase
import com.illa.cashvan.core.app_preferences.domain.use_case.token.ClearAccessTokenUseCase
import com.illa.cashvan.core.app_preferences.domain.use_case.token.ClearRefreshTokenUseCase
import com.illa.cashvan.core.app_preferences.domain.use_case.token.GetRefreshTokenUseCase
import com.illa.cashvan.core.app_preferences.domain.use_case.token.GetTokenUseCase
import com.illa.cashvan.core.app_preferences.domain.use_case.token.SaveAccessTokenUseCase
import com.illa.cashvan.core.app_preferences.domain.use_case.token.SaveRefreshTokenUseCase
import com.illa.cashvan.core.app_preferences.domain.use_case.user.ClearUserUseCase
import com.illa.cashvan.core.app_preferences.domain.use_case.user.GetUserUseCase
import com.illa.cashvan.core.app_preferences.domain.use_case.user.SaveUserUseCase
import com.illa.cashvan.core.app_preferences.domain.use_case.auth.IsUserLoggedInUseCase
import com.illa.cashvan.core.app_preferences.domain.use_case.auth.LogoutUseCase
import com.illa.cashvan.core.auth.presentation.viewmodel.AuthenticationViewModel
import com.illa.cashvan.core.user.presentation.viewmodel.UserViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val preferencesModule = module {

    // Repository
    single<PreferencesRepo> { PreferencesRepoImpl(androidContext()) }

    // Token Use Cases
    single { GetTokenUseCase(get()) }
    single { GetRefreshTokenUseCase(get()) }
    single { SaveAccessTokenUseCase(get()) }
    single { SaveRefreshTokenUseCase(get()) }
    single { ClearAccessTokenUseCase(get()) }
    single { ClearRefreshTokenUseCase(get()) }

    // App Cache Use Cases
    single { ClearAppDataUseCase(get()) }

    // User Use Cases
    single { SaveUserUseCase(get()) }
    single { GetUserUseCase(get()) }
    single { ClearUserUseCase(get()) }

    // Auth Use Cases
    single { IsUserLoggedInUseCase(get(), get()) }
    single { LogoutUseCase(get(),get()) }

    // ViewModels
    viewModel { UserViewModel(get()) }
    viewModel { AuthenticationViewModel(get(), get()) }
}