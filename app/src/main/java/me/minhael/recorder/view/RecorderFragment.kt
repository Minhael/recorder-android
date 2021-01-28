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
import java.lang.RuntimeException

class RecorderFragment: Fragment() {

    private val viewModel: RecorderViewModel by activityViewModels()

    private var _v: FragmentRecorderBinding? = null
    private val v get() = _v!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.lastDb.observe(this) {  }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _v = FragmentRecorderBinding.inflate(inflater, container, false)
        return v.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        v.recorderBtn.text = "Crash"
        v.recorderBtn.setOnClickListener {
            throw RuntimeException("Test Crash")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _v = null
    }

    data class RecorderViewModel(
        val lastDb: MutableLiveData<Long> = MutableLiveData(0)
    ) : ViewModel()
}