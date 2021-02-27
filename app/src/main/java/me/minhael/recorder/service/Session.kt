package me.minhael.recorder.service

import me.minhael.design.props.Props
import me.minhael.design.state.Redux
import me.minhael.design.x.getWith
import me.minhael.recorder.PropTags
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

class Session(
    private val props: Props,
    private val reports: Reports,
    private val recording: Recording,
    private val measures: Measures,
    private val schedule: Schedule
) {

    private val rdx = Redux()
    private var handler: ((Any) -> Unit)? = null

    fun start() {
        rdx.dispatch { startSession(it) }
    }

    fun stop() {
        rdx.dispatch { stopSession(it) }
    }

    fun isRecording() = recording.isRecording()
    fun startTime() = rdx.getWith<Long>(STATE_START_TIME)
    fun interval() = rdx.getWith<Long>(STATE_INTERVAL)

    private fun startSession(state: Map<String, Any>): Map<String, Any> {
        if (state.getWith(STATE_ON_AIR, false))
            return emptyMap()

        logger.debug("Start session")

        val now = DateTime.now().millis
        val interval = props.get(PropTags.MEASURE_PERIOD_UPDATE_MS, PropTags.MEASURE_PERIOD_UPDATE_MS_DEFAULT)
        reports.start()
        recording.start()
        measures.start(interval)

        return mapOf(
            STATE_ON_AIR to true,
            STATE_START_TIME to now,
            STATE_INTERVAL to interval
        )
    }

    private fun stopSession(state: Map<String, Any>): Map<String, Any> {
        if (!state.getWith(STATE_ON_AIR, false))
            return emptyMap()

        logger.debug("Stop session")

        schedule.manualStopped()
        measures.stop()
        handler?.also { measures.drop(it) }
        recording.stop()
        reports.stop(
            state.getWith(STATE_START_TIME, System.currentTimeMillis()),
            state.getWith(STATE_INTERVAL, props.get(PropTags.MEASURE_PERIOD_UPDATE_MS, 250L))
        )

         return mapOf(STATE_ON_AIR to false)
    }

    companion object {
        private val STATE_ON_AIR = "${Session::class.java}.state.on_air"
        private val STATE_START_TIME = "${Session::class.java}.state.start_time"
        private val STATE_INTERVAL = "${Session::class.java}.state.interval"

        private val logger = LoggerFactory.getLogger(Session::class.java)
    }
}