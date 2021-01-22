package me.minhael.recorder

import androidx.work.PeriodicWorkRequest

object PropTags {
    const val DIR_RECORDING = "dir.recording"

    const val RECORDING_DAILY = "recording.daily"
    const val RECORDING_DAILY_DEFAULT = false

    const val RECORDING_TIME_START = "recording.time.start"
    const val RECORDING_TIME_START_DEFAULT = 0L

    const val RECORDING_DURATION_MS = "recording.duration.ms"
    const val RECORDING_DURATION_MS_DEFAULT = 5 * 1000L

    const val RECORDING_PERIOD_MS = "recording.period.ms"
    const val RECORDING_PERIOD_MS_DEFAULT = PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS
}