package me.minhael.recorder.component

import android.app.Application
import androidx.work.WorkManager
import me.minhael.design.android.AndroidProps
import me.minhael.design.android.AndroidUriAccessor
import me.minhael.design.fs.OkUriAccessor
import me.minhael.design.fs.Uri
import me.minhael.design.job.JobScheduler
import me.minhael.design.props.Props
import me.minhael.design.sl.FstSerializer
import me.minhael.design.sl.Serializer
import me.minhael.recorder.*
import me.minhael.recorder.controller.RecordController
import me.minhael.recorder.controller.ScheduleController
import me.minhael.recorder.controller.Storage
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinApiExtension
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.slf4j.LoggerFactory

class MainApplication : Application() {

    @KoinApiExtension
    override fun onCreate() {
        super.onCreate()

        logger.debug("Insert Koin")
        startKoin {
            androidLogger()
            androidContext(this@MainApplication)
            modules(
                listOf(
                    module {
                        factory { WorkManager.getInstance(androidContext()) }

                        single<Serializer> { FstSerializer { FstSerializer.forK() } }
                        factory {
                            Uri.Resolver(
                                AndroidUriAccessor(androidContext().contentResolver),
                                OkUriAccessor { OkHttpClient.Builder().build() }
                            )
                        }
                        factory<Props> { AndroidProps(androidContext().getSharedPreferences("default", MODE_PRIVATE), get()) }
                        factory<Recorder> { AmrRecorder() }
                        factory<JobScheduler> { me.minhael.design.koin.AndroidScheduler(get(), get()) }

                        single { Storage.from(androidContext()) }
                        single { RecordController(get(), get()) }
                        single { ScheduleController(get(), get()) }
                    }
                )
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MainApplication::class.java)
    }
}