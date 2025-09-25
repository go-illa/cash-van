package com.illa.cashvan.core.location

import android.Manifest
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationPermissionHandler(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit,
    content: @Composable (requestPermission: () -> Unit, isPermissionGranted: Boolean) -> Unit
) {
    val context = LocalContext.current

    val locationPermissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    )

    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted) {
            onPermissionGranted()
        } else if (locationPermissionsState.shouldShowRationale) {
            onPermissionDenied()
        }
    }

    content(
        { locationPermissionsState.launchMultiplePermissionRequest() },
        locationPermissionsState.allPermissionsGranted
    )
}