package me.minhael.recorder.service

import android.content.Context
import kotlinx.coroutines.*
import me.minhael.design.android.BoundService
import me.minhael.design.state.Redux
import me.minhael.design.x.getWith
import me.minhael.recorder.Measurable
import me.minhael.recorder.component.RecorderService
import org.slf4j.LoggerFactory

class Measures(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {

    private val rdx = Redux()
    private var job: Job? = null

    fun start(interval: Long) {
        rdx.dispatch { startMeasuring(interval, it) }
    }

    fun stop() {
        rdx.dispatch { stopMeasuring(it) }
    }

    fun observe(handler: (Int) -> Unit): (Any) -> Unit {
        return rdx.observeWith(STATE_MEASURE, handler)
    }

    fun drop(handler: (Any) -> Unit) {
        rdx.remove(STATE_MEASURE, handler)
    }

    private fun startMeasuring(interval: Long, state: Map<String, Any>): Map<String, Any> {
        if (state.getWith(STATE_IS_MEASURING, false))
            return emptyMap()

        logger.debug("Start measure")
        job = scope.launch {
            startMeasure(context, interval) { measure ->
                rdx.setState { mapOf(STATE_MEASURE to measure) }
            }
        }

        return mapOf(STATE_IS_MEASURING to true)
    }

    private fun stopMeasuring(state: Map<String, Any>): Map<String, Any> {
        if (!state.getWith(STATE_IS_MEASURING, false))
            return emptyMap()

        scope.launch {
            logger.debug("Stop measure")
            job?.cancelAndJoin()
        }

        return mapOf(STATE_IS_MEASURING to false)
    }

    private suspend fun startMeasure(context: Context, interval: Long, handler: (Int) -> Unit) = coroutineScope {
        BoundService.startForeground<Measurable>(context, RecorderService::class.java).use { service ->
            //  Get sound level and accumulate
            while (isActive) {
                handler(service.api.soundLevel())
                delay(interval)
            }
        }
    }

    companion object {
        private val STATE_IS_MEASURING = "${Measures::class.java}.state.is_measuring"
        private val STATE_MEASURE = "${Measures::class.java}.state.measure"

        private val logger = LoggerFactory.getLogger(Measures::class.java)
    }
}