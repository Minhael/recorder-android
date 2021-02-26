package me.minhael.recorder.service

import kotlinx.coroutines.*
import me.minhael.design.fs.Uri
import me.minhael.design.props.Props
import me.minhael.design.sl.Serializer
import me.minhael.design.state.Redux
import me.minhael.design.x.getWith
import me.minhael.recorder.PropTags
import org.joda.time.format.DateTimeFormat
import org.slf4j.LoggerFactory

class Reports(
    private val storage: Storage,
    private val props: Props,
    private val resolver: Uri.Resolver,
    private val serializer: Serializer,
    private val measures: Measures,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {

    private val rdx = Redux()
    private var handler: ((Any) -> Unit)? = null

    fun start() {
        rdx.dispatch { startPlotting(it) }
    }

    fun stop(startTime: Long, interval: Long) {
        rdx.dispatch { stopPlotting(startTime, interval, it) }
    }

    private fun startPlotting(state: Map<String, Any>): Map<String, Any> {
        if (state.getWith(STATE_IS_PLOTTING, false))
            return emptyMap()

        logger.debug("Start reports")

        handler = measures.observe { measure -> rdx.dispatch { updateMeasure(measure, it) } }

        return mapOf(
            STATE_IS_PLOTTING to true,
            STATE_MEASURES to arrayListOf<Int>()
        )
    }

    private fun stopPlotting(startTime: Long, interval: Long, state: Map<String, Any>): Map<String, Any> {
        if (!state.getWith(STATE_IS_PLOTTING, false))
            return emptyMap()

        logger.debug("Stop reports")

        handler?.also { measures.drop(it) }

        val report = Report(startTime, interval, state.getWith(STATE_MEASURES, arrayListOf()))

        val pattern = props.get(PropTags.RECORDING_FILE_PATTERN, PropTags.RECORDING_FILE_PATTERN_DEFAULT)
        val filename = DateTimeFormat.forPattern(pattern).print(report.startTime)
        val uriReport = storage.dirPublic.create("application/json", "$filename.json")

        //  Write report
        scope.launch {
            withContext(dispatcher) {
                resolver.writeTo(uriReport).use { serializer.serialize(report, it) }
            }
        }

        return mapOf(STATE_IS_PLOTTING to false)
    }

    private fun updateMeasure(measure: Int, state: Map<String, Any>): Map<String, Any> {
        val plots = state.getWith(STATE_MEASURES, arrayListOf<Int>())
        plots.add(measure)
        return mapOf(STATE_MEASURES to plots)
    }

    data class Report(
        val startTime: Long,
        val interval: Long,
        val levels: MutableList<Int>
    )

    companion object {
        private val STATE_IS_PLOTTING = "${Report::class.java}.state.is_plotting"
        private val STATE_MEASURES = "${Report::class.java}.state.measures"

        private val logger = LoggerFactory.getLogger(Reports::class.java)
    }
}