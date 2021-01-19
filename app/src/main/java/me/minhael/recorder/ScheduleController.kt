package me.minhael.recorder

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.runBlocking
import me.minhael.android.Services
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat
import org.koin.core.component.KoinApiExtension
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit
import kotlin.math.max

class ScheduleController(private val workManager: WorkManager) {

    @KoinApiExtension
    fun activate(timestamp: Long, durationMs: Long, periodMs: Long = 24 * 60 * 60 * 1000) {
        val current = System.currentTimeMillis()
        val delays = max(timestamp - current, 0)
        val data = Data.Builder().putLong(PARAM_DURATION_MS, durationMs).build()
        val request = PeriodicWorkRequestBuilder<ActivateWorker>(periodMs, TimeUnit.MILLISECONDS)
            .setInitialDelay(delays, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()
        workManager.enqueueUniquePeriodicWork(WORK_ACTIVATE, ExistingPeriodicWorkPolicy.REPLACE, request)
    }

    fun deactivate() {
        workManager.cancelUniqueWork(WORK_ACTIVATE)
    }

    fun lastActivation(): Pair<Long, Long>? {
        return runBlocking {
            workManager.getWorkInfosForUniqueWork(WORK_ACTIVATE).await().getOrNull(0)
                ?.outputData?.run {
                    if (hasKeyWithValueOfType<Long>(RESULT_LAST_ACTIVATION) && hasKeyWithValueOfType<Long>(PARAM_DURATION_MS))
                        getLong(RESULT_LAST_ACTIVATION, 0) to getLong(PARAM_DURATION_MS, 0)
                    else
                        null
                }
        }
    }

    @KoinApiExtension
    class ActivateWorker(
        private val context: Context,
        workerParameters: WorkerParameters
    ): Worker(context, workerParameters), KoinComponent {
        override fun doWork(): Result {
            val storage: Storage by inject()
            val workManager: WorkManager by inject()

            Services.start<Recorder>(context, RecorderService::class.java) { recorder ->
                if (!recorder.isRecording()) {
                    val request = OneTimeWorkRequestBuilder<DeactivateWorker>()
                        .setInitialDelay(inputData.getLong(PARAM_DURATION_MS, 8 * 60 * 60 * 1000), TimeUnit.MILLISECONDS)
                        .build()
                    workManager.enqueueUniqueWork(WORK_DEACTIVATE, ExistingWorkPolicy.REPLACE, request)
                    recorder.record(storage.dirCache, "Record@${FORMAT_TIME.print(LocalDateTime())}")
                }
            }

            return Result.success(
                Data.Builder()
                    .putLong(PARAM_DURATION_MS, inputData.getLong(PARAM_DURATION_MS, 8 * 60 * 60 * 1000))
                    .putLong(RESULT_LAST_ACTIVATION, System.currentTimeMillis())
                    .build()
            )
        }
    }

    @KoinApiExtension
    class DeactivateWorker(
        private val context: Context,
        workerParameters: WorkerParameters
    ): Worker(context, workerParameters), KoinComponent {
        override fun doWork(): Result {
            Services.start<Recorder>(context, RecorderService::class.java) { recorder ->
                recorder.stop()
            }
            return Result.success()
        }
    }

    companion object {
        private val FORMAT_TIME = DateTimeFormat.forPattern("yyyyMMddHHmmss")

        private val WORK_ACTIVATE = "${ScheduleController::class.java}.activate"
        private val WORK_DEACTIVATE = "${ScheduleController::class.java}.deactivate"

        private val PARAM_DURATION_MS = "${ScheduleController::class.java}.duration_ms"
        private val RESULT_LAST_ACTIVATION = "${ScheduleController::class.java}.last_activation"
    }
}