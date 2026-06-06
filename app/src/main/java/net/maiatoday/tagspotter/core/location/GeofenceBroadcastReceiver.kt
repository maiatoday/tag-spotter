package net.maiatoday.tagspotter.core.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null || geofencingEvent.hasError()) {
            Log.e("GeofenceReceiver", "GeofencingEvent has error or is null")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            val triggeringLocation = geofencingEvent.triggeringLocation

            if (!triggeringGeofences.isNullOrEmpty()) {
                val spotIds = triggeringGeofences.map { it.requestId }.toTypedArray<String?>()
                
                val dataBuilder = Data.Builder()
                    .putStringArray("spot_ids", spotIds)
                
                triggeringLocation?.let {
                    dataBuilder.putDouble("latitude", it.latitude)
                    dataBuilder.putDouble("longitude", it.longitude)
                }

                val workRequest = OneTimeWorkRequestBuilder<GeofenceTransitionWorker>()
                    .setInputData(dataBuilder.build())
                    .build()

                WorkManager.getInstance(context).enqueue(workRequest)
            }
        }
    }
}
