package com.illa.cashvan

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.illa.cashvan.navigation.CashVanNavigation
import com.illa.cashvan.ui.theme.CashVanTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CashVanTheme {
                CashVanNavigation()
            }
        }
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