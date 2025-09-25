package com.illa.cashvan.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.*
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.illa.cashvan.core.auth.presentation.viewmodel.AuthenticationViewModel
import com.illa.cashvan.ui.home.HomeScreen
import com.illa.cashvan.ui.inventory.InventoryScreen
import com.illa.cashvan.ui.orders.CreateOrderScreen
import com.illa.cashvan.ui.orders.OrderDetailsScreen
import com.illa.cashvan.ui.profile.PersonalProfileScreen
import com.illa.cashvan.ui.signin.SignInScreen
import com.illa.cashvan.ui.splash.SplashScreen
import org.koin.androidx.compose.koinViewModel

data class BottomNavItem(
    val key: Any,
    val icon: ImageVector,
    val label: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashVanNavigation(
    authenticationViewModel: AuthenticationViewModel = koinViewModel()
) {
    val authState by authenticationViewModel.authState.collectAsState()
    val backStack = remember { mutableStateListOf<Any>(SplashKey) }
    val currentKey = backStack.lastOrNull() ?: SplashKey

    // Handle authentication state changes
    LaunchedEffect(authState) {
        if (!authState.isLoading) {
            backStack.clear()
            if (authState.isLoggedIn) {
                backStack.add(HomeKey)
            } else {
                backStack.add(SignInKey)
            }
        }
    }

    val bottomNavItems = listOf(
        BottomNavItem(HomeKey, Icons.Default.Home, "الاوردرات"),
        BottomNavItem(InventoryKey, Icons.Default.Inventory, "مخزون"),
        BottomNavItem(ProfileKey, Icons.Default.Person, "الملف الشخصي"),
    )

    val isSignInKey = currentKey == SignInKey
    val isSplashKey = currentKey == SplashKey

    Scaffold(
        bottomBar = {
            if (!isSignInKey && !isSplashKey) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentKey == item.key,
                            onClick = {
                                if (currentKey != HomeKey && item.key != HomeKey) {
                                    backStack.removeLastOrNull()
                                }
                                if (currentKey != item.key) {
                                    backStack.add(item.key)
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavDisplay(
            backStack = backStack,
            onBack = {
                if (backStack.size > 1) {
                    backStack.removeLastOrNull()
                }
            },
            modifier = Modifier.padding(paddingValues),
            entryProvider = { key ->
                when (key) {
                    SplashKey -> NavEntry(key) {
                        SplashScreen()
                    }
                    SignInKey -> NavEntry(key) {
                        SignInScreen(
                            onSignInSuccess = {
                                authenticationViewModel.refreshAuthState()
                            }
                        )
                    }
                    HomeKey -> NavEntry(key) {
                        HomeScreen(
                            onCreateOrderClick = {
                                backStack.add(CreateOrderKey)
                            },
                            onOrderDetailsClick = { orderId ->
                                backStack.add(OrderDetailsKey(orderId))
                            }
                        )
                    }
                    ProfileKey -> NavEntry(key) {
                        PersonalProfileScreen(
                            onLogout = {
                                authenticationViewModel.logout()
                            }
                        )
                    }
                    InventoryKey -> NavEntry(key) {
                        InventoryScreen(
                            onAddOrderClick = {
                                backStack.add(OrderDetailsKey("ORD-2024-${(100..999).random()}"))
                            },
                        )
                    }
                    CreateOrderKey -> NavEntry(key) {
                        CreateOrderScreen(
                            onBackClick = {
                                if (backStack.size > 1) {
                                    backStack.removeLastOrNull()
                                }
                            },
                            onCreateOrder = {
                                backStack.add(OrderDetailsKey("ORD-2024-${(100..999).random()}"))
                            }
                        )
                    }
                    is OrderDetailsKey -> NavEntry(key) {
                        OrderDetailsScreen(
                            orderId = key.orderId,
                            onBackClick = {
                                if (backStack.size > 1) {
                                    backStack.removeLastOrNull()
                                }
                            },
                            onConfirmOrder = {
                                backStack.clear()
                                backStack.add(HomeKey)
                            }
                        )
                    }
                    else -> NavEntry(Unit) {
                        Text("Unknown route")
                    }
                }
            }
        )
    }
}