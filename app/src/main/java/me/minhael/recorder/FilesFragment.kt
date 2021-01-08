package me.minhael.recorder

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
import kotlinx.android.synthetic.main.fragment_files.*
import kotlinx.android.synthetic.main.item_record.view.*
import me.minhael.android.ItemAdapter
import me.minhael.design.FileSystem

class FilesFragment : Fragment() {

    private val viewModel: FilesViewModel by activityViewModels()

    private val items = ItemAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.directory.observe(this) { refresh(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_files, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        main_recycler.apply {
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
                    view.record_name.text = it.filename
                }
            }
            .apply { items.put(0, *toTypedArray()) }
    }

    data class FilesViewModel(
        val directory: MutableLiveData<FileSystem> = MutableLiveData<FileSystem>()
    ) : ViewModel()
}