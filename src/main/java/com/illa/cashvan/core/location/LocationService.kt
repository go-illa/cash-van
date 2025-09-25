package com.illa.cashvan.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume

data class LocationData(
    val latitude: Double,
    val longitude: Double
)

class LocationService(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Result<LocationData> {
        return try {
            val location = suspendCancellableCoroutine { continuation ->
                val cancellationTokenSource = CancellationTokenSource()

                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val locationData = LocationData(
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                        continuation.resume(locationData)
                    } else {
                        continuation.resume(null)
                    }
                }.addOnFailureListener { exception ->
                    continuation.resume(null)
                }

                continuation.invokeOnCancellation {
                    cancellationTokenSource.cancel()
                }
            }

            if (location != null) {
                Result.success(location)
            } else {
                Result.failure(Exception("Unable to get location"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): Result<LocationData> {
        return try {
            val location = fusedLocationClient.lastLocation.await()
            if (location != null) {
                val locationData = LocationData(
                    latitude = location.latitude,
                    longitude = location.longitude
                )
                Result.success(locationData)
            } else {
                Result.failure(Exception("No last known location available"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}