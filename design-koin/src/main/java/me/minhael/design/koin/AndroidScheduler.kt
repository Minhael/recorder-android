package me.minhael.design.koin

import android.content.Context
import androidx.work.*
import me.minhael.design.job.JobScheduler
import me.minhael.design.sl.Serializer
import me.minhael.design.x.deserialize
import org.koin.core.component.KoinApiExtension
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.max

@KoinApiExtension
class AndroidScheduler(private val workManager: WorkManager, private val serializer: Serializer) : JobScheduler {

    override fun set(name: String, trigger: JobScheduler.Trigger, builder: () -> JobScheduler.Job) {
        trigger.visit(provider, name, builder)
    }

    override fun remove(name: String) {
        logger.info("Remove {}", name)
        workManager.cancelUniqueWork(name)
    }

    private val provider = object : JobScheduler.TriggerProvider {
        override fun setup(
            trigger: JobScheduler.Trigger,
            name: String,
            builder: () -> JobScheduler.Job
        ) {
            val bJob = ByteArrayOutputStream().use {
                serializer.serialize(Builder(builder), it)
                it.toByteArray()
            }
            logger.info("Run {} now", name)
            workManager.enqueueUniqueWork(
                name,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<JobWorker>()
                    .setInputData(Data.Builder().putByteArray(DATA_JOB, bJob).build())
                    .build()
            )
        }

        override fun setup(
            trigger: JobScheduler.OneShot,
            name: String,
            builder: () -> JobScheduler.Job
        ) {
            val bJob = ByteArrayOutputStream().use {
                serializer.serialize(Builder(builder), it)
                it.toByteArray()
            }
            val delayMs = max(0, trigger.timestamp - System.currentTimeMillis())
            logger.info("Run {} after {}ms", name, delayMs)
            workManager.enqueueUniqueWork(
                name,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<JobWorker>()
                    .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                    .setInputData(Data.Builder().putByteArray(DATA_JOB, bJob).build())
                    .build()
            )
        }

        override fun setup(
            trigger: JobScheduler.Periodic,
            name: String,
            builder: () -> JobScheduler.Job
        ) {
            val bJob = ByteArrayOutputStream().use {
                serializer.serialize(Builder(builder), it)
                it.toByteArray()
            }

            val delayMs = max(0, trigger.timestamp - System.currentTimeMillis())
            if (trigger.flexMs < PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS) {
                logger.info("Run {} after {}ms every {}ms", name, delayMs, trigger.periodMs)
                workManager.enqueueUniquePeriodicWork(
                    name,
                    ExistingPeriodicWorkPolicy.REPLACE,
                    PeriodicWorkRequestBuilder<JobWorker>(trigger.periodMs, TimeUnit.MILLISECONDS)
                        .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                        .setInputData(Data.Builder().putByteArray(DATA_JOB, bJob).build())
                        .build()
                )
            } else {
                val periodMs = max(PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS, trigger.periodMs)
                val flexMs = max(PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS, trigger.flexMs)
                logger.info("Run {} after {}ms every {}ms +- {}ms", name, delayMs, periodMs, flexMs)
                workManager.enqueueUniquePeriodicWork(
                    name,
                    ExistingPeriodicWorkPolicy.REPLACE,
                    PeriodicWorkRequestBuilder<JobWorker>(periodMs, TimeUnit.MILLISECONDS, flexMs, TimeUnit.MILLISECONDS)
                        .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                        .setInputData(Data.Builder().putByteArray(DATA_JOB, bJob).build())
                        .build()
                )
            }
        }

        override fun setup(
            trigger: JobScheduler.Boot,
            name: String,
            builder: () -> JobScheduler.Job
        ) {
            TODO("Not yet implemented")
        }
    }

    internal class JobWorker(
        context: Context,
        workerParameters: WorkerParameters
    ) : Worker(context, workerParameters), KoinComponent {
        override fun doWork(): Result {
            val deserializer: Serializer by inject()

            val bJob = inputData.getByteArray(DATA_JOB)
            val job = deserializer.deserialize<Builder>(ByteArrayInputStream(bJob))

            return when (job.builder().execute()) {
                true -> Result.success()
                false -> Result.failure()
                else -> Result.retry()
            }
        }
    }

    private data class Builder(val builder: () -> JobScheduler.Job)

    companion object {
        private val DATA_JOB = "${AndroidScheduler::class.java}.job"

        private val logger = LoggerFactory.getLogger(AndroidScheduler::class.java)
    }
}