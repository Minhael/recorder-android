package me.minhael.recorder

object PropTags {
    const val DIR_RECORDING = "dir.recording"

    const val RECORDING_FILE_PATTERN = "recording.file.pattern"
    const val RECORDING_FILE_PATTERN_DEFAULT = "yyyy-MM-dd HH-mm-ss"

    const val SCHEDULE_DAILY = "schedule.daily"
    const val SCHEDULE_DAILY_DEFAULT = false

    const val SCHEDULE_TIME_START = "schedule.time.start"
    const val SCHEDULE_TIME_START_DEFAULT = 1609513200000L

    const val SCHEDULE_TIME_END = "schedule.time.end"
    const val SCHEDULE_TIME_END_DEFAULT = 1609545600000L

    const val SCHEDULE_PERIOD_MS = "schedule.period.ms"
    const val SCHEDULE_PERIOD_MS_DEFAULT = 24 * 60 * 60 * 1000L

    const val MEASURE_PERIOD_UPDATE_MS = "measure.period.update.ms"
    const val MEASURE_PERIOD_UPDATE_MS_DEFAULT = 1000L
}