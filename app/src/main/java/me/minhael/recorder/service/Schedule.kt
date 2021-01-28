package me.minhael.recorder.service

import me.minhael.design.job.Jobs
import me.minhael.design.props.Props
import me.minhael.recorder.PropTags
import org.joda.time.DateTime
import org.koin.core.component.KoinApiExtension
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.max

class Schedule(
    private val props: Props,
    private val scheduler: Jobs
) {

    @KoinApiExtension
    fun activate() {
        val now = DateTime()

        val startTime = DateTime(props.get(PropTags.RECORDING_TIME_START, PropTags.RECORDING_TIME_START_DEFAULT))
        val nextStart = now
            .withHourOfDay(startTime.hourOfDay)
            .withMinuteOfHour(startTime.minuteOfHour)
            .withSecondOfMinute(0)
            .withMillisOfSecond(0)
            .let {
                if (it.isBefore(now))
                    it.plusDays(1)
                else
                    it
            }

        val nextEnd = DateTime(props.get(PropTags.RECORDING_TIME_END, PropTags.RECORDING_TIME_END_DEFAULT)).run {
            nextStart.plus(max(0, millis - startTime.millis))
        }

        val periodMs = props.get(PropTags.RECORDING_PERIOD_MS, PropTags.RECORDING_PERIOD_MS_DEFAULT)

        scheduler.set(WORK_ACTIVATE, Jobs.Periodic(nextStart.millis, periodMs)) { Activate(nextEnd.millis) }
    }

    fun deactivate() {
        scheduler.remove(WORK_ACTIVATE)
    }

    fun manualStop() {
        scheduler.remove(WORK_DEACTIVATE)
    }

    @KoinApiExtension
    class Activate(private val endTime: Long) : Jobs.Job, KoinComponent {
        override fun execute(): Boolean {
            val scheduler: Jobs by inject()
            val recording: Recording by inject()

            scheduler.set(WORK_DEACTIVATE, Jobs.OneShot(endTime)) { Deactivate() }
            recording.start()

            return true
        }
    }

    @KoinApiExtension
    class Deactivate : Jobs.Job, KoinComponent {
        override fun execute(): Boolean {
            val recording: Recording by inject()
            recording.stop()
            recording.save()
            return true
        }
    }

    companion object {
        private val WORK_ACTIVATE = "${Schedule::class.simpleName}.activate"
        private val WORK_DEACTIVATE = "${Schedule::class.simpleName}.deactivate"
    }
}