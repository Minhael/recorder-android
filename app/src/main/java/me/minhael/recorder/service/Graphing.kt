package me.minhael.recorder.service

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.minhael.design.state.Redux
import me.minhael.design.x.getWith
import org.slf4j.LoggerFactory
import kotlin.math.max
import kotlin.math.min

class Graphing(
    private val measures: Measures,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {

    private val rdx = Redux()
    private var handler: ((Any) -> Unit)? = null

    fun start(observer: (Levels) -> Unit) {
        handler = measures.observe { measure ->
            if (measure > 0) {
                rdx
                    .dispatch { dispatchMeasure(measure, it) }
                    .let {
                        Levels(
                            rdx.getWith(STATE_MEASURES) ?: measure,
                            rdx.getWith(STATE_AVERAGE) ?: measure,
                            rdx.getWith(STATE_MAX) ?: measure
                        )
                    }
                    .also {
                        scope.launch(dispatcher) { observer(it) }
                    }
            }
        }
    }

    fun stop() {
        handler?.also { measures.drop(it) }
    }

    fun getLevels(): Levels {
        return Levels(
            rdx.getWith(STATE_MEASURES) ?: 0,
            rdx.getWith(STATE_AVERAGE) ?: 0,
            rdx.getWith(STATE_MAX) ?: 0
        )
    }

    private fun dispatchMeasure(measure: Int, state: Map<String, Any>): Map<String, Any> {
        val average = state.getWith(STATE_AVERAGE, measure)
        val max = state.getWith(STATE_MAX, measure)
        logger.trace("Average = {} Max = {} Measure = {}", average, max, measure)

        val count = min(RATIO, state.getWith(STATE_COUNT, 0) + 1)

        return mapOf(
            STATE_MEASURES to measure,
            STATE_AVERAGE to ((count - 1) / count.toDouble() * average + measure / count.toDouble()).toInt(),
            STATE_MAX to max(max, measure),
            STATE_COUNT to count
        )
    }

    data class Levels(
        val measure: Int,
        val average: Int,
        val max: Int
    )

    companion object {
        private val STATE_MEASURES = "${Graphing::class.java}.state.measures"
        private val STATE_AVERAGE = "${Graphing::class.java}.state.average"
        private val STATE_MAX = "${Graphing::class.java}.state.max"
        private val STATE_COUNT = "${Graphing::class.java}.state.count"

        private val logger = LoggerFactory.getLogger(Graphing::class.java)

        private const val RATIO = 3
    }
}