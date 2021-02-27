package me.minhael.design.koin

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import me.minhael.design.job.Job
import me.minhael.design.job.JobManager
import me.minhael.design.sl.Serializer
import me.minhael.design.test.JobManagerTest
import org.joda.time.DateTimeUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.koin.core.component.KoinApiExtension
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import org.koin.test.inject
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

@ExtendWith(MockitoExtension::class)
internal class AndroidSchedulerTest : KoinTest, JobManagerTest {

    @Mock
    private lateinit var serializer: Serializer

    override val subject: AndroidScheduler by inject()

    override fun await(name: String, millis: Long, iteration: Long) {
        WorkManagerTestInitHelper.getTestDriver(InstrumentationRegistry.getInstrumentation().targetContext)?.apply {
            val manager = WorkManager.getInstance(InstrumentationRegistry.getInstrumentation().targetContext)
            manager.getWorkInfosForUniqueWork(name).get().forEach {
                try {
                    if (iteration == 0L)
                        setInitialDelayMet(it.id)
                    else
                        setPeriodDelayMet(it.id)
                } catch (e: IllegalArgumentException) {
                    if (it.state != WorkInfo.State.CANCELLED)
                        throw e
                }
            }
        }
    }

    @KoinApiExtension
    override fun hotBoot() {
        subject.purgeBootJobs(Dispatchers.Unconfined)
    }

    override fun advanceTo(name: String, timestamp: Long, isAfterTrigger: Boolean) {
        DateTimeUtils.setCurrentMillisFixed(timestamp)
        if (isAfterTrigger)
            WorkManagerTestInitHelper.getTestDriver(InstrumentationRegistry.getInstrumentation().targetContext)?.apply {
                val manager = WorkManager.getInstance(InstrumentationRegistry.getInstrumentation().targetContext)
                manager.getWorkInfosForUniqueWork(name).get().forEach {
                    try {
                        setInitialDelayMet(it.id)
                    } catch (e: IllegalArgumentException) {
                        if (it.state != WorkInfo.State.CANCELLED)
                            throw e
                    }
                }
            }
    }

    @BeforeEach
    fun setup() {
        //  Configure WorkManager for test
        val config = Configuration.Builder().setMinimumLoggingLevel(Log.DEBUG).setExecutor(SynchronousExecutor()).build()
        WorkManagerTestInitHelper.initializeTestWorkManager(InstrumentationRegistry.getInstrumentation().targetContext, config)

        //  Mocking serializer
        val jobCapture2 = argumentCaptor<Job>()
        lenient().doNothing().whenever(serializer).serialize(jobCapture2.capture(), any())
        lenient().`when`(serializer.deserialize(any(), any<Class<*>>())).thenAnswer { jobCapture2.lastValue }

        //  Test injector
        startKoin {
            modules(
                module {
                    factory { serializer }
                    single<JobQueue> { MockQueue() }
                    factory { WorkManager.getInstance(InstrumentationRegistry.getInstrumentation().targetContext) }
                    factory { AndroidScheduler(get(), get(), get()) } bind JobManager::class
                }
            )
        }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    fun dummy() {
        get<JobManager>()
    }

    private class MockQueue: JobQueue {
        private val map = HashMap<String, Job>()

        override fun append(name: String, job: Job): String {
            map[name] = job
            return name
        }

        override fun remove(name: String) {
            map.remove(name)
        }

        override fun peek(): List<Pair<String, Job>> {
            return map.map { (name, job) -> name to job }
        }

        override fun removeIds(vararg uuid: String) {
            uuid.forEach { remove(it) }
        }
    }
}