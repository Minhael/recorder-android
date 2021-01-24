package me.minhael.recorder

import androidx.work.PeriodicWorkRequest

object PropTags {
    const val DIR_RECORDING = "dir.recording"

    const val RECORDING_DAILY = "recording.daily"
    const val RECORDING_DAILY_DEFAULT = false

    const val RECORDING_TIME_START = "recording.time.start"
    const val RECORDING_TIME_START_DEFAULT = 1609513200000L

    const val RECORDING_TIME_END = "recording.time.end"
    const val RECORDING_TIME_END_DEFAULT = 1609545600000L

    const val RECORDING_PERIOD_MS = "recording.period.ms"
    const val RECORDING_PERIOD_MS_DEFAULT = 24 * 60 * 60 * 1000L
}