package com.illa.cashvan

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.illa.cashvan.core.connectivity.ConnectivityStatus
import com.illa.cashvan.core.connectivity.ConnectivityViewModel
import com.illa.cashvan.navigation.CashVanNavigation
import com.illa.cashvan.ui.common.NoInternetScreen
import com.illa.cashvan.ui.theme.CashVanTheme
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force light mode - disable dark mode completely
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        enableEdgeToEdge()
        setContent {
            CashVanTheme {
                // Inject ConnectivityViewModel
                val connectivityViewModel: ConnectivityViewModel = koinViewModel()
                val connectivityState by connectivityViewModel.connectivityStatus.collectAsState()
                val isRetrying by connectivityViewModel.isRetrying.collectAsState()

                // Wrap navigation with connectivity overlay
                Box(modifier = Modifier.fillMaxSize()) {
                    CashVanNavigation(
                        onLogout = {
                            restartApp()
                        }
                    )

                    // Show overlay when disconnected
                    if (connectivityState is ConnectivityStatus.Disconnected) {
                        NoInternetScreen(
                            isRetrying = isRetrying,
                            onRetry = { connectivityViewModel.checkConnectivity() }
                        )
                    }
                }
            }
        }
    }

    private fun restartApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(updateLocale(newBase, "ar"))
    }

    private fun updateLocale(context: Context?, language: String): Context? {
        if (context == null) return null

        val locale = Locale(language)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        return context.createConfigurationContext(config)
    }
}