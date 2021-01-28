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
import me.minhael.design.android.Services
import me.minhael.design.props.Props
import me.minhael.recorder.*
import me.minhael.recorder.databinding.ActivityRecordBinding
import me.minhael.recorder.service.Recording
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

        Services.start<Recorder>(this, RecorderService::class.java) { updateViews(it.isRecording()) }

        v.recordFab.setOnClickListener {
            v.recordFab.isEnabled = false

            recording.toggle {
                updateViews(it)
                v.recordFab.isEnabled = true
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_record, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_timer -> {
                startActivity(Intent(this, ScheduleActivity::class.java))
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

    private fun updateViews(isRecording: Boolean) {
        v.recordFab.setImageDrawable(
            ContextCompat.getDrawable(
                applicationContext,
                if (isRecording)
                    R.drawable.ic_baseline_stop_24
                else
                    R.drawable.ic_baseline_mic_24
            )
        )

        if (isRecording) {
            job?.cancel()
            job = lifecycleScope.launch {
                while (isActive) {
                    recorderViewModel.apply {
                        recording.levels.also {
                            measure.value = it.measure
                            average.value = it.average
                            max.value = it.max
                        }
                    }
                    delay(props.get(PropTags.MEASURE_PERIOD_UPDATE_MS, PropTags.MEASURE_PERIOD_UPDATE_MS_DEFAULT))
                }
            }
        } else {
            job?.cancel()
        }
    }
}