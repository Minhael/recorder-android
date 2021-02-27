package me.minhael.recorder.service

import android.content.Context
import kotlinx.coroutines.*
import me.minhael.design.android.BoundService
import me.minhael.design.fs.Uri
import me.minhael.design.state.Redux
import me.minhael.design.x.getWith
import me.minhael.recorder.Recorder
import me.minhael.recorder.component.RecorderService
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.slf4j.LoggerFactory

class Recording(
    private val context: Context,
    private val storage: Storage,
    private val resolver: Uri.Resolver,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {

    private val rdx = Redux()

    fun start(filename: String = FORMAT_TIME.print(DateTime.now())) {
        rdx.dispatch { startRecording(context, filename, it) }
    }

    fun stop() {
        rdx.dispatch { stopRecording(context, it) }
    }

    fun isRecording() = rdx.getWith<Boolean>(STATE_IS_RECORDING) ?: false

    private fun startRecording(context: Context, filename: String, state: Map<String, Any>): Map<String, Any> {
        if (state.getWith(STATE_IS_RECORDING, false))
            return emptyMap()

        scope.launch {
            BoundService.startForeground<Recorder>(context, RecorderService::class.java).use { service ->
                val recorder = service.api

                rdx.dispatch {
                    if (it[STATE_IS_RECORDING] == true && !recorder.isRecording()) {
                        logger.info("Start record")
                        val uri = recorder.record(storage.dirCache, filename)
                        mapOf(STATE_URI to uri)
                    } else {
                        emptyMap()
                    }
                }
            }
        }

        return mapOf(STATE_IS_RECORDING to true)
    }

    private fun stopRecording(context: Context, state: Map<String, Any>): Map<String, Any> {
        if (!state.getWith(STATE_IS_RECORDING, false))
            return emptyMap()

        scope.launch {
            BoundService.startForeground<Recorder>(context, RecorderService::class.java).use { service ->
                val recorder = service.api

                val next = rdx.dispatch { prev ->
                    if (!prev.getWith(STATE_IS_RECORDING, false) && recorder.isRecording()) {
                        logger.info("Stop record")

                        //  Stop recording
                        recorder.use { it.stop() }
                    }
                    emptyMap()
                }

                if (next[STATE_IS_RECORDING] != true && !recorder.isRecording() && next.containsKey(STATE_URI)) {
                    val cache = next.getWith(STATE_URI, "")

                    //  Copy the audio file & delete cache
                    withContext(dispatcher) {
                        storage.dirCache.peek(cache)?.apply {
                            resolver.readFrom(uri).use { storage.dirPublic.copy(it, mimeType, filename) }
                            storage.dirCache.delete(uri)
                        }
                    }
                }
            }
        }

        return mapOf(STATE_IS_RECORDING to false)
    }

    companion object {
        private val FORMAT_TIME = DateTimeFormat.forPattern("yyyy-MM-dd HH-mm-ss")

        private val STATE_IS_RECORDING = "${Recording::class.java}.state.is_recording"
        private val STATE_URI = "${Recording::class.java}.state.uri"

        private val logger = LoggerFactory.getLogger(Recording::class.java)
    }
}