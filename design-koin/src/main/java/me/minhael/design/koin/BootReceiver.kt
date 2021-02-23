package me.minhael.design.koin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.*
import org.koin.core.component.KoinApiExtension
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@KoinApiExtension
internal class BootReceiver: BroadcastReceiver() {

    private val receiver: KoinReceiver by lazy { KoinReceiver() }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            receiver.onReceive()
        }
    }

    class KoinReceiver: KoinComponent {

        private val scheduler: AndroidScheduler by inject()

        fun onReceive() {
            scheduler.purgeBootJobs()
        }
    }
}