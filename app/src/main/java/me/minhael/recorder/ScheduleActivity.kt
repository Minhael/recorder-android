package me.minhael.recorder

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_schedule.*
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinApiExtension

class ScheduleActivity : AppCompatActivity() {

    private val controller: ScheduleController by inject()

    private val scheduleViewModel: ScheduleFragment.ScheduleViewModel by viewModels()

    @KoinApiExtension
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)
        setSupportActionBar(schedule_toolbar)

        scheduleViewModel.schedule.observe(this) {
            if (it != null) {
                val (timestamp, durationMs) = it
                controller.activate(timestamp, durationMs)
            } else {
                controller.deactivate()
            }
        }
        scheduleViewModel.lastActivation.value = controller.lastActivation()
    }
}