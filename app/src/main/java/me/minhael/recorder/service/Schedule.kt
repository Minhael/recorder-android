package me.minhael.recorder.service

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.minhael.design.job.Job
import me.minhael.design.job.JobManager
import me.minhael.design.job.JobTrigger
import me.minhael.design.props.Props
import me.minhael.recorder.PropTags
import org.joda.time.DateTime
import org.koin.core.component.KoinApiExtension
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory

class Schedule(
    private val props: Props,
    private val scheduler: JobManager
) {

    @KoinApiExtension
    fun activate() {
        val nextStart = nextStart(props)
        val nextEnd = nextEnd(props, nextStart)

        logger.debug("Computed schedule\nNextStart = {}\nNextEnd = {}\nDuration = {} Period = {}", nextStart, nextEnd)

        val cron = "0 ${nextStart.minuteOfHour} ${nextStart.hourOfDay} * * ?"

        scheduler.set(WORK_ACTIVATE, JobTrigger.Cron(cron)) { Activate(nextEnd.millis) }
    }

    fun deactivate() {
        scheduler.remove(WORK_ACTIVATE)
    }

    fun manualStopped() {
        scheduler.remove(WORK_DEACTIVATE)
    }

    @KoinApiExtension
    class Activate(private val endTime: Long) : Job.Task, KoinComponent {
        override fun execute(): Boolean? {
            logger.info("Start recording in background")

            val scheduler: JobManager by inject()
            val recording: Recording by inject()
            val now = System.currentTimeMillis()

            if (endTime > now) {
                scheduler.set(WORK_DEACTIVATE, JobTrigger.OneShot(endTime - now)) { Deactivate() }
                GlobalScope.launch { recording.start() }
            }

            return true
        }
    }

    @KoinApiExtension
    class Deactivate : Job.Task, KoinComponent {
        override fun execute(): Boolean? {
            logger.info("End background recording")

            val recording: Recording by inject()
            GlobalScope.launch { recording.stop() }

            return true
        }
    }

    companion object {
        private val WORK_ACTIVATE = "${Schedule::class.simpleName}.activate"
        private val WORK_DEACTIVATE = "${Schedule::class.simpleName}.deactivate"

        private val logger = LoggerFactory.getLogger(Schedule::class.java)

        private fun nextStart(props: Props): DateTime {
            val now = DateTime()
            val startTime = DateTime(props.get(PropTags.SCHEDULE_TIME_START, PropTags.SCHEDULE_TIME_START_DEFAULT))
            return now
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
        }

        private fun nextEnd(props: Props, nextStart: DateTime = nextStart(props)): DateTime {
            val endTime = DateTime(props.get(PropTags.SCHEDULE_TIME_END, PropTags.SCHEDULE_TIME_END_DEFAULT))
            return nextStart
                .withHourOfDay(endTime.hourOfDay)
                .withMinuteOfHour(endTime.minuteOfHour)
                .let {
                    if (it.isBefore(nextStart))
                        it.plusDays(1)
                    else
                        it
                }
        }
    }
}