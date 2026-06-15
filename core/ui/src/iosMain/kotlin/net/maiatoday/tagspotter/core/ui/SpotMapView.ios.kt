@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package net.maiatoday.tagspotter.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import platform.MapKit.*
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.darwin.NSObject
import platform.UIKit.UIColor
import platform.CoreLocation.CLLocation

@Composable
actual fun SpotMapView(
    latitude: Double,
    longitude: Double,
    zoomLevel: Double,
    markers: List<MapMarker>,
    useDarkMap: Boolean,
    modifier: Modifier,
    radiusCircleCenterLatitude: Double?,
    radiusCircleCenterLongitude: Double?,
    radiusCircleMeters: Double,
    onMapClick: ((Double, Double) -> Unit)?,
    onMapReady: (() -> Unit)?
) {
    val coordinate = CLLocationCoordinate2DMake(latitude, longitude)
    
    // Create and remember the MKMapView
    val mapView = remember {
        MKMapView().apply {
            showsUserLocation = true
        }
    }

    // Set map delegate to handle overlays and custom pin colors/interactions
    val delegate = remember(markers) {
        SpotMapViewDelegate(markers)
    }

    UIKitView(
        factory = {
            mapView.delegate = delegate
            
            // Set initial center region
            val meters = 100000.0 / (zoomLevel * zoomLevel)
            val region = MKCoordinateRegionMakeWithDistance(coordinate, meters, meters)
            mapView.setRegion(region, animated = false)
            
            onMapReady?.invoke()
            mapView
        },
        update = { view ->
            // Clear existing annotations and overlays
            view.removeAnnotations(view.annotations)
            view.removeOverlays(view.overlays)
            
            // Add overlays
            if (radiusCircleCenterLatitude != null && radiusCircleCenterLongitude != null && radiusCircleMeters > 0.0) {
                val circleCenter = CLLocationCoordinate2DMake(radiusCircleCenterLatitude, radiusCircleCenterLongitude)
                val circle = MKCircle.circleWithCenterCoordinate(circleCenter, radiusCircleMeters)
                view.addOverlay(circle)
            }
            
            // Add annotations
            markers.forEach { mapMarker ->
                val annotation = MKPointAnnotation().apply {
                    setCoordinate(CLLocationCoordinate2DMake(mapMarker.latitude, mapMarker.longitude))
                    setTitle(mapMarker.title)
                }
                view.addAnnotation(annotation)
            }
        },
        modifier = modifier
    )
}

private class SpotMapViewDelegate(
    private val markers: List<MapMarker>
) : NSObject(), MKMapViewDelegateProtocol {
    
    override fun mapView(mapView: MKMapView, viewForAnnotation: MKAnnotationProtocol): MKAnnotationView? {
        if (viewForAnnotation === mapView.userLocation) return null
        
        val reuseId = "SpotMarker"
        var annotationView = mapView.dequeueReusableAnnotationViewWithIdentifier(reuseId) as? MKPinAnnotationView
        if (annotationView == null) {
            annotationView = MKPinAnnotationView(viewForAnnotation, reuseId)
            annotationView.canShowCallout = true
        } else {
            annotationView.annotation = viewForAnnotation
        }
        
        // Customize pin color based on marker category
        val marker = markers.find { it.title == viewForAnnotation.title }
        if (marker != null) {
            val colorHex = if (marker.status == "erased") {
                "#808080" // Gray
            } else {
                when (marker.category.lowercase()) {
                    "sticker" -> "#FF007F"
                    "stencil" -> "#39FF14"
                    "poster" -> "#FFFF00"
                    "throwup" -> "#00FFFF"
                    "piece" -> "#FF00FF"
                    "burner" -> "#FF4500"
                    else -> "#9D00FF"
                }
            }
            annotationView.pinTintColor = colorFromHex(colorHex)
        } else {
            annotationView.pinTintColor = UIColor.redColor
        }
        
        return annotationView
    }

    override fun mapView(mapView: MKMapView, didSelectAnnotationView: MKAnnotationView) {
        val annotation = didSelectAnnotationView.annotation ?: return
        val marker = markers.find { it.title == annotation.title }
        marker?.onClick?.invoke()
    }
    
    override fun mapView(mapView: MKMapView, rendererForOverlay: MKOverlayProtocol): MKOverlayRenderer {
        if (rendererForOverlay is MKCircle) {
            return MKCircleRenderer(rendererForOverlay).apply {
                fillColor = colorFromHex("#00FFCC").colorWithAlphaComponent(0.12)
                strokeColor = colorFromHex("#00FFCC")
                lineWidth = 2.0
            }
        }
        return MKOverlayRenderer(rendererForOverlay)
    }
}

private fun colorFromHex(hex: String): UIColor {
    val cleanHex = hex.removePrefix("#")
    val rgb = cleanHex.toInt(16)
    val r = ((rgb shr 16) and 0xFF).toDouble() / 255.0
    val g = ((rgb shr 8) and 0xFF).toDouble() / 255.0
    val b = (rgb and 0xFF).toDouble() / 255.0
    return UIColor.colorWithRed(r, green = g, blue = b, alpha = 1.0)
}
