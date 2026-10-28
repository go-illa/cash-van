package com.illa.cashvan.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.illa.cashvan.core.auth.presentation.viewmodel.AuthenticationState
import com.illa.cashvan.core.auth.presentation.viewmodel.AuthenticationViewModel
import com.illa.cashvan.core.session.AgentRole
import com.illa.cashvan.fulfillment.navigation.FulfillmentNavigation
import com.illa.cashvan.sales.navigation.SalesNavigation
import com.illa.cashvan.ui.modulepicker.ModulePickerScreen
import com.illa.cashvan.ui.signin.SignInScreen
import com.illa.cashvan.ui.splash.SplashScreen
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

private enum class AppDestination { Splash, SignIn, ModulePicker, Fulfillment, Sales }

private const val SPLASH_DURATION_MS = 3_000L

/**
 * Splash → sign-in → (module picker) → the module the agent's roles allow.
 *
 * @param onLogout runs after local data is cleared; the activity restarts to drop every module's state.
 */
@Composable
fun AppNavigation(
    onLogout: () -> Unit,
    authenticationViewModel: AuthenticationViewModel = koinViewModel()
) {
    val authState by authenticationViewModel.authState.collectAsStateWithLifecycle()
    var splashComplete by remember { mutableStateOf(false) }
    var destination by remember { mutableStateOf(AppDestination.Splash) }

    LaunchedEffect(Unit) {
        delay(SPLASH_DURATION_MS)
        splashComplete = true
    }

    LaunchedEffect(authState, splashComplete) {
        if (splashComplete && !authState.isLoading) {
            destination = destinationFor(authState)
        }
    }

    val logout: () -> Unit = { authenticationViewModel.logout(onComplete = onLogout) }
    val switchModule: (() -> Unit)? =
        if (authState.canSwitchModule) authenticationViewModel::switchModule else null

    Crossfade(targetState = destination, label = "app_destination") { target ->
        when (target) {
            AppDestination.Splash -> Scaffold { padding ->
                SplashScreen(modifier = Modifier.padding(padding))
            }
            AppDestination.SignIn -> Scaffold { padding ->
                SignInScreen(
                    modifier = Modifier.padding(padding),
                    onSignInSuccess = authenticationViewModel::refreshAuthState
                )
            }
            AppDestination.ModulePicker -> Scaffold { padding ->
                ModulePickerScreen(
                    modifier = Modifier.padding(padding),
                    onModuleSelected = authenticationViewModel::selectModule,
                    onLogout = logout
                )
            }
            AppDestination.Fulfillment -> FulfillmentNavigation(
                onLogout = logout,
                onSwitchModule = switchModule
            )
            AppDestination.Sales -> SalesNavigation(
                onLogout = logout,
                onSwitchModule = switchModule
            )
        }
    }
}

private fun destinationFor(state: AuthenticationState): AppDestination = when {
    !state.isLoggedIn -> AppDestination.SignIn
    state.activeModule == AgentRole.SALES -> AppDestination.Sales
    state.activeModule == AgentRole.FULFILLMENT -> AppDestination.Fulfillment
    else -> AppDestination.ModulePicker
}
