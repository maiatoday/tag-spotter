package net.maiatoday.tagspotter.core.location

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@JsName("navigator")
external val myNavigator: Navigator

external interface Navigator : JsAny {
    val geolocation: Geolocation
}

external interface Geolocation : JsAny {
    fun getCurrentPosition(
        successCallback: (position: Position) -> Unit,
        errorCallback: ((PositionError) -> Unit)? = definedExternally,
        options: PositionOptions? = definedExternally
    )
}

external interface Position : JsAny {
    val coords: Coordinates
    val timestamp: Double
}

external interface Coordinates : JsAny {
    val latitude: Double
    val longitude: Double
}

external interface PositionError : JsAny {
    val message: String
}

external interface PositionOptions : JsAny {
    var enableHighAccuracy: Boolean?
    var timeout: Int?
    var maximumAge: Int?
}

class WasmLocationProvider : LocationProvider {
    override suspend fun getCurrentLocation(): LocationData? {
        return suspendCancellableCoroutine { continuation ->
            try {
                myNavigator.geolocation.getCurrentPosition(
                    successCallback = { position ->
                        if (continuation.isActive) {
                            continuation.resume(
                                LocationData(
                                    latitude = position.coords.latitude,
                                    longitude = position.coords.longitude,
                                    isFallback = false
                                )
                            )
                        }
                    },
                    errorCallback = { error ->
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                )
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
        }
    }
}
