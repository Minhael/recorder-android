package me.minhael.recorder.controller

import android.content.Context
import me.minhael.design.android.Services
import me.minhael.design.job.JobScheduler
import me.minhael.design.props.Props
import me.minhael.recorder.PropTags
import me.minhael.recorder.Recorder
import me.minhael.recorder.component.RecorderService
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat
import org.koin.core.component.KoinApiExtension
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ScheduleController(
    private val props: Props,
    private val scheduler: JobScheduler
) {

    @KoinApiExtension
    fun activate() {
//        val timestamp = props.get(PropTags.RECORDING_TIME_START, PropTags.RECORDING_TIME_START_DEFAULT)
        val timestamp = System.currentTimeMillis() + 5 * 1000L
        val durationMs = props.get(PropTags.RECORDING_DURATION_MS, PropTags.RECORDING_DURATION_MS_DEFAULT)
        val periodMs = props.get(PropTags.RECORDING_PERIOD_MS, PropTags.RECORDING_PERIOD_MS_DEFAULT)

        scheduler.set(WORK_ACTIVATE, JobScheduler.Periodic(timestamp, periodMs)) {
            Activate(durationMs)
        }
    }

    fun deactivate() {
        scheduler.remove(WORK_ACTIVATE)
    }

    @KoinApiExtension
    class Activate(private val durationMs: Long) : JobScheduler.Job, KoinComponent {
        override fun execute(): Boolean {
            val storage: Storage by inject()
            val context: Context by inject()
            val scheduler: JobScheduler by inject()

            Services.startForeground<Recorder>(context, RecorderService::class.java) {
                if (!it.isRecording()) {
                    scheduler.set(WORK_DEACTIVATE, JobScheduler.OneShot(System.currentTimeMillis() + durationMs)) {
                        Deactivate()
                    }
                    it.record(storage.dirCache, "Record@${FORMAT_TIME.print(LocalDateTime())}")
                }
            }

            return true
        }
    }

    @KoinApiExtension
    class Deactivate : JobScheduler.Job, KoinComponent {
        override fun execute(): Boolean {
            val context: Context by inject()
            Services.startForeground<Recorder>(context, RecorderService::class.java) {
                it.stop()
                it.close()
            }
            return true
        }
    }

    companion object {
        private val FORMAT_TIME = DateTimeFormat.forPattern("yyyyMMddHHmmss")

        private val WORK_ACTIVATE = "${ScheduleController::class.java}.activate"
        private val WORK_DEACTIVATE = "${ScheduleController::class.java}.deactivate"
    }
}