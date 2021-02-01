package me.minhael.recorder.component

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.minhael.design.android.AndroidFS
import me.minhael.design.android.Documents
import me.minhael.design.props.Props
import me.minhael.recorder.Permissions
import me.minhael.recorder.PropTags
import me.minhael.recorder.R
import me.minhael.recorder.service.Recording
import me.minhael.recorder.databinding.ActivityRecordBinding
import me.minhael.recorder.service.Storage
import me.minhael.recorder.view.RecorderFragment
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class RecorderActivity : AppCompatActivity() {

    private val props: Props by inject()
    private val storage: Storage by inject()
    private val recording: Recording by inject()

    private val recorderViewModel: RecorderFragment.RecorderViewModel by viewModel()
    private lateinit var v: ActivityRecordBinding

    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        v = ActivityRecordBinding.inflate(layoutInflater)
        setContentView(v.root)
        setSupportActionBar(v.recordToolbar)

        v.recordFab.setOnClickListener {
            v.recordFab.isEnabled = false
            toggleRecording(recording.state)
            v.recordFab.isEnabled = true
        }

        val lastState = recording.state
        updateView(lastState)
        if (lastState.isRecording) {
            startPolling()
        } else {
            recorderViewModel.state.value = lastState.run { mapRecordingState2ViewState(this) }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_record, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_folder -> {
                Documents.select(this, 0) {
                    props.put(PropTags.DIR_RECORDING, it.toString())
                    storage.dirPublic = AndroidFS.base(applicationContext, it)
                    Snackbar.make(v.root, R.string.msg_change_root_directory, Snackbar.LENGTH_SHORT).show()
                }
                true
            }
            R.id.action_browse -> {
                startActivity(
                    Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse(storage.dirPublic.root())
                    }
                )
                true
            }
            R.id.action_grant_permissions -> {
                grantPermissions()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun grantPermissions() {
        Permissions.request(this) {
            if (it)
                Snackbar.make(v.root, R.string.msg_permissions_granted, Snackbar.LENGTH_SHORT).show()
            else
                Snackbar
                    .make(v.root, R.string.msg_permissions_not_granted, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.word_retry) { grantPermissions() }
                    .show()
        }
    }

    private fun toggleRecording(state: Recording.State) {
        lifecycleScope.launch {
            if (state.isRecording) {
                updateView(recording.stop())
                stopPolling()
            } else {
                updateView(recording.start())
                startPolling()
            }
        }
    }

    private fun updateView(state: Recording.State) {
        recorderViewModel.isRecording.value = state.isRecording
        v.recordFab.setImageDrawable(
            ContextCompat.getDrawable(
                applicationContext,
                if (state.isRecording)
                    R.drawable.ic_baseline_stop_24
                else
                    R.drawable.ic_baseline_mic_24
            )
        )
    }

    private fun startPolling() {
        val periodMs = props.get(PropTags.UI_GRAPH_UPDATE_MS, PropTags.UI_GRAPH_UPDATE_MS_DEFAULT)
        stopPolling()

        job = lifecycleScope.launch {
            while (isActive) {
                recorderViewModel.state.value = recording.state.run { mapRecordingState2ViewState(this) }
                delay(periodMs)
            }
        }
    }

    private fun stopPolling() {
        job?.cancel()
        job = null
    }

    private fun mapRecordingState2ViewState(state: Recording.State): RecorderFragment.ViewState {
        return RecorderFragment.ViewState(
            state.startTime ?: System.currentTimeMillis(),
            state.startTime?.let { System.currentTimeMillis() - it } ?: 0,
            state.measures.lastOrNull() ?: 0,
            state.levels.average,
            state.levels.max
        )
    }
}