package com.illa.cashvan.sales.ui.nearby

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.illa.cashvan.sales.domain.route.buildGoogleMapsSearchUrl
import com.illa.cashvan.sales.domain.route.calculateDistance
import com.illa.cashvan.sales.domain.route.formatDistance
import com.illa.cashvan.sales.feature.nearby.presentation.viewmodel.CoverageBucket
import com.illa.cashvan.sales.feature.nearby.presentation.viewmodel.CoverageMapViewModel
import com.illa.cashvan.sales.feature.nearby.presentation.viewmodel.coverageBucket
import com.illa.cashvan.sales.ui.components.GoogleMapView
import com.illa.cashvan.sales.ui.components.LoadingBox
import com.illa.cashvan.sales.ui.components.SalesColors
import com.illa.cashvan.sales.ui.components.SalesTopBar
import com.illa.cashvan.sales.ui.components.ZainBold
import com.illa.cashvan.sales.ui.components.ZainRegular
import com.illa.cashvan.sales.ui.components.salesFieldColors
import com.illa.cashvan.sales.ui.route.FawryBadge
import com.illa.cashvan.sales.ui.route.Pill
import com.illa.cashvan.sales.ui.route.priceTierLabel
import org.koin.androidx.compose.koinViewModel

private val BUCKET_COLORS = mapOf(
    CoverageBucket.UNVISITED to Color(0xFF6B7280),
    CoverageBucket.ONCE to Color(0xFFF97316),
    CoverageBucket.COMPLETED to Color(0xFF16A34A)
)
private val BUCKET_LABELS = mapOf(
    CoverageBucket.COMPLETED to "زيارتان",
    CoverageBucket.ONCE to "زيارة واحدة",
    CoverageBucket.UNVISITED to "لم تُزر"
)
private val EGYPT = LatLng(26.8, 30.8)

/** Zoom that fits the 10 nearest merchants around the agent, as the PWA's auto-zoom table. */
private fun zoomForDistanceKm(km: Double): Float = when {
    km < 0.15 -> 19f
    km < 0.25 -> 18f
    km < 0.4 -> 17f
    km < 0.8 -> 16f
    km < 1.5 -> 15f
    km < 3 -> 14f
    km < 6 -> 13f
    km < 12 -> 12f
    km < 25 -> 11f
    else -> 10f
}

private fun dot(color: Color, sizePx: Int): BitmapDescriptor {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val radius = sizePx / 2f
    canvas.drawCircle(radius, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = android.graphics.Color.WHITE })
    canvas.drawCircle(radius, radius, radius * 0.75f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color.toArgb() })
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

@Composable
fun CoverageMapScreen(onBack: () -> Unit, viewModel: CoverageMapViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val cameraPlaced = remember { mutableStateOf(false) }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(SalesColors.Background)) {
        SalesTopBar(title = "خريطة التغطية", onBack = onBack)
        Box(modifier = Modifier.weight(1f)) {
            if (state.isLoading) {
                LoadingBox()
            } else {
                val visible = state.visible
                GoogleMapView(modifier = Modifier.fillMaxSize()) { map ->
                    map.uiSettings.isMapToolbarEnabled = false
                    map.clear()
                    state.agentLocation?.let {
                        map.addMarker(MarkerOptions().position(LatLng(it.latitude, it.longitude)).icon(dot(Color(0xFF2563EB), 40)).zIndex(2f))
                    }
                    visible.forEach { merchant ->
                        val selected = merchant.merchantId == state.selectedId
                        map.addMarker(
                            MarkerOptions()
                                .position(LatLng(merchant.latitude!!, merchant.longitude!!))
                                .icon(dot(BUCKET_COLORS.getValue(merchant.coverageBucket()), if (selected) 52 else 36))
                                .zIndex(if (selected) 1f else 0f)
                        )?.tag = merchant.merchantId
                    }
                    map.setOnMarkerClickListener { marker ->
                        (marker.tag as? String)?.let(viewModel::select)
                        true
                    }
                    map.setOnMapClickListener { viewModel.select(null) }
                    val agent = state.agentLocation
                    when {
                        state.search.isNotBlank() && visible.isNotEmpty() -> {
                            if (visible.size == 1) {
                                map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(visible[0].latitude!!, visible[0].longitude!!), 17f))
                            } else {
                                val bounds = LatLngBounds.builder().apply { visible.forEach { include(LatLng(it.latitude!!, it.longitude!!)) } }.build()
                                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 64))
                            }
                        }
                        !cameraPlaced.value -> {
                            cameraPlaced.value = true
                            if (agent == null) {
                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(EGYPT, 6f))
                            } else {
                                val distances = state.merchants.map {
                                    calculateDistance(agent.latitude, agent.longitude, it.latitude!!, it.longitude!!)
                                }.sorted()
                                val tenth = distances.getOrNull(9) ?: distances.lastOrNull()
                                val zoom = tenth?.let { zoomForDistanceKm(it / 1000) } ?: 15f
                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(agent.latitude, agent.longitude), zoom))
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.search,
                    onValueChange = viewModel::onSearch,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("بحث بالاسم أو الكود أو الهاتف…", color = Color(0xFFBDBDBD)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = SalesColors.Muted) },
                    shape = RoundedCornerShape(10.dp),
                    colors = salesFieldColors()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BUCKET_LABELS.forEach { (bucket, label) ->
                        val active = bucket in state.buckets
                        Row(
                            modifier = Modifier
                                .background(if (active) BUCKET_COLORS.getValue(bucket).copy(alpha = 0.2f) else Color.White, RoundedCornerShape(50))
                                .clickable { viewModel.toggleBucket(bucket) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier
                                .size(10.dp)
                                .background(BUCKET_COLORS.getValue(bucket), CircleShape))
                            Text(" $label (${state.count(bucket)})", fontSize = 12.sp, fontFamily = ZainBold)
                        }
                    }
                }
            }

            state.selected?.let { merchant ->
                val agent = state.agentLocation
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(12.dp)
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(merchant.displayName, fontSize = 17.sp, fontFamily = ZainBold, modifier = Modifier.weight(1f))
                        FawryBadge(merchant.fawryDeviceNumber, merchant.wantsFawryDevice)
                        IconButton(onClick = { viewModel.select(null) }) { Icon(Icons.Outlined.Close, contentDescription = "إغلاق") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Pill(merchant.merchantCode, SalesColors.Muted)
                        val bucket = merchant.coverageBucket()
                        Pill(
                            when (bucket) {
                                CoverageBucket.UNVISITED -> "لم تُزر بعد"
                                CoverageBucket.ONCE -> "زيارة واحدة"
                                CoverageBucket.COMPLETED -> "زيارتان ✓"
                            },
                            BUCKET_COLORS.getValue(bucket)
                        )
                        Pill(priceTierLabel(merchant.priceTier), SalesColors.Primary)
                    }
                    if (agent != null && merchant.latitude != null && merchant.longitude != null) {
                        Text(formatDistance(calculateDistance(agent.latitude, agent.longitude, merchant.latitude, merchant.longitude)), color = SalesColors.Muted, fontSize = 13.sp)
                    }
                    merchant.phoneNumber?.let { Text(it, fontFamily = ZainRegular, color = SalesColors.Primary) }
                    Row(
                        modifier = Modifier.clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(buildGoogleMapsSearchUrl(merchant.latitude!!, merchant.longitude!!))))
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Map, contentDescription = null, tint = SalesColors.Primary)
                        Text(" افتح في خرائط جوجل", fontFamily = ZainBold, color = SalesColors.Primary)
                    }
                }
            }
        }
    }
}
