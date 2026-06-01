package net.maiatoday.tagspotter.utils

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object LocationHelper {

    data class SpotLocation(
        val latitude: Double,
        val longitude: Double,
        val isFallback: Boolean
    )

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): SpotLocation? {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        // 1. Try to get a high-accuracy current location with a timeout of 4 seconds
        val location = withTimeoutOrNull(4000) {
            suspendCancellableCoroutine<android.location.Location?> { continuation ->
                val cts = CancellationTokenSource()
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cts.token
                ).addOnCompleteListener { task ->
                    if (continuation.isActive) {
                        if (task.isSuccessful) {
                            continuation.resume(task.result)
                        } else {
                            continuation.resume(null)
                        }
                    }
                }
                continuation.invokeOnCancellation { cts.cancel() }
            }
        }

        if (location != null) {
            return SpotLocation(location.latitude, location.longitude, false)
        }

        // 2. Fallback: try to get last known location
        val lastLocation = suspendCancellableCoroutine<android.location.Location?> { continuation ->
            fusedLocationClient.lastLocation.addOnCompleteListener { task ->
                if (continuation.isActive) {
                    if (task.isSuccessful) {
                        continuation.resume(task.result)
                    } else {
                        continuation.resume(null)
                    }
                }
            }
        }

        if (lastLocation != null) {
            return SpotLocation(lastLocation.latitude, lastLocation.longitude, true)
        }

        return null
    }
}
