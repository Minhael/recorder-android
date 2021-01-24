package me.minhael.recorder.component

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import me.minhael.design.android.AndroidFS
import me.minhael.design.android.Documents
import me.minhael.design.android.Services
import me.minhael.design.props.Props
import me.minhael.recorder.Permissions
import me.minhael.recorder.PropTags
import me.minhael.recorder.R
import me.minhael.recorder.Recorder
import me.minhael.recorder.databinding.ActivityRecordBinding
import me.minhael.recorder.service.Recording
import me.minhael.recorder.service.Storage
import org.koin.android.ext.android.inject

class RecordActivity : AppCompatActivity() {

    private val props: Props by inject()
    private val storage: Storage by inject()
    private val recording: Recording by inject()

    private lateinit var v: ActivityRecordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        v = ActivityRecordBinding.inflate(layoutInflater)
        setContentView(v.root)
        setSupportActionBar(v.recordToolbar)

        Services.start<Recorder>(this, RecorderService::class.java) {
            v.recordFab.setImageDrawable(
                ContextCompat.getDrawable(
                    applicationContext,
                    if (it.isRecording())
                        R.drawable.ic_baseline_stop_24
                    else
                        R.drawable.ic_baseline_mic_24
                )
            )
        }

        v.recordFab.setOnClickListener {
            v.recordFab.isEnabled = false

            recording.toggle {
                v.recordFab.setImageDrawable(
                    ContextCompat.getDrawable(
                        applicationContext,
                        if (it)
                            R.drawable.ic_baseline_stop_24
                        else
                            R.drawable.ic_baseline_mic_24
                    )
                )
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
}