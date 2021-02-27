package me.minhael.recorder.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.anychart.AnyChart
import com.anychart.chart.common.dataentry.DataEntry
import com.anychart.core.cartesian.series.Line
import com.anychart.enums.Interval
import com.anychart.scales.DateTime
import me.minhael.recorder.R
import me.minhael.recorder.databinding.FragmentRecorderBinding
import org.apache.commons.collections4.queue.CircularFifoQueue
import org.joda.time.DateTimeZone
import org.joda.time.Period
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.PeriodFormatterBuilder


class RecorderFragment : Fragment() {

    private val viewModel: RecorderViewModel by activityViewModels()

    private val dataPointSize = 100
    private val dataMeasure = CircularFifoQueue<DataEntry>(dataPointSize)
    private val dataAverage = CircularFifoQueue<DataEntry>(dataPointSize)

    private var _v: FragmentRecorderBinding? = null
    private val v get() = _v!!

    private var seriesMeasure: Line? = null
    private var seriesAverage: Line? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.isRecording.observe(this) {
            if (it) {
                dataMeasure.clear()
                dataAverage.clear()
            }
        }
        viewModel.state.observe(this) { updateViews(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _v = FragmentRecorderBinding.inflate(inflater, container, false)
        return v.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //  Create a line chart
        val chart = AnyChart.line().apply {
            animation(true)
            xScale(
                DateTime.instantiate().apply {
                    ticks().interval(Interval.SECOND, 1)
                }
            )
            yScale().apply {
                softMinimum(20)
                softMaximum(60)
            }
            yAxis(0).labels().position("inside")
        }

        //  Must use some fake data to instantiate the graph view, otherwise it is not updating
        dataMeasure.offer(LocalTimeDataEntry(System.currentTimeMillis(), 0))
        dataAverage.offer(LocalTimeDataEntry(System.currentTimeMillis(), 0))

        //  Setup data series
        val colorMeasure = ContextCompat.getColor(requireContext(), R.color.colorPrimary).toLong() and 0x00ffffffff
        val colorAverage = ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark).toLong() and 0x00ffffffff
        seriesMeasure = chart.line(dataMeasure.toMutableList()).apply {
            name(getString(R.string.word_measure))
            color("#${colorMeasure.toString(16).takeLast(6)}")
        }
        seriesAverage = chart.line(dataAverage.toMutableList()).apply {
            name(getString(R.string.word_average))
            color("#${colorAverage.toString(16).takeLast(6)}")
        }

        //  Clear dummy data
        dataMeasure.clear()
        dataAverage.clear()

        //  Draw chart
        v.recorderGraphLevel.setChart(chart)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _v = null
        seriesAverage = null
        seriesMeasure = null
    }

    private fun updateViews(state: ViewState) {
        v.recorderTvValue.text = state.measure.toString()
        v.recorderTvAverage.text = state.average.toString()
        v.recorderTvMax.text = state.max.toString()

        v.recorderTvStartTime.text = FORMAT_START_TIME.print(state.startTime)
        val duration = System.currentTimeMillis() - state.startTime
        v.recorderTvDuration.text = FORMAT_DURATION.print(Period(duration))

        //  Accumulate values
        val timestamp = state.startTime + duration
        dataMeasure.offer(LocalTimeDataEntry(timestamp, state.measure))
        dataAverage.offer(LocalTimeDataEntry(timestamp, state.average))

        //  Update graph
        seriesMeasure?.data(dataMeasure.toMutableList())
        seriesAverage?.data(dataAverage.toMutableList())
    }

    data class ViewState(
        val startTime: Long,
        val measure: Int,
        val average: Int,
        val max: Int
    )

    data class RecorderViewModel(
        val isRecording: MutableLiveData<Boolean> = MutableLiveData(false),
        val state: MutableLiveData<ViewState> = MutableLiveData()
    ) : ViewModel()

    private class LocalTimeDataEntry(timestamp: Long, value: Number) : DataEntry() {
        init {
            setValue("x", localTimeZone.convertUTCToLocal(timestamp))
            setValue("value", value)
        }

        companion object {
            private val localTimeZone = DateTimeZone.getDefault()
        }
    }

    companion object {
        private val FORMAT_START_TIME = DateTimeFormat.forPattern("MM/dd HH:mm")
        private val FORMAT_DURATION = PeriodFormatterBuilder()
            .printZeroAlways()
            .minimumPrintedDigits(2)
            .appendHours()
            .appendSeparator(":")
            .minimumPrintedDigits(2)
            .appendMinutes()
            .appendSeparator(":")
            .appendSeconds()
            .toFormatter()
    }
}