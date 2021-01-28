package me.minhael.design.android

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import kotlinx.coroutines.coroutineScope
import java.io.Closeable
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class BoundService<T>(
    val api: T,
    private val context: Context,
    private val conn: ServiceConnection
): Closeable {

    override fun close() {
        context.unbindService(conn)
    }

    companion object {

        suspend inline fun <reified T> start(context: Context, service: Class<out Service>, flags: Int = Context.BIND_AUTO_CREATE) = coroutineScope {
            Services.start(context, service)
            bind<T>(context, service, flags)
        }

        suspend inline fun <reified T> startForeground(context: Context, service: Class<out Service>, flags: Int = Context.BIND_AUTO_CREATE) = coroutineScope {
            Services.startForeground(context, service)
            bind<T>(context, service, flags)
        }

        suspend inline fun <reified T> bind(context: Context, service: Class<out Service>, flags: Int = Context.BIND_AUTO_CREATE) = suspendCoroutine<BoundService<T>> { continuation ->
            val conn = object : ServiceConnection {
                override fun onServiceConnected(p0: ComponentName, p1: IBinder) {
                    continuation.resume(BoundService(p1 as T, context, this))
                }

                override fun onServiceDisconnected(p0: ComponentName?) { }
            }
            if (!context.bindService(Intent(context, service), conn, flags))
                throw IllegalStateException("Failed binding to $service")
        }
    }
}