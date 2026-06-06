package net.maiatoday.tagspotter.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import net.maiatoday.tagspotter.MainActivity
import net.maiatoday.tagspotter.TagSpotterApplication
import net.maiatoday.tagspotter.utils.PackManager
import java.io.File
import java.io.FileInputStream

class ImportPackWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val tempFilePath = inputData.getString(KEY_TEMP_FILE_PATH)
        if (tempFilePath.isNullOrEmpty()) {
            Log.e(TAG, "No temporary file path provided")
            return Result.failure()
        }

        val tempFile = File(tempFilePath)
        if (!tempFile.exists()) {
            Log.e(TAG, "Temporary file does not exist: $tempFilePath")
            return Result.failure()
        }

        val app = applicationContext as TagSpotterApplication
        val repository = app.repository
        val settingsRepository = app.settingsRepository

        return try {
            val currentPhotographer = settingsRepository.photographerName.first()
            val importedCount = withContext(Dispatchers.IO) {
                FileInputStream(tempFile).use { inputStream ->
                    PackManager.importPack(
                        context = applicationContext,
                        repository = repository,
                        inputStream = inputStream,
                        currentPhotographerName = currentPhotographer
                    )
                }
            }
            Log.i(TAG, "Successfully imported $importedCount spots")
            showImportNotification(
                context = applicationContext,
                title = "Import Successful",
                message = "Imported $importedCount spots!",
                isSuccess = true
            )
            Result.success(workDataOf(KEY_IMPORTED_COUNT to importedCount))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import pack", e)
            showImportNotification(
                context = applicationContext,
                title = "Import Failed",
                message = "Failed to import: ${e.localizedMessage}",
                isSuccess = false
            )
            Result.failure(workDataOf(KEY_ERROR_MESSAGE to (e.localizedMessage ?: "Unknown error")))
        } finally {
            // Always clean up the temp file
            if (tempFile.exists()) {
                try {
                    tempFile.delete()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete temporary file", e)
                }
            }
        }
    }

    private fun showImportNotification(
        context: Context,
        title: String,
        message: String,
        isSuccess: Boolean
    ) {
        val channelId = "import_status_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "Import Status Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifies you when a spot pack import finishes."
        }
        notificationManager.createNotificationChannel(channel)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val iconRes = if (isSuccess) {
            android.R.drawable.ic_menu_save
        } else {
            android.R.drawable.ic_dialog_alert
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        notificationManager.notify(1002, builder.build())
    }

    companion object {
        private const val TAG = "ImportPackWorker"
        const val KEY_TEMP_FILE_PATH = "key_temp_file_path"
        const val KEY_IMPORTED_COUNT = "key_imported_count"
        const val KEY_ERROR_MESSAGE = "key_error_message"
    }
}
