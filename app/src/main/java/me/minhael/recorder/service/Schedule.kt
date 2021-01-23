package me.minhael.recorder.service

import me.minhael.design.job.JobScheduler
import me.minhael.design.props.Props
import me.minhael.recorder.PropTags
import org.joda.time.format.DateTimeFormat
import org.koin.core.component.KoinApiExtension
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class Schedule(
    private val props: Props,
    private val scheduler: JobScheduler
) {

    @KoinApiExtension
    fun activate() {
        val timestamp = props.get(PropTags.RECORDING_TIME_START, PropTags.RECORDING_TIME_START_DEFAULT)
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
            val scheduler: JobScheduler by inject()
            val recording: Recording by inject()

            scheduler.set(WORK_DEACTIVATE, JobScheduler.OneShot(System.currentTimeMillis() + durationMs)) {
                Deactivate()
            }

            recording.start()

            return true
        }
    }

    @KoinApiExtension
    class Deactivate : JobScheduler.Job, KoinComponent {
        override fun execute(): Boolean {
            val recording: Recording by inject()
            recording.stop()
            return true
        }
    }

    companion object {
        private val WORK_ACTIVATE = "${Schedule::class.simpleName}.activate"
        private val WORK_DEACTIVATE = "${Schedule::class.simpleName}.deactivate"
    }
}