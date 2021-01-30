package me.minhael.recorder.service

import me.minhael.design.job.Jobs
import me.minhael.design.props.Props
import me.minhael.recorder.PropTags
import org.joda.time.DateTime
import org.koin.core.component.KoinApiExtension
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory

class Schedule(
    private val props: Props,
    private val scheduler: Jobs
) {

    @KoinApiExtension
    fun activate() {
        val now = DateTime()

        val startTime = DateTime(props.get(PropTags.SCHEDULE_TIME_START, PropTags.SCHEDULE_TIME_START_DEFAULT))
        val endTime = DateTime(props.get(PropTags.SCHEDULE_TIME_END, PropTags.SCHEDULE_TIME_END_DEFAULT))

        logger.debug("Schedule settings\nStartTime = {}\nEndTime = {}\nDuration = {}", startTime, endTime)

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
        val nextEnd = nextStart
            .withHourOfDay(endTime.hourOfDay)
            .withMinuteOfHour(endTime.minuteOfHour)
            .let {
                if (it.isBefore(nextStart))
                    it.plusDays(1)
                else
                    it
            }
        val duration = nextEnd.millis - nextStart.millis
        val periodMs = props.get(PropTags.SCHEDULE_PERIOD_MS, PropTags.SCHEDULE_PERIOD_MS_DEFAULT)

        logger.debug("Computed schedule\nNextStart = {}\nNextEnd = {}\nDuration = {} Period = {}", nextStart, nextEnd, duration, periodMs)

        scheduler.set(WORK_ACTIVATE, Jobs.Periodic(nextStart.millis, periodMs)) { Activate(duration) }
    }

    fun deactivate() {
        scheduler.remove(WORK_ACTIVATE)
    }

    fun manualStop() {
        scheduler.remove(WORK_DEACTIVATE)
    }

    @KoinApiExtension
    class Activate(private val duration: Long) : Jobs.Job, KoinComponent {
        override fun execute(): Boolean {
            logger.info("Start recording in background")

            val scheduler: Jobs by inject()
            val recording: Recording by inject()

            scheduler.set(WORK_DEACTIVATE, Jobs.OneShot(System.currentTimeMillis() + duration)) { Deactivate() }
            recording.start()

            return true
        }
    }

    @KoinApiExtension
    class Deactivate : Jobs.Job, KoinComponent {
        override fun execute(): Boolean {
            logger.info("End background recording")

            val recording: Recording by inject()
            recording.stop()
            recording.save()
            return true
        }
    }

    companion object {
        private val WORK_ACTIVATE = "${Schedule::class.simpleName}.activate"
        private val WORK_DEACTIVATE = "${Schedule::class.simpleName}.deactivate"

        private val logger = LoggerFactory.getLogger(Schedule::class.java)
    }
}