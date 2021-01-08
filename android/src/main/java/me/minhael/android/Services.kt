package me.minhael.android

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import java.lang.IllegalStateException

object Services {

    inline fun <reified T> start(context: Context, service: Class<out Service>, crossinline onBind: (T) -> Unit) {
        val intent = Intent(context, service)
        if (context.startService(intent) == null)
            throw IllegalStateException("Failed to start $service")

        val connection = object : ServiceConnection {
            override fun onServiceConnected(p0: ComponentName, p1: IBinder) {
                try {
                    onBind(p1 as T)
                } finally {
                    context.unbindService(this)
                }
            }

            override fun onServiceDisconnected(p0: ComponentName) { }
        }
        if (!context.bindService(Intent(context, service), connection, Context.BIND_AUTO_CREATE))
            throw IllegalStateException("Failed to bind service")
    }
}