package me.minhael.recorder

import android.app.*
import android.content.Intent
import android.os.Binder
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.koin.android.ext.android.inject

class RecorderService : Service() {

    private val recorder: FsRecorder by inject()

    private val binder = RecorderBinder()

    override fun onDestroy() {
        recorder.close()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_NOTIFICATION -> startActivity(
                Intent(applicationContext, RecordActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
        return START_NOT_STICKY
    }

    override fun onBind(p0: Intent) = binder

    private fun buildNotification(@StringRes contentRes: Int = R.string.msg_recording): Notification {
        val intent = Intent(applicationContext, RecorderService::class.java)
            .apply { action = ACTION_NOTIFICATION }
        val pendingIntent = PendingIntent.getService(
            baseContext, REQ_CODE_CLICK, intent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat
            .Builder(this, CHANNEL_RECORDER)
            .setContentTitle(getText(R.string.app_name))
            .setContentText(getText(contentRes))
            .setSmallIcon(R.drawable.ic_baseline_settings_voice_24)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun startForeground() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = getString(R.string.app_name)
            val channel =
                NotificationChannel(CHANNEL_RECORDER, name, NotificationManager.IMPORTANCE_DEFAULT)
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

// Notification ID cannot be 0.
        startForeground(ONGOING_NOTIFICATION_ID, buildNotification())
    }

    private fun stopForeground() {
        NotificationManagerCompat.from(baseContext).cancel(ONGOING_NOTIFICATION_ID)
        stopForeground(true)
    }

    inner class RecorderBinder : Recorder, Binder() {

        override fun record(filename: String): String {
            stop()
            startForeground()
            return recorder.record(filename)
        }

        override fun stop() {
            if (recorder.isRecording()) {
                recorder.stop()
                startForeground(ONGOING_NOTIFICATION_ID, buildNotification(R.string.msg_saving))
                stopForeground()
            }
        }

        override fun isRecording(): Boolean {
            return recorder.isRecording()
        }

        override fun close() {
            stopSelf()
        }
    }

    companion object {

        private val CHANNEL_RECORDER = "${RecorderService::class.java}.channel"
        private val ACTION_NOTIFICATION = "${RecorderService::class.java}.action_notification"
        private const val REQ_CODE_CLICK = 1
        private const val ONGOING_NOTIFICATION_ID = 1
    }
}