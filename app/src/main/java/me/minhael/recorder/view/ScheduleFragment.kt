package me.minhael.recorder.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import me.minhael.design.android.ItemAdapter
import me.minhael.design.android.TimePickerFragment
import me.minhael.recorder.R
import me.minhael.recorder.databinding.FragmentScheduleBinding
import me.minhael.recorder.databinding.ItemPrefBinding
import org.joda.time.format.DateTimeFormat

class ScheduleFragment : Fragment() {

    private val viewModel: ScheduleViewModel by activityViewModels()

    private val items = ItemAdapter()

    private var _v: FragmentScheduleBinding? = null
    private val v get() = _v!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.isActivate.observe(this) { updateDaily(it) }
        viewModel.interval.observe(this) { (startTime, endTime) -> updateInterval(startTime, endTime) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _v = FragmentScheduleBinding.inflate(inflater, container, false)
        return v.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val lm = object : LinearLayoutManager(context) {
            override fun canScrollHorizontally() = false
            override fun canScrollVertically() = false
        }
        v.scheduleRecycler.apply {
            layoutManager = lm
            adapter = items
            addItemDecoration(DividerItemDecoration(context, lm.orientation))
            setHasFixedSize(true)
        }
        v.scheduleLayoutToggle.setOnClickListener { v.scheduleToggle.toggle() }
        updateDaily(false)
        updateInterval(0, 0)
    }

    private fun updateDaily(isActivate: Boolean) {
        v.scheduleToggle.apply {
            setOnCheckedChangeListener(null)
            isChecked = isActivate
            setOnCheckedChangeListener { _, isChecked ->
                viewModel.isActivate.value = isChecked
            }
        }
    }

    private fun updateInterval(startTime: Long, endTime: Long) {
        val prefStart = ItemAdapter.Item(R.layout.item_pref) {
            ItemPrefBinding.bind(it).apply {
                itemPrefLabel.setText(R.string.phrase_start_time)
                itemPrefValue.text = FORMAT_TIME.print(startTime)
                root.setOnClickListener { selectStartTime(startTime, endTime) }
            }
        }
        items.update(0, prefStart)

        val prefEnd = ItemAdapter.Item(R.layout.item_pref) {
            ItemPrefBinding.bind(it).apply {
                itemPrefLabel.setText(R.string.phrase_end_time)
                itemPrefValue.text = FORMAT_TIME.print(endTime)
                root.setOnClickListener { selectEndTime(startTime, endTime) }
            }
        }
        items.update(1, prefEnd)
    }

    private fun selectStartTime(original: Long, endTime: Long) {
        childFragmentManager.setFragmentResultListener(REQ_KEY_SELECT_START, this) { _, bundle ->
            viewModel.interval.value = bundle.getLong(TimePickerFragment.RESULT_TIME) to endTime
        }
        TimePickerFragment.newInstance(REQ_KEY_SELECT_START, original).show(childFragmentManager, TAG_TIME_PICKER)
    }

    private fun selectEndTime(startTime: Long, original: Long) {
        childFragmentManager.setFragmentResultListener(REQ_KEY_SELECT_END, this) { _, bundle ->
            val endTime = bundle.getLong(TimePickerFragment.RESULT_TIME)

            //  Error handling for end time boundary
            if (startTime < endTime) {
                viewModel.interval.value = startTime to endTime
            } else {
                Snackbar
                    .make(v.root, R.string.msg_schedule_early_end_time, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.word_retry) { selectEndTime(startTime, original) }
                    .show()
            }
        }
        TimePickerFragment.newInstance(REQ_KEY_SELECT_END, original).show(childFragmentManager, TAG_TIME_PICKER)
    }

    data class ScheduleViewModel(
        val isActivate: MutableLiveData<Boolean> = MutableLiveData(false),
        val interval: MutableLiveData<Pair<Long, Long>> = MutableLiveData(0L to 0L)
    ) : ViewModel()

    companion object {
        private val FORMAT_TIME = DateTimeFormat.forPattern("HH:mm")

        private val REQ_KEY_SELECT_START = "${ScheduleFragment::class.java}.select_start"
        private val REQ_KEY_SELECT_END = "${ScheduleFragment::class.java}.select_end"

        private val TAG_TIME_PICKER = "${ScheduleFragment::class.java}.time_picker"
    }
}