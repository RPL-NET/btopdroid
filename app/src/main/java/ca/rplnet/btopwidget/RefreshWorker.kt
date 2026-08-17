package ca.rplnet.btopwidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit

class RefreshWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)

        val btopIds = appWidgetManager.getAppWidgetIds(
            ComponentName(applicationContext, BtopWidgetProvider::class.java)
        )
        for (id in btopIds) {
            BtopWidgetProvider.updateWidget(applicationContext, appWidgetManager, id)
        }

        val termuxIds = appWidgetManager.getAppWidgetIds(
            ComponentName(applicationContext, TermuxWidgetProvider::class.java)
        )
        for (id in termuxIds) {
            TermuxWidgetProvider.updateWidget(applicationContext, appWidgetManager, id)
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "btop-widget-refresh"

        // 15 minutes = le minimum permis par WorkManager pour du periodique
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<RefreshWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
