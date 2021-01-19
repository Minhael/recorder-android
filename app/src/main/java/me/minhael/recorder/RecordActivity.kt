package me.minhael.recorder

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_record.*
import me.minhael.android.Services
import org.koin.android.ext.android.inject

class RecordActivity : AppCompatActivity() {

    private val controller: RecordController by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record)

        Services.start<Recorder>(this, RecorderService::class.java) {
            record_fab.setImageDrawable(
                ContextCompat.getDrawable(
                    applicationContext,
                    if (it.isRecording())
                        R.drawable.ic_baseline_stop_24
                    else
                        R.drawable.ic_baseline_mic_24
                )
            )
        }

        record_fab.setOnClickListener {
            record_fab.isEnabled = false

            controller.toggle(this) {
                record_fab.setImageDrawable(
                    ContextCompat.getDrawable(
                        applicationContext,
                        if (it)
                            R.drawable.ic_baseline_stop_24
                        else
                            R.drawable.ic_baseline_mic_24
                    )
                )
                record_fab.isEnabled = true
            }
        }
    }
}