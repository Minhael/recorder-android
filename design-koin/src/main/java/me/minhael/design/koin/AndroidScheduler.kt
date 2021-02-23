package me.minhael.design.koin

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.*
import me.minhael.design.job.Job
import me.minhael.design.job.JobManager
import me.minhael.design.job.JobTrigger
import me.minhael.design.sl.Serializer
import me.minhael.design.x.deserialize
import org.joda.time.DateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.quartz.CronExpression
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.Serializable
import java.util.concurrent.TimeUnit
import kotlin.math.max

class AndroidScheduler(
    private val workManager: WorkManager,
    private val bootJobs: JobQueue,
    private val serializer: Serializer
) : JobManager {

    override fun set(name: String, trigger: JobTrigger, job: Job): String {
        return trigger.visit(scheduler, name, job)
    }

    override fun remove(name: String) {
        logger.info("Remove {}", name)
        workManager.cancelUniqueWork(name)
        bootJobs.remove(name)
    }

    internal fun purgeBootJobs(dispatcher: CoroutineDispatcher = Dispatchers.IO, scope: CoroutineScope = GlobalScope) {
        bootJobs.peek().forEach { (uuid, job) ->
            scope.launch(dispatcher) {
                when (job.build().execute()) {
                    true, false -> bootJobs.removeIds(uuid)
                }
            }
        }
    }

    private val scheduler = object : JobManager.Scheduler {

        override fun setup(trigger: JobTrigger, name: String, job: Job): String {
            val bJob = ByteArrayOutputStream().use {
                serializer.serialize(Wrapper(job), it)
                it.toByteArray()
            }
            logger.info("Run {} now", name)
            val request = OneTimeWorkRequestBuilder<JobWorker>()
                .setInputData(
                    Data.Builder()
                        .putByteArray(DATA_JOB, bJob)
                        .build()
                )
                .build()
            workManager.enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, request)
            return request.id.toString()
        }

        override fun setup(trigger: JobTrigger.OneShot, name: String, job: Job): String {
            val bJob = ByteArrayOutputStream().use {
                serializer.serialize(Wrapper(job), it)
                it.toByteArray()
            }
            val delayMs = trigger.afterMs
            logger.info("Run {} after {}ms", name, delayMs)
            val request = OneTimeWorkRequestBuilder<JobWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setInputData(
                    Data.Builder()
                        .putByteArray(DATA_JOB, bJob)
                        .build()
                )
                .build()
            workManager.enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, request)
            return request.id.toString()
        }

        override fun setup(trigger: JobTrigger.Periodic, name: String, job: Job): String {
            val bJob = ByteArrayOutputStream().use {
                serializer.serialize(Wrapper(job), it)
                it.toByteArray()
            }

            val delayMs = trigger.startAfterMs
            val request = if (trigger.flexMs < PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS) {
                logger.info("Run {} after {}ms every {}ms", name, delayMs, trigger.periodMs)
                PeriodicWorkRequestBuilder<JobWorker>(trigger.periodMs, TimeUnit.MILLISECONDS)
                    .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                    .setInputData(
                        Data.Builder()
                            .putByteArray(DATA_JOB, bJob)
                            .build()
                    )
                    .build()
            } else {
                val periodMs = max(PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS, trigger.periodMs)
                val flexMs = max(PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS, trigger.flexMs)
                logger.info("Run {} after {}ms every {}ms +- {}ms", name, delayMs, periodMs, flexMs)
                PeriodicWorkRequestBuilder<JobWorker>(periodMs, TimeUnit.MILLISECONDS, flexMs, TimeUnit.MILLISECONDS)
                    .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                    .setInputData(
                        Data.Builder()
                            .putByteArray(DATA_JOB, bJob)
                            .build()
                    )
                    .build()
            }

            workManager.enqueueUniquePeriodicWork(name, ExistingPeriodicWorkPolicy.REPLACE, request)
            return request.id.toString()
        }

        override fun setup(trigger: JobTrigger.Cron, name: String, job: Job): String {
            return setup(
                JobTrigger.OneShot(getDelaysFromNow(trigger.expression)),
                name,
                ConsecutiveWrapper(trigger.expression, name, job)
            )
        }

        override fun setup(trigger: JobTrigger.Boot, name: String, job: Job): String {
            return bootJobs.append(name, job)
        }
    }

    internal class JobWorker(
        context: Context,
        workerParameters: WorkerParameters
    ) : Worker(context, workerParameters), KoinComponent {
        override fun doWork(): Result {
            val deserializer: Serializer by inject()

            val bJob = inputData.getByteArray(DATA_JOB)
            val wrapper = deserializer.deserialize<Wrapper>(ByteArrayInputStream(bJob))

            return when (wrapper.job.build().execute()) {
                true -> Result.success()
                false -> Result.failure()
                else -> Result.retry()
            }
        }
    }

    private data class Wrapper(val job: Job) : Serializable

    private class ConsecutiveWrapper(private val cron: String, private val name: String, private val job: Job): Job {
        override fun build(): Job.Task {
            return ConsecutiveTask(cron, name, job)
        }
    }

    private class ConsecutiveTask(
        private val cron: String,
        private val name: String,
        private val job: Job
    ): Job.Task, KoinComponent {

        override fun execute(): Boolean? {
            val jobs: JobManager by inject()

            val rt = job.build().execute()

            when (rt) {
                true, null -> jobs.set(
                    name, JobTrigger.OneShot(getDelaysFromNow(cron)), ConsecutiveWrapper(cron, name, job)
                )
            }

            return rt
        }
    }

    companion object {
        private val DATA_JOB = "${AndroidScheduler::class.java}.job"

        private val logger = LoggerFactory.getLogger(AndroidScheduler::class.java)

        private fun getDelaysFromNow(cron: String): Long {
            val exp = CronExpression(cron)
            val now = DateTime.now()
            return exp.getNextValidTimeAfter(now.toDate()).time - now.millis
        }
    }
}