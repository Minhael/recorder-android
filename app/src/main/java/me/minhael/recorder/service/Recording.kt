package me.minhael.recorder.service

import android.content.Context
import kotlinx.coroutines.*
import me.minhael.design.android.BoundService
import me.minhael.design.props.Props
import me.minhael.recorder.Measurable
import me.minhael.recorder.PropTags
import me.minhael.recorder.Recorder
import me.minhael.recorder.component.RecorderService
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.slf4j.LoggerFactory
import kotlin.math.max

class Recording(
    private val context: Context,
    private val storage: Storage,
    private val props: Props,
    private val schedule: Schedule,
    private val exporter: Exporter
) {

    var state = State()
        private set

    private val scope = CoroutineScope(Dispatchers.Default)

    private var job: Job? = null

    suspend fun start(): State {
        return changeState { startRecording(it) }
    }

    suspend fun stop(): State {
        return changeState { stopRecording(it) }
    }

    private suspend fun changeState(reducer: suspend (State) -> State): State {
        return reducer(state).also { state = it }
    }

    private suspend fun startRecording(state: State): State {
        if (state.isRecording)
            return state

        return BoundService.startForeground<Recorder>(context, RecorderService::class.java).use { service ->
            val recorder = service.api

            logger.info("Start record")

            val periodMs = props.get(PropTags.MEASURE_PERIOD_UPDATE_MS, PropTags.MEASURE_PERIOD_UPDATE_MS_DEFAULT)
            val now = DateTime()
            val uri = recorder.record(storage.dirCache, FORMAT_TIME.print(now))
            val nextState = State(true, uri, now.millis, periodMs)

            job = scope.launch {
                BoundService.startForeground<Measurable>(context, RecorderService::class.java).use { service ->

                    //  Initiate sound level
                    service.api.soundLevel().also {
                        nextState.levels.apply {
                            average = it
                            max = it
                        }
                    }

                    //  Get sound level and accumulate
                    launch(Dispatchers.IO) {
                        while (isActive) {
                            val level = service.api.soundLevel()
                            nextState.measures.add(level)
                            nextState.levels.apply {
                                average = (average + level) / 2
                                max = max(max, level)
                            }
                            delay(periodMs)
                        }
                    }
                }
            }

            nextState
        }
    }

    private suspend fun stopRecording(state: State): State {
        if (!state.isRecording)
            return state

        return BoundService.startForeground<Recorder>(context, RecorderService::class.java).use { service ->
            val recorder = service.api

            logger.info("Stop record")

            schedule.manualStopped()
            recorder.use { it.stop() }

            scope.launch {
                val cache = state.uri
                val startTime = state.startTime
                val interval = state.interval
                val measures = state.measures

                if (cache != null && startTime != null && interval != null) {
                    val report = Exporter.Report(startTime, interval, measures)
                    val uri = exporter.saveAsync(cache, report)
                }

                //  Remove cache
                cache?.also { storage.dirCache.delete(it) }
            }

            job?.cancel()

            state.copy(isRecording = false)
        }
    }

    data class State(
        val isRecording: Boolean = false,
        val uri: String? = null,
        val startTime: Long? = null,
        val interval: Long? = null,
        val measures: MutableList<Int> = mutableListOf(),
        val levels: Levels = Levels()
    )

    data class Levels(
        var average: Int = 0,
        var max: Int = 0
    )

    companion object {
        private val FORMAT_TIME = DateTimeFormat.forPattern("yyyyMMddHHmmss")

        private val logger = LoggerFactory.getLogger(Recording::class.java)
    }
}