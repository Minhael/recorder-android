package me.minhael.design.android

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.TimePicker
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import org.joda.time.DateTime

class TimePickerFragment : DialogFragment(), TimePickerDialog.OnTimeSetListener {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val current = arguments?.getLong(PARAM_CURRENT_MS) ?: System.currentTimeMillis()
        val joda = DateTime(current)

        return TimePickerDialog(activity, this, joda.hourOfDay, joda.minuteOfHour, DateFormat.is24HourFormat(activity))
    }

    override fun onTimeSet(view: TimePicker, hourOfDay: Int, minute: Int) {
        val set = DateTime.now().withHourOfDay(hourOfDay).withMinuteOfHour(minute)
        val result = if (set.isBeforeNow)
            set.plusDays(1).millis
        else
            set.millis

        arguments
            ?.getString(PARAM_REQ_KEY)
            ?.also { setFragmentResult(it, bundleOf(RESULT_TIME to result)) }
    }

    companion object {
        val RESULT_TIME = "${TimePickerFragment::class.java}.time"

        fun newInstance(reqKey: String, currentMs: Long): TimePickerFragment {
            val args = Bundle().apply {
                putString(PARAM_REQ_KEY, reqKey)
                putLong(PARAM_CURRENT_MS, currentMs)
            }
            return TimePickerFragment().apply {
                arguments = args
            }
        }

        private val PARAM_REQ_KEY = "${TimePickerFragment::class.java}.req_key"
        private val PARAM_CURRENT_MS = "${TimePickerFragment::class.java}.current_ms"
    }
}