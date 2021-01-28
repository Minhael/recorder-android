package me.minhael.recorder.service

import android.content.Context
import kotlinx.coroutines.*
import me.minhael.design.android.BoundService
import me.minhael.design.android.Services
import me.minhael.design.fs.Uri
import me.minhael.design.props.Props
import me.minhael.design.sl.JacksonSerializer
import me.minhael.design.sl.Serializer
import me.minhael.recorder.Measurable
import me.minhael.recorder.PropTags
import me.minhael.recorder.Recorder
import me.minhael.recorder.component.RecorderService
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import kotlin.math.max

class Recording(
    private val context: Context,
    private val resolver: Uri.Resolver,
    private val storage: Storage,
    private val schedule: Schedule,
    private val props: Props,
    private val serializer: Serializer
) {

    val levels = Levels()

    private val scope = CoroutineScope(Dispatchers.Default)

    private var output: Session? = null
    private var job: Job? = null

    fun start() {
        Services.startForeground<Recorder>(context, RecorderService::class.java) { recorder ->
            if (!recorder.isRecording())
                startRecording(recorder)
        }
    }

    fun stop() {
        Services.startForeground<Recorder>(context, RecorderService::class.java) { recorder ->
            stopRecording(recorder)
        }
    }

    fun save() {
        Services.startForeground<Recorder>(context, RecorderService::class.java) { recorder ->
            stopRecording(recorder)
            output?.apply { save(this) }
            output = null
        }
    }

    fun toggle(onToggle: (Boolean) -> Unit) {
        Services.start<Recorder>(context, RecorderService::class.java) { recorder ->
            val isRecording = recorder.isRecording()
            if (isRecording) {
                stopRecording(recorder)
                output?.apply { save(this) }
                output = null
            } else
                startRecording(recorder)
            onToggle(!isRecording)
        }
    }

    private fun startRecording(recorder: Recorder) {
        stopRecording(recorder)

        val session = Session(recorder.record(storage.dirCache, "Record@${FORMAT_TIME.print(DateTime())}"))

        job = scope.launch {
            val delayMs = props.get(PropTags.MEASURE_PERIOD_UPDATE_MS, PropTags.MEASURE_PERIOD_UPDATE_MS_DEFAULT)
            BoundService.startForeground<Measurable>(context, RecorderService::class.java).use { service ->

                //  Delay 2 seconds to avoid sound produced by phone vibration
                delay(2000)
                service.api.soundLevel()
                delay(delayMs)

                //  Initiate sound level
                service.api.soundLevel().also {
                    levels.apply {
                        measure = it
                        average = it
                        max = it
                    }
                }

                //  Get sound level and accumulate
                while (isActive) {
                    val level = service.api.soundLevel()
                    session.levels.add(level)
                    levels.apply {
                        measure = level
                        average = (average + level) / 2
                        max = max(max, level)
                    }
                    delay(delayMs)
                }
            }
        }

        output?.uri?.also { storage.dirCache.delete(it) }
        output = session
    }

    private fun stopRecording(recorder: Recorder) {
        if (recorder.isRecording()) {
            schedule.manualStop()
            recorder.stop()
            job?.cancel()
            recorder.close()
        }
    }

    private fun save(session: Session) {
        val filename = "Record@${FORMAT_TIME.print(session.startTime)}"

        //  Copy the audio file
        scope.launch {
            launch(Dispatchers.IO) {
                resolver.readFrom(session.uri).use {
                    storage.dirPublic.copy(
                        it,
                        "audio/amr",
                        android.net.Uri.parse(session.uri).lastPathSegment ?: filename
                    )
                }
            }
            storage.dirCache.delete(session.uri)
        }

        //  Write report
        scope.launch {
            launch(Dispatchers.IO) {
                resolver.writeTo(storage.dirPublic.create("application/json", filename)).use {
                    serializer.serialize(session, it)
                }
            }
        }
    }

    data class Pulse(
        val offset: Long,
        val duration: Long,
    )

    data class Levels(
        var measure: Int = 0,
        var average: Int = 0,
        var max: Int = 0
    )

    private data class Session(
        val uri: String,
        val startTime: Long = System.currentTimeMillis(),
        val levels: MutableList<Int> = mutableListOf(),
        val pulses: MutableList<Pulse> = mutableListOf()
    )

    companion object {
        private val FORMAT_TIME = DateTimeFormat.forPattern("yyyyMMddHHmmss")
    }
}