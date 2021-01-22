package me.minhael.recorder.component

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import me.minhael.design.props.Props
import me.minhael.recorder.PropTags
import me.minhael.recorder.controller.ScheduleController
import me.minhael.recorder.databinding.ActivityScheduleBinding
import me.minhael.recorder.ui.ScheduleFragment
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinApiExtension

class ScheduleActivity : AppCompatActivity() {

    private val props: Props by inject()
    private val controller: ScheduleController by inject()

    private val scheduleViewModel: ScheduleFragment.ScheduleViewModel by viewModels()

    private lateinit var v: ActivityScheduleBinding

    @KoinApiExtension
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        v = ActivityScheduleBinding.inflate(layoutInflater)
        setContentView(v.root)
        setSupportActionBar(v.scheduleToolbar)

        scheduleViewModel.isActivate.value = props.get(PropTags.RECORDING_DAILY, PropTags.RECORDING_DAILY_DEFAULT)
        scheduleViewModel.timestamp.value = props.get(PropTags.RECORDING_TIME_START, PropTags.RECORDING_TIME_START_DEFAULT)
        scheduleViewModel.durationMs.value = props.get(PropTags.RECORDING_DURATION_MS, PropTags.RECORDING_DURATION_MS_DEFAULT)
        scheduleViewModel.periodMs.value = props.get(PropTags.RECORDING_PERIOD_MS, PropTags.RECORDING_PERIOD_MS_DEFAULT)

        scheduleViewModel.isActivate.observe(this) {
            if (props.get(PropTags.RECORDING_DAILY, PropTags.RECORDING_DAILY_DEFAULT) != it ) {
                props.put(PropTags.RECORDING_DAILY, it)
                if (it) {
                    controller.activate()
                } else {
                    controller.deactivate()
                }
            }
        }

        scheduleViewModel.timestamp.observe(this) {
            if (props.get(PropTags.RECORDING_TIME_START, PropTags.RECORDING_TIME_START_DEFAULT) != it ) {
                props.put(PropTags.RECORDING_TIME_START, PropTags.RECORDING_TIME_START_DEFAULT)
                if (props.get(PropTags.RECORDING_DAILY, PropTags.RECORDING_DAILY_DEFAULT)) {
                    controller.activate()
                }
            }
        }

        scheduleViewModel.durationMs.observe(this) {
            if (props.get(PropTags.RECORDING_DURATION_MS, PropTags.RECORDING_DURATION_MS_DEFAULT) != it ) {
                props.put(PropTags.RECORDING_DURATION_MS, PropTags.RECORDING_DURATION_MS_DEFAULT)
                if (props.get(PropTags.RECORDING_DAILY, PropTags.RECORDING_DAILY_DEFAULT)) {
                    controller.activate()
                }
            }
        }

        scheduleViewModel.periodMs.observe(this) {
            if (props.get(PropTags.RECORDING_PERIOD_MS, PropTags.RECORDING_PERIOD_MS_DEFAULT) != it ) {
                props.put(PropTags.RECORDING_PERIOD_MS, PropTags.RECORDING_PERIOD_MS_DEFAULT)
                if (props.get(PropTags.RECORDING_DAILY, PropTags.RECORDING_DAILY_DEFAULT)) {
                    controller.activate()
                }
            }
        }
    }
}