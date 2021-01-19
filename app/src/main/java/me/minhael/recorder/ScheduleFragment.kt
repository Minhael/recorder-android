package me.minhael.recorder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.android.synthetic.main.fragment_schedule.*
import org.joda.time.format.DateTimeFormat

class ScheduleFragment : Fragment() {

    private val viewModel: ScheduleViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.lastActivation.observe(this) { updateViews(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_schedule, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateViews(null)
    }

    private fun updateViews(lastActivation: Pair<Long, Long>?) {
        schedule_tg_activation.isChecked = lastActivation != null
        schedule_tg_activation.setOnCheckedChangeListener { _, isChecked ->
            viewModel.schedule.value = if (isChecked) {
                System.currentTimeMillis() + 30 * 1000 to 30 * 1000
            } else {
                null
            }
        }

        if (lastActivation != null) {
            val (timestamp, durationMs) = lastActivation
            schedule_tv_last_recording.text = FORMAT_DATETIME.print(timestamp)
            schedule_tv_duration.text = "${durationMs / 1000}"
        }
    }

    data class ScheduleViewModel(
        val lastActivation: MutableLiveData<Pair<Long, Long>?> = MutableLiveData(),
        val schedule: MutableLiveData<Pair<Long, Long>?> = MutableLiveData()
    ): ViewModel()

    companion object {
        private val FORMAT_DATETIME = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
    }
}