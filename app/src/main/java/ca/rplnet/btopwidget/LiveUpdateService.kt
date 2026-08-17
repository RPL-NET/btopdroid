package ca.rplnet.btopwidget

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper

// Contourne la limite OS de updatePeriodMillis (minimum 30min pour un
// widget) en gardant le process actif via un foreground service, et en
// appelant updateAppWidget() nous-meme sur une boucle courte. C'est la
// meme technique que KWGT utilise pour ses widgets animes (horloge avec
// secondes qui bougent, etc). Cout: notification persistante obligatoire
// + plus de batterie consommee tant que le service tourne.
class LiveUpdateService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var running = false

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
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

            handler.postDelayed(this, INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildNotification())
        running = true
        handler.post(tick)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID, "btopdroid refresh temps reel", NotificationManager.IMPORTANCE_MIN
        )
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("btopdroid")
            .setContentText("refresh en direct actif")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "btopdroid_live"
        private const val NOTIF_ID = 1
        private const val INTERVAL_MS = 2000L

        fun start(context: Context) {
            Prefs.setLiveUpdateEnabled(context, true)
            val intent = Intent(context, LiveUpdateService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            Prefs.setLiveUpdateEnabled(context, false)
            context.stopService(Intent(context, LiveUpdateService::class.java))
        }
    }
}
