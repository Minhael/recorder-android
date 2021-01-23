package me.minhael.recorder.component

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import me.minhael.design.android.Services
import me.minhael.recorder.R
import me.minhael.recorder.Recorder
import me.minhael.recorder.databinding.ActivityRecordBinding
import me.minhael.recorder.service.Recording
import org.koin.android.ext.android.inject

class RecordActivity : AppCompatActivity() {

    private val recording: Recording by inject()

    private lateinit var v: ActivityRecordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        v = ActivityRecordBinding.inflate(layoutInflater)
        setContentView(v.root)

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
}