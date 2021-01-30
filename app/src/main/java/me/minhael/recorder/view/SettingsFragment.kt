package me.minhael.recorder.view

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.preference.*
import com.google.android.material.snackbar.Snackbar
import me.minhael.design.android.TimePickerFragment
import me.minhael.recorder.R
import org.joda.time.format.DateTimeFormat
import java.lang.NumberFormatException

class SettingsFragment : PreferenceFragmentCompat() {

    private val viewModel: SettingsViewModel by activityViewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_settings, rootKey)
        val prefFilename = findPreference<EditTextPreference>(getString(R.string.key_recording_file_pattern))  ?: throw IllegalStateException()
        viewModel.filenamePattern.observe(this) {
            prefFilename.summary = it
            prefFilename.setDefaultValue(it)
        }

        val prefGraphing = findPreference<ListPreference>(getString(R.string.key_recording_graphing_period)) ?: throw IllegalStateException()
        viewModel.graphing.observe(this) {
            prefGraphing.summary = it.toString()
            prefGraphing.setDefaultValue(it.toString())
        }
        prefGraphing.setOnPreferenceChangeListener { _, newValue ->
            try {
                viewModel.graphing.value = (newValue as String).toLong()
                true
            } catch (e: NumberFormatException) {
                false
            }
        }

        val prefDailyRecording = findPreference<SwitchPreferenceCompat>(getString(R.string.key_recording_daily)) ?: throw IllegalStateException()
        val prefStartTime = findPreference<Preference>(getString(R.string.key_schedule_start_time)) ?: throw IllegalStateException()
        val prefEndTime = findPreference<Preference>(getString(R.string.key_schedule_end_time)) ?: throw IllegalStateException()

        viewModel.isActivate.observe(this) { prefDailyRecording.setDefaultValue(it.toString()) }
        prefDailyRecording.setOnPreferenceChangeListener { _, newValue ->
            viewModel.isActivate.value = newValue as Boolean
            true
        }
        viewModel.interval.observe(this) { (startTime, endTime) ->
            prefStartTime.summary = FORMAT_TIME.print(startTime)
            prefEndTime.summary = FORMAT_TIME.print(endTime)
            prefStartTime.setOnPreferenceClickListener {
                selectStartTime(startTime, endTime)
                true
            }
            prefEndTime.setOnPreferenceClickListener {
                selectEndTime(startTime, endTime)
                true
            }
        }
    }

    private fun selectStartTime(original: Long, endTime: Long) {
        childFragmentManager.setFragmentResultListener(REQ_KEY_SELECT_START, this) { _, bundle ->
            viewModel.interval.value = bundle.getLong(TimePickerFragment.RESULT_TIME) to endTime
        }
        TimePickerFragment.newInstance(REQ_KEY_SELECT_START, original).show(childFragmentManager, TAG_TIME_PICKER)
    }

    private fun selectEndTime(startTime: Long, original: Long) {
        childFragmentManager.setFragmentResultListener(REQ_KEY_SELECT_END, this) { _, bundle ->
            viewModel.interval.value = startTime to bundle.getLong(TimePickerFragment.RESULT_TIME)
        }
        TimePickerFragment.newInstance(REQ_KEY_SELECT_END, original).show(childFragmentManager, TAG_TIME_PICKER)
    }

    data class SettingsViewModel(
        val filenamePattern: MutableLiveData<String> = MutableLiveData(""),
        val graphing: MutableLiveData<Long> = MutableLiveData(0),
        val isActivate: MutableLiveData<Boolean> = MutableLiveData(false),
        val interval: MutableLiveData<Pair<Long, Long>> = MutableLiveData(0L to 0L)
    ) : ViewModel()

    companion object {
        private val FORMAT_TIME = DateTimeFormat.forPattern("HH:mm")

        private val REQ_KEY_SELECT_START = "${SettingsFragment::class.java}.select_start"
        private val REQ_KEY_SELECT_END = "${SettingsFragment::class.java}.select_end"

        private val TAG_TIME_PICKER = "${SettingsFragment::class.java}.time_picker"
    }
}