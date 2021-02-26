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
import kotlinx.coroutines.launch
import me.minhael.design.android.AndroidFS
import me.minhael.design.android.Documents
import me.minhael.design.props.Props
import me.minhael.recorder.Permissions
import me.minhael.recorder.PropTags
import me.minhael.recorder.R
import me.minhael.recorder.databinding.ActivityRecordBinding
import me.minhael.recorder.service.Graphing
import me.minhael.recorder.service.Session
import me.minhael.recorder.service.Storage
import me.minhael.recorder.view.RecorderFragment
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class RecorderActivity : AppCompatActivity() {

    private val props: Props by inject()
    private val storage: Storage by inject()
    private val session: Session by inject()
    private val graphing: Graphing by inject()

    private val recorderViewModel: RecorderFragment.RecorderViewModel by viewModel()
    private lateinit var v: ActivityRecordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        v = ActivityRecordBinding.inflate(layoutInflater)
        setContentView(v.root)
        setSupportActionBar(v.recordToolbar)

        v.recordFab.setOnClickListener {
            v.recordFab.isEnabled = false
            toggleRecording()
            v.recordFab.isEnabled = true
        }

        val isRecording = session.isRecording()
        updateView(isRecording)
        if (isRecording) {
            startPolling()
        } else {
            recorderViewModel.state.value = mapRecordingState2ViewState(graphing.getLevels())
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

    private fun toggleRecording() {
        lifecycleScope.launch {
            if (session.isRecording()) {
                session.stop()
                updateView(false)
                stopPolling()
            } else {
                session.start()
                updateView(true)
                startPolling()
            }
        }
    }

    private fun updateView(isRecording: Boolean) {
        recorderViewModel.isRecording.value = isRecording
        v.recordFab.setImageDrawable(
            ContextCompat.getDrawable(
                applicationContext,
                if (isRecording)
                    R.drawable.ic_baseline_stop_24
                else
                    R.drawable.ic_baseline_mic_24
            )
        )
    }

    private fun startPolling() {
        graphing.start { recorderViewModel.state.value = mapRecordingState2ViewState(it) }
    }

    private fun stopPolling() {
        graphing.stop()
    }

    private fun mapRecordingState2ViewState(levels: Graphing.Levels): RecorderFragment.ViewState {
        return RecorderFragment.ViewState(
            session.startTime() ?: System.currentTimeMillis(),
            levels.measure,
            levels.average,
            levels.max
        )
    }
}