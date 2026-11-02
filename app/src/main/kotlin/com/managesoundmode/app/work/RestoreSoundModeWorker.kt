package com.managesoundmode.app.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.managesoundmode.app.ManageSoundApplication
import java.util.concurrent.TimeUnit

class RestoreSoundModeWorker(
  appContext: Context,
  workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    val application = applicationContext as? ManageSoundApplication ?: return Result.failure()
    return application.container.soundModeRepository
      .cancelOrRestoreTimedSession()
      .fold(
        onSuccess = {
          Log.i(TAG, "Modo de som anterior restaurado com sucesso pelo timer.")
          Result.success()
        },
        onFailure = { error ->
          Log.e(TAG, "Falha ao restaurar modo de som no RestoreSoundModeWorker", error)
          Result.failure()
        },
      )
  }

  companion object {
    private const val TAG = "RestoreSoundModeWorker"
    private const val UNIQUE_WORK_NAME = "manage_sound_mode_auto_restore"

    fun schedule(context: Context, durationMinutes: Int) {
      val request = OneTimeWorkRequestBuilder<RestoreSoundModeWorker>()
        .setInitialDelay(durationMinutes.toLong().coerceAtLeast(1L), TimeUnit.MINUTES)
        .build()

      WorkManager.getInstance(context).enqueueUniqueWork(
        UNIQUE_WORK_NAME,
        ExistingWorkPolicy.REPLACE,
        request,
      )
    }

    fun cancel(context: Context) {
      WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }
  }
}
