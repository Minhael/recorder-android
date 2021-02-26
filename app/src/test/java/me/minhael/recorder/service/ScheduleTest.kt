package me.minhael.recorder.service

import me.minhael.design.job.JobManager
import me.minhael.design.props.Props
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
internal class ScheduleTest : AutoCloseKoinTest() {

    @Mock
    lateinit var props: Props

    @Mock
    lateinit var jm: JobManager

    @Mock
    lateinit var recording: Recording

    private val module = module {
        factory { props }
        factory { jm }
        factory { recording }
    }

    @Before
    fun setUp() {
        startKoin { modules(module) }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun activate() {
    }

    @Test
    fun activateNextDay() {
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