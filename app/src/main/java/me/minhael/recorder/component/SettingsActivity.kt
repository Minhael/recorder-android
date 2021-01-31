package me.minhael.recorder.component

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import me.minhael.design.props.Props
import me.minhael.recorder.PropTags
import me.minhael.recorder.databinding.ActivitySettingsBinding
import me.minhael.recorder.service.Schedule
import me.minhael.recorder.view.SettingsFragment
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinApiExtension

class SettingsActivity: AppCompatActivity() {

    private val props: Props by inject()
    private val schedule: Schedule by inject()

    private val settingsViewModel: SettingsFragment.SettingsViewModel by viewModels()

    private lateinit var v: ActivitySettingsBinding

    @KoinApiExtension
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        v = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(v.root)
        setSupportActionBar(v.settingsToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val startTime = props.get(PropTags.SCHEDULE_TIME_START, PropTags.SCHEDULE_TIME_START_DEFAULT)
        val endTime = props.get(PropTags.SCHEDULE_TIME_END, PropTags.SCHEDULE_TIME_END_DEFAULT)

        settingsViewModel.filenamePattern.value = props.get(PropTags.RECORDING_FILE_PATTERN, PropTags.RECORDING_FILE_PATTERN_DEFAULT)
        settingsViewModel.measurePeriod.value = props.get(PropTags.MEASURE_PERIOD_UPDATE_MS, PropTags.MEASURE_PERIOD_UPDATE_MS_DEFAULT)
        settingsViewModel.isActivate.value = props.get(PropTags.SCHEDULE_DAILY, PropTags.SCHEDULE_DAILY_DEFAULT)
        settingsViewModel.interval.value = startTime to endTime
        settingsViewModel.graphingPeriod.value = props.get(PropTags.UI_GRAPH_UPDATE_MS, PropTags.UI_GRAPH_UPDATE_MS_DEFAULT)

        settingsViewModel.filenamePattern.observe(this) {
            if (props.get(PropTags.RECORDING_FILE_PATTERN, PropTags.RECORDING_FILE_PATTERN_DEFAULT) != it) {
                props.put(PropTags.RECORDING_FILE_PATTERN, it)
            }
        }

        settingsViewModel.measurePeriod.observe(this) {
            if (props.get(PropTags.MEASURE_PERIOD_UPDATE_MS, PropTags.MEASURE_PERIOD_UPDATE_MS_DEFAULT) != it) {
                props.put(PropTags.MEASURE_PERIOD_UPDATE_MS, it)
            }
        }

        settingsViewModel.isActivate.observe(this) {
            if (props.get(PropTags.SCHEDULE_DAILY, PropTags.SCHEDULE_DAILY_DEFAULT) != it) {
                props.put(PropTags.SCHEDULE_DAILY, it)
                if (it) {
                    schedule.activate()
                } else {
                    schedule.deactivate()
                }
            }
        }

        settingsViewModel.interval.observe(this) { (startTime, endTime) ->
            if (props.get(PropTags.SCHEDULE_TIME_START, PropTags.SCHEDULE_TIME_START_DEFAULT) != startTime) {
                props.put(PropTags.SCHEDULE_TIME_START, startTime)
                if (props.get(PropTags.SCHEDULE_DAILY, PropTags.SCHEDULE_DAILY_DEFAULT)) {
                    schedule.activate()
                }
            }

            if (props.get(PropTags.SCHEDULE_TIME_END, PropTags.SCHEDULE_TIME_END_DEFAULT) != endTime) {
                props.put(PropTags.SCHEDULE_TIME_END, endTime)
                if (props.get(PropTags.SCHEDULE_DAILY, PropTags.SCHEDULE_DAILY_DEFAULT)) {
                    schedule.activate()
                }
            }
        }

        settingsViewModel.graphingPeriod.observe(this) {
            if (props.get(PropTags.UI_GRAPH_UPDATE_MS, PropTags.UI_GRAPH_UPDATE_MS_DEFAULT) != it) {
                props.put(PropTags.UI_GRAPH_UPDATE_MS, it)
            }
        }
    }
}