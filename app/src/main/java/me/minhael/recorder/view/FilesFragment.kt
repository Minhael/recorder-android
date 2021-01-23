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
import me.minhael.design.android.ItemAdapter
import me.minhael.design.fs.FileSystem
import me.minhael.recorder.R
import me.minhael.recorder.databinding.FragmentFilesBinding
import me.minhael.recorder.databinding.ItemRecordBinding

class FilesFragment : Fragment() {

    private val viewModel: FilesViewModel by activityViewModels()

    private val items = ItemAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.directory.observe(this) { refresh(it) }
    }

    private var _v: FragmentFilesBinding? = null
    private val v get() = _v!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _v = FragmentFilesBinding.inflate(inflater, container, false)
        return v.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _v = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        v.mainRecycler.apply {
            adapter = items
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            this.setHasFixedSize(true)
        }
    }

    private fun refresh(fs: FileSystem) {
        items.clear()
        fs.list()
            .map { fs.peek(it) }
            .map {
                ItemAdapter.Item(R.layout.item_record) { view ->
                    val v = ItemRecordBinding.bind(view)
                    v.recordName.text = it.filename
                }
            }
            .apply { items.put(0, *toTypedArray()) }
    }

    data class FilesViewModel(
        val directory: MutableLiveData<FileSystem> = MutableLiveData<FileSystem>()
    ) : ViewModel()
}