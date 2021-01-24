package me.minhael.recorder.service

import android.content.Context
import me.minhael.design.android.Services
import me.minhael.design.fs.Uri
import me.minhael.recorder.Recorder
import me.minhael.recorder.component.RecorderService
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat

class Recording(
    private val context: Context,
    private val resolver: Uri.Resolver,
    private val storage: Storage,
    private val schedule: Schedule
) {

    private var output: String? = null

    fun start() {
        Services.startForeground<Recorder>(context, RecorderService::class.java) { recorder ->
            if (!recorder.isRecording())
                startRecording(recorder)
        }
    }

    fun stop() {
        Services.startForeground<Recorder>(context, RecorderService::class.java) { recorder ->
            stopAndReport(recorder)
        }
    }

    fun toggle(onToggle: (Boolean) -> Unit) {
        Services.start<Recorder>(context, RecorderService::class.java) { recorder ->
            val isRecording = recorder.isRecording()
            if (isRecording)
                stopAndReport(recorder)
            else
                startRecording(recorder)
            onToggle(!isRecording)
        }
    }

    private fun startRecording(recorder: Recorder) {
        stopAndReport(recorder)
        output = recorder.record(storage.dirCache, "Record@${FORMAT_TIME.print(LocalDateTime())}")
    }

    private fun stopAndReport(recorder: Recorder) {
        if (recorder.isRecording()) {
            schedule.manualStop()
            recorder.stop()
            recorder.close()
            output?.apply { export(this) }
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