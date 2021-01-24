package me.minhael.recorder.service

import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import me.minhael.design.job.Jobs
import me.minhael.design.props.Props
import me.minhael.recorder.PropTags
import org.joda.time.DateTimeUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.koin.core.component.KoinApiExtension
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.KoinTest
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

internal class ScheduleTest : AutoCloseKoinTest() {

    @Mock
    lateinit var props: Props

    @Mock
    lateinit var jobs: Jobs

    @Mock
    lateinit var recording: Recording

    private val module = module {
        factory { props }
        factory { jobs }
        factory { recording }
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        `when`(props.get(eq(PropTags.RECORDING_PERIOD_MS), any())).thenReturn(PropTags.RECORDING_PERIOD_MS_DEFAULT)

        startKoin { modules(module) }
    }

    @After
    fun tearDown() {
    }

    @KoinApiExtension
    @Test
    fun activate() {
        DateTimeUtils.setCurrentMillisFixed(sameDay)

        `when`(props.get(eq(PropTags.RECORDING_TIME_START), any())).thenReturn(PropTags.RECORDING_TIME_START_DEFAULT)
        `when`(props.get(eq(PropTags.RECORDING_TIME_END), any())).thenReturn(PropTags.RECORDING_TIME_END_DEFAULT)

        val schedule = Schedule(props, jobs)
        val name = argumentCaptor<String>()
        val trigger = argumentCaptor<Jobs.Trigger>()
        val job = argumentCaptor<() -> Jobs.Job>()

        schedule.activate()
        verify(jobs).set(name.capture(), trigger.capture(), job.capture())
        reset(jobs)

        job.firstValue().execute()
        verify(jobs).set(name.capture(), trigger.capture(), job.capture())
        verify(recording).start()
        verify(recording, never()).stop()

        job.secondValue().execute()
        verify(recording).stop()

        schedule.deactivate()
        verify(jobs).remove(eq(name.firstValue))
        verify(jobs, never()).remove(eq(name.secondValue))

        val aTrigger = trigger.firstValue
        assertTrue(aTrigger is Jobs.Periodic)
        (aTrigger as Jobs.Periodic).apply {
            assertEquals(sameStart, timestamp)
            assertEquals(PropTags.RECORDING_PERIOD_MS_DEFAULT, periodMs)
            assertEquals(PropTags.RECORDING_PERIOD_MS_DEFAULT, flexMs)
        }

        val bTrigger = trigger.secondValue
        assertTrue(bTrigger is Jobs.OneShot)
        (bTrigger as Jobs.OneShot).apply {
            assertEquals(sameEnd, timestamp)
        }
    }

    @KoinApiExtension
    @Test
    fun activateNextDay() {
        DateTimeUtils.setCurrentMillisFixed(nextDay)

        `when`(props.get(eq(PropTags.RECORDING_TIME_START), any())).thenReturn(PropTags.RECORDING_TIME_START_DEFAULT)
        `when`(props.get(eq(PropTags.RECORDING_TIME_END), any())).thenReturn(PropTags.RECORDING_TIME_END_DEFAULT)

        val schedule = Schedule(props, jobs)
        val name = argumentCaptor<String>()
        val trigger = argumentCaptor<Jobs.Trigger>()
        val job = argumentCaptor<() -> Jobs.Job>()

        schedule.activate()
        verify(jobs).set(name.capture(), trigger.capture(), job.capture())
        reset(jobs)

        job.firstValue().execute()
        verify(jobs).set(name.capture(), trigger.capture(), job.capture())
        verify(recording).start()
        verify(recording, never()).stop()

        job.secondValue().execute()
        verify(recording).stop()

        schedule.deactivate()
        verify(jobs).remove(eq(name.firstValue))
        verify(jobs, never()).remove(eq(name.secondValue))

        val aTrigger = trigger.firstValue
        assertTrue(aTrigger is Jobs.Periodic)
        (aTrigger as Jobs.Periodic).apply {
            assertEquals(nextStart, timestamp)
            assertEquals(PropTags.RECORDING_PERIOD_MS_DEFAULT, periodMs)
            assertEquals(PropTags.RECORDING_PERIOD_MS_DEFAULT, flexMs)
        }

        val bTrigger = trigger.secondValue
        assertTrue(bTrigger is Jobs.OneShot)
        (bTrigger as Jobs.OneShot).apply {
            assertEquals(nextEnd, timestamp)
        }
    }

    companion object {

        private const val sameDay = 1611451206561
        private const val sameStart = 1611500400000
        private const val sameEnd = 1611532800000

        private const val nextDay = 1611500401000
        private const val nextStart = 1611586800000
        private const val nextEnd = 1611619200000
    }
}