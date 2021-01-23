package me.minhael.recorder.component

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import me.minhael.design.props.Props
import me.minhael.recorder.PropTags
import me.minhael.recorder.databinding.ActivityScheduleBinding
import me.minhael.recorder.service.Schedule
import me.minhael.recorder.view.ScheduleFragment
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinApiExtension

class ScheduleActivity : AppCompatActivity() {

    private val props: Props by inject()
    private val controller: Schedule by inject()

    private val scheduleViewModel: ScheduleFragment.ScheduleViewModel by viewModels()

    private lateinit var v: ActivityScheduleBinding

    @KoinApiExtension
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        v = ActivityScheduleBinding.inflate(layoutInflater)
        setContentView(v.root)
        setSupportActionBar(v.scheduleToolbar)

        val startTime = props.get(PropTags.RECORDING_TIME_START, PropTags.RECORDING_TIME_START_DEFAULT)
        val duration = props.get(PropTags.RECORDING_DURATION_MS, PropTags.RECORDING_DURATION_MS_DEFAULT)

        scheduleViewModel.isActivate.value = props.get(PropTags.RECORDING_DAILY, PropTags.RECORDING_DAILY_DEFAULT)
        scheduleViewModel.interval.value = startTime to duration
        scheduleViewModel.periodMs.value = props.get(PropTags.RECORDING_PERIOD_MS, PropTags.RECORDING_PERIOD_MS_DEFAULT)

        scheduleViewModel.isActivate.observe(this) {
            if (props.get(PropTags.RECORDING_DAILY, PropTags.RECORDING_DAILY_DEFAULT) != it) {
                props.put(PropTags.RECORDING_DAILY, it)
                if (it) {
                    controller.activate()
                } else {
                    controller.deactivate()
                }
            }
        }

        scheduleViewModel.interval.observe(this) { (startTime, duration) ->
            if (props.get(PropTags.RECORDING_TIME_START, PropTags.RECORDING_TIME_START_DEFAULT) != startTime) {
                props.put(PropTags.RECORDING_TIME_START, startTime)
                if (props.get(PropTags.RECORDING_DAILY, PropTags.RECORDING_DAILY_DEFAULT)) {
                    controller.activate()
                }
            }

            if (props.get(PropTags.RECORDING_DURATION_MS, PropTags.RECORDING_DURATION_MS_DEFAULT) != duration) {
                props.put(PropTags.RECORDING_DURATION_MS, duration)
                if (props.get(PropTags.RECORDING_DAILY, PropTags.RECORDING_DAILY_DEFAULT)) {
                    controller.activate()
                }
            }
        }

        scheduleViewModel.periodMs.observe(this) {
            if (props.get(PropTags.RECORDING_PERIOD_MS, PropTags.RECORDING_PERIOD_MS_DEFAULT) != it) {
                props.put(PropTags.RECORDING_PERIOD_MS, it)
                if (props.get(PropTags.RECORDING_DAILY, PropTags.RECORDING_DAILY_DEFAULT)) {
                    controller.activate()
                }
            }
        }
    }
}