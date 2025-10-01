package com.illa.cashvan.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.illa.cashvan.core.location.LocationPermissionHandler
import com.illa.cashvan.core.location.LocationViewModel
import com.illa.cashvan.ui.orders.OrderScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onCreateOrderClick: () -> Unit = {},
    onOrderDetailsClick: (String) -> Unit = {},
    locationViewModel: LocationViewModel = koinViewModel()
) {
    LocationPermissionHandler(
        onPermissionGranted = {
            locationViewModel.onPermissionGranted()
        },
        onPermissionDenied = {
            locationViewModel.onPermissionDenied()
        }
    ) { requestPermission, isPermissionGranted ->
        LaunchedEffect(Unit) {
            if (!isPermissionGranted) {
                requestPermission()
            }
        }

        OrderScreen(
            onAddOrderClick = onCreateOrderClick,
            onOrderClick = { order ->
                onOrderDetailsClick(order.id)
            }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, locale = "ar")
@Composable
fun HomeScreenPreview() {
    HomeScreen()
}