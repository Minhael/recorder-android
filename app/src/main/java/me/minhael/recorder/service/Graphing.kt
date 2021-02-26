package me.minhael.recorder.service

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.minhael.design.state.Redux
import me.minhael.design.x.getWith
import kotlin.math.max

class Graphing(
    private val measures: Measures,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {

    private val rdx = Redux()
    private var handler: ((Any) -> Unit)? = null

    fun start(observer: (Levels) -> Unit) {
        handler = measures.observe { measure ->
            rdx
                .dispatch { dispatchMeasure(measure, it) }
                .let {
                    Levels(
                        it.getWith(STATE_MEASURES, measure),
                        it.getWith(STATE_AVERAGE, measure),
                        it.getWith(STATE_MAX, measure)
                    )
                }
                .also {
                    scope.launch(dispatcher) { observer(it) }
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
        val count = state.getWith(STATE_COUNT, 0)

        val newCount = (count + 1).toDouble()

        return mapOf(
            STATE_MEASURES to measure,
            STATE_AVERAGE to ((newCount - 1) / newCount * average + measure / newCount).toInt(),
            STATE_MAX to max(max, measure),
            STATE_COUNT to count + 1
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
    }
}