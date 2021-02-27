package me.minhael.recorder.service

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
import kotlin.math.max

class Schedule(
    private val props: Props,
    private val scheduler: JobManager
) {

    @KoinApiExtension
    fun activate() {
        val nextStart = nextStart(props)
        logger.debug("Activate = {}", nextStart)

        val cron = "0 ${nextStart.minuteOfHour} ${nextStart.hourOfDay} * * ?"
        scheduler.set(WORK_ACTIVATE, JobTrigger.Cron(cron), ActivateJob())
    }

    fun deactivate() {
        scheduler.remove(WORK_ACTIVATE)
    }

    fun manualStopped() {
        scheduler.remove(WORK_DEACTIVATE)
    }

    class ActivateJob : Job {
        @KoinApiExtension
        override fun build(): Job.Task {
            return Activate()
        }
    }

    class DeactivateJob: Job {
        @KoinApiExtension
        override fun build(): Job.Task {
            return Deactivate()
        }
    }

    @KoinApiExtension
    class Activate : Job.Task, KoinComponent {
        override fun execute(): Boolean {
            logger.info("Start recording in background")

            val props: Props by inject()
            val scheduler: JobManager by inject()
            val session: Session by inject()

            val now = DateTime.now()
            val nextEnd = nextEnd(props, now)

            logger.debug("Deactivate = {}", nextEnd)
            val duration = max(1000L * 60, nextEnd.millis - now.millis)
            logger.debug("after {}", duration)

            scheduler.set(WORK_DEACTIVATE, JobTrigger.OneShot(duration), DeactivateJob())
            session.start()

            return true
        }
    }

    @KoinApiExtension
    class Deactivate : Job.Task, KoinComponent {
        override fun execute(): Boolean {
            logger.info("End background recording")

            val session: Session by inject()
            session.stop()

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