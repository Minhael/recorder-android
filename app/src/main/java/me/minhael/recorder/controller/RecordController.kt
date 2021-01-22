package me.minhael.recorder.controller

import android.app.Activity
import me.minhael.design.android.Services
import me.minhael.design.fs.Uri
import me.minhael.recorder.Recorder
import me.minhael.recorder.component.RecorderService
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat

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