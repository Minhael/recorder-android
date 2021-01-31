package me.minhael.recorder.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import me.minhael.recorder.databinding.FragmentRecorderBinding
import org.joda.time.Period
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.PeriodFormatterBuilder

class RecorderFragment: Fragment() {

    private val viewModel: RecorderViewModel by activityViewModels()

    private var _v: FragmentRecorderBinding? = null
    private val v get() = _v!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.measure.observe(this) { v.recorderTvValue.text = it.toString() }
        viewModel.average.observe(this) { v.recorderTvAverage.text = it.toString() }
        viewModel.max.observe(this) { v.recorderTvMax.text = it.toString() }
        viewModel.startTime.observe(this) { v.recorderTvStartTime.text = FORMAT_START_TIME.print(it) }
        viewModel.duration.observe(this) { v.recorderTvDuration.text = FORMAT_DURATION.print(Period(it)) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _v = FragmentRecorderBinding.inflate(inflater, container, false)
        return v.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _v = null
    }

    data class RecorderViewModel(
        val startTime: MutableLiveData<Long> = MutableLiveData(0),
        val duration: MutableLiveData<Long> = MutableLiveData(0),
        val measure: MutableLiveData<Int> = MutableLiveData(0),
        val average: MutableLiveData<Int> = MutableLiveData(0),
        val max: MutableLiveData<Int> = MutableLiveData(0),
    ) : ViewModel()

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