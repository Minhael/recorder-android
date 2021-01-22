package me.minhael.recorder.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import me.minhael.recorder.databinding.FragmentScheduleBinding
import org.joda.time.format.DateTimeFormat

class ScheduleFragment : Fragment() {

    private val viewModel: ScheduleViewModel by activityViewModels()

    private var _v: FragmentScheduleBinding? = null
    private val v get() = _v!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.isActivate.observe(this) { updateViews(it) }
        viewModel.timestamp.observe(this) { }
        viewModel.durationMs.observe(this) { }
        viewModel.periodMs.observe(this) { }
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
        updateViews(false)
    }

    private fun updateViews(isActivate: Boolean) {
        v.scheduleTgActivation.isChecked = isActivate
        v.scheduleTgActivation.setOnCheckedChangeListener { _, isChecked ->
            viewModel.isActivate.value = isChecked
        }
    }

    data class ScheduleViewModel(
        val isActivate: MutableLiveData<Boolean> = MutableLiveData(false),
        val timestamp: MutableLiveData<Long> = MutableLiveData(0),
        val durationMs: MutableLiveData<Long> = MutableLiveData(0),
        val periodMs: MutableLiveData<Long> = MutableLiveData(0)
    ): ViewModel()

    companion object {
        private val FORMAT_DATETIME = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
    }
}