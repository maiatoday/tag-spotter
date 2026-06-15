@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package net.maiatoday.tagspotter.core.location

import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.CoreLocation.CLLocation
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlinx.cinterop.useContents
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class IosLocationProvider : LocationProvider {
    private val locationManager = CLLocationManager()

    override suspend fun getCurrentLocation(): LocationData? {
        val status = CLLocationManager.authorizationStatus()
        
        if (status == kCLAuthorizationStatusDenied || status == kCLAuthorizationStatusRestricted) {
            return null
        }
        
        if (status == kCLAuthorizationStatusNotDetermined) {
            locationManager.requestWhenInUseAuthorization()
        }
        
        // Try getting last known location first
        val lastLocation = locationManager.location
        if (lastLocation != null) {
            val coordinate = lastLocation.coordinate
            return LocationData(
                latitude = coordinate.useContents { latitude },
                longitude = coordinate.useContents { longitude },
                isFallback = false
            )
        }
        
        // If not immediately available, suspend and wait for one location update
        return suspendCancellableCoroutine { continuation ->
            val delegate = IosLocationManagerDelegate(continuation)
            
            locationManager.delegate = delegate
            continuation.invokeOnCancellation {
                locationManager.stopUpdatingLocation()
                locationManager.delegate = null
            }
            
            if (status == kCLAuthorizationStatusAuthorizedWhenInUse || 
                status == kCLAuthorizationStatusAuthorizedAlways) {
                locationManager.startUpdatingLocation()
            }
        }
    }
}

private class IosLocationManagerDelegate(
    private val continuation: kotlinx.coroutines.CancellableContinuation<LocationData?>
) : NSObject(), CLLocationManagerDelegateProtocol {
    
    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val location = didUpdateLocations.firstOrNull() as? CLLocation
        if (location != null) {
            val coordinate = location.coordinate
            val data = coordinate.useContents {
                LocationData(
                    latitude = latitude,
                    longitude = longitude,
                    isFallback = false
                )
            }
            if (continuation.isActive) {
                continuation.resume(data)
            }
        }
        manager.stopUpdatingLocation()
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        if (continuation.isActive) {
            continuation.resume(null)
        }
        manager.stopUpdatingLocation()
    }

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        val newStatus = CLLocationManager.authorizationStatus()
        if (newStatus == kCLAuthorizationStatusAuthorizedWhenInUse || 
            newStatus == kCLAuthorizationStatusAuthorizedAlways) {
            manager.startUpdatingLocation()
        } else if (newStatus == kCLAuthorizationStatusDenied || 
                   newStatus == kCLAuthorizationStatusRestricted) {
            if (continuation.isActive) {
                continuation.resume(null)
            }
        }
    }
}
