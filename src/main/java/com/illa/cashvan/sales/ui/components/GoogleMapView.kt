package com.illa.cashvan.sales.ui.components

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer

/**
 * A Google [MapView] tied to the screen's lifecycle. [onUpdate] runs with the map whenever the composable
 * recomposes, so it should redraw markers from current state.
 *
 * Needs a Maps SDK key in the manifest (`MAPS_API_KEY`); without one the map stays blank.
 */
@Composable
fun GoogleMapView(
    modifier: Modifier = Modifier,
    onUpdate: (GoogleMap) -> Unit
) {
    val context = LocalContext.current
    val mapView = remember {
        MapsInitializer.initialize(context.applicationContext)
        MapView(context).apply { onCreate(Bundle()) }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }
    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view -> view.getMapAsync(onUpdate) }
    )
}
