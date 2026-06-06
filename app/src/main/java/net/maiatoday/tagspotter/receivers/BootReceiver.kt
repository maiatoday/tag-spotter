package net.maiatoday.tagspotter.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import net.maiatoday.tagspotter.workers.RecreateGeofencesWorker

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val workRequest = OneTimeWorkRequestBuilder<RecreateGeofencesWorker>().build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}
