package me.minhael.recorder

import android.app.Activity
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import me.minhael.android.Services
import me.minhael.design.fs.Uri
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat
import org.koin.core.component.KoinApiExtension
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RecordController(
    private val resolver: Uri.Resolver,
    private val storage: Storage
) {

    private var output: String? = null

    fun toggle(activity: Activity, onToggle: (Boolean) -> Unit) {
        Services.start<Recorder>(activity, RecorderService::class.java) { recorder ->
            val isRecording = recorder.isRecording()
            if (isRecording) {
                recorder.stop()
                recorder.close()
                output?.apply { export(this) }
            } else {
                output = recorder.record(storage.dirCache, "Record@${FORMAT_TIME.print(LocalDateTime())}")
            }
            onToggle(!isRecording)
        }
    }

    private fun export(uri: String) {
        resolver.readFrom(uri).use {
            storage.dirPublic.copy(
                it,
                "audio/amr",
                android.net.Uri.parse(uri).lastPathSegment ?: "Record<${FORMAT_TIME.print(LocalDateTime())}.amr"
            )
        }
        storage.dirCache.delete(uri)
    }

    companion object {
        private val FORMAT_TIME = DateTimeFormat.forPattern("yyyyMMddHHmmss")
    }
}