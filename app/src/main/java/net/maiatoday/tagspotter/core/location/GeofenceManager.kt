package net.maiatoday.tagspotter.core.location

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import net.maiatoday.tagspotter.core.model.Spot

class GeofenceManager(private val context: Context) {
    private val geofencingClient = LocationServices.getGeofencingClient(context)

    fun registerGeofence(spot: Spot, onResult: (Boolean) -> Unit = {}) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            onResult(false)
            return
        }

        val geofence = Geofence.Builder()
            .setRequestId(spot.id.toString())
            .setCircularRegion(spot.latitude, spot.longitude, 100f) // 100 meters
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(request, getGeofencePendingIntent())
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun unregisterGeofence(spotId: Long) {
        geofencingClient.removeGeofences(listOf(spotId.toString()))
    }

    fun recreateAllGeofences(spots: List<Spot>, onResult: (Boolean) -> Unit = {}) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            onResult(false)
            return
        }

        if (spots.isEmpty()) {
            geofencingClient.removeGeofences(getGeofencePendingIntent())
            onResult(true)
            return
        }

        val geofences = spots.map { spot ->
            Geofence.Builder()
                .setRequestId(spot.id.toString())
                .setCircularRegion(spot.latitude, spot.longitude, 100f)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
        }

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()

        geofencingClient.removeGeofences(getGeofencePendingIntent()).addOnCompleteListener {
            geofencingClient.addGeofences(request, getGeofencePendingIntent())
                .addOnSuccessListener { onResult(true) }
                .addOnFailureListener { onResult(false) }
        }
    }

    private fun getGeofencePendingIntent(): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }
}