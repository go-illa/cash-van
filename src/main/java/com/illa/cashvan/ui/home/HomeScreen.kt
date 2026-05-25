package com.illa.cashvan.ui.home

import android.Manifest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.tooling.preview.Preview
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.illa.cashvan.core.location.LocationViewModel
import com.illa.cashvan.ui.orders.OrderScreen
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    onCreateOrderClick: () -> Unit = {},
    onCreateVisitWithOrder: () -> Unit = {},
    onCreateVisitWithoutOrder: () -> Unit = {},
    onOrderDetailsClick: (String) -> Unit = {},
    onVisitDetailsClick: (String) -> Unit = {},
    locationViewModel: LocationViewModel = koinViewModel()
) {
    val locationPermissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    )

    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted) {
            locationViewModel.onPermissionGranted()
        } else if (locationPermissionsState.shouldShowRationale) {
            locationViewModel.onPermissionDenied()
        }
    }

    LaunchedEffect(Unit) {
        if (!locationPermissionsState.allPermissionsGranted) {
            locationPermissionsState.launchMultiplePermissionRequest()
        }
    }

    OrderScreen(
        onAddOrderClick = onCreateOrderClick,
        onCreateVisitWithOrder = onCreateVisitWithOrder,
        onCreateVisitWithoutOrder = onCreateVisitWithoutOrder,
        onOrderClick = { order -> onOrderDetailsClick(order.id) },
        onVisitDetailsClick = onVisitDetailsClick
    )
}

@Preview(showBackground = true, showSystemUi = true, locale = "ar")
@Composable
fun HomeScreenPreview() {
    HomeScreen()
}
