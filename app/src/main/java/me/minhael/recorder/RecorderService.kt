package me.minhael.recorder

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import me.minhael.design.Uri
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat
import org.koin.android.ext.android.inject
import java.io.File

class RecorderService : Service() {

    private val resolver: Uri.Resolver by inject()
    private val storage: Storage by inject()

    private var recorder: Recorder? = null
    private var output: String? = null

    override fun onCreate() {
        super.onCreate()

        startForeground()
        startRecording()
    }

    override fun onDestroy() {
        stopRecording()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_QUIT -> {
                startForeground(ONGOING_NOTIFICATION_ID, buildNotification(R.string.msg_saving))
                export { stopService() }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(p0: Intent): IBinder {
        TODO("Not yet implemented")
    }

    private fun buildNotification(@StringRes contentRes: Int = R.string.msg_recording): Notification {
        val intent = Intent(applicationContext, RecorderService::class.java).apply {
            action = ACTION_QUIT
        }
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
            val channel = NotificationChannel(CHANNEL_RECORDER, name, NotificationManager.IMPORTANCE_DEFAULT)
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

// Notification ID cannot be 0.
        startForeground(ONGOING_NOTIFICATION_ID, buildNotification())
    }

    private fun stopService() {
        NotificationManagerCompat.from(baseContext).cancel(ONGOING_NOTIFICATION_ID)
        stopForeground(true)
        stopSelf()
    }

    private fun startRecording() {
        Permissions.requestRecording(baseContext) { isGranted ->
            if (isGranted) {
                recorder = Recorder(storage.dirCache)
                output = recorder?.record("Record@${FORMAT_TIME.print(LocalDateTime())}")
            } else {
                stopService()
            }
        }
    }

    private fun stopRecording() {
        recorder?.apply {
            stop()
            close()
        }
    }

    private fun export(onFinish:() -> Unit) {
        output?.apply {
            resolver.readFrom(this).use {
                storage.dirPublic.copy(it,
                    "audio/amr",
                    android.net.Uri.parse(this).lastPathSegment ?: "Record<${FORMAT_TIME.print(LocalDateTime())}.amr"
                )
            }
            storage.dirCache.delete(this)
        }
        onFinish()
    }

    companion object {
        private val FORMAT_TIME = DateTimeFormat.forPattern("yyyyMMddHHmmss")

        private val ACTION_QUIT = "${RecorderService::class.java}.action_quit"

        private const val REQ_CODE_CLICK = 1

        private const val ONGOING_NOTIFICATION_ID = 1

        private val CHANNEL_RECORDER = "${RecorderService::class.java}.channel"
    }
}