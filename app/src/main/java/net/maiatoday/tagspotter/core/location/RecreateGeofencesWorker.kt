package net.maiatoday.tagspotter.core.location

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import net.maiatoday.tagspotter.core.database.SpotRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RecreateGeofencesWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val repository: SpotRepository by inject()
    private val geofenceManager = GeofenceManager(context)

    override suspend fun doWork(): Result {
        return try {
            val starredSpots = repository.getStarredSpots()
            if (starredSpots.isNotEmpty()) {
                geofenceManager.recreateAllGeofences(starredSpots) { success ->
                    Log.d("RecreateGeofencesWorker", "Re-registered ${starredSpots.size} geofences on boot: success=$success")
                }
            } else {
                geofenceManager.recreateAllGeofences(emptyList()) { success ->
                    Log.d("RecreateGeofencesWorker", "Cleared geofences as there are no starred spots: success=$success")
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("RecreateGeofencesWorker", "Error recreating geofences", e)
            Result.failure()
        }
    }
}
