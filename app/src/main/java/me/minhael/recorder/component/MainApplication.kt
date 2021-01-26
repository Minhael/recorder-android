package me.minhael.recorder.component

import android.app.Application
import androidx.work.WorkManager
import me.minhael.design.android.AndroidProps
import me.minhael.design.android.AndroidUriAccessor
import me.minhael.design.fs.OkUriAccessor
import me.minhael.design.fs.Uri
import me.minhael.design.job.Jobs
import me.minhael.design.koin.AndroidScheduler
import me.minhael.design.props.Props
import me.minhael.design.sl.FstSerializer
import me.minhael.design.sl.Serializer
import me.minhael.recorder.*
import me.minhael.recorder.service.Recording
import me.minhael.recorder.service.Schedule
import me.minhael.recorder.service.Storage
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

                        single<Serializer> { FstSerializer { FstSerializer.default() } }
                        factory {
                            Uri.Resolver(
                                AndroidUriAccessor(androidContext().contentResolver),
                                OkUriAccessor { OkHttpClient.Builder().build() }
                            )
                        }
                        factory<Props> { AndroidProps(androidContext().getSharedPreferences("default", MODE_PRIVATE), get()) }
                        factory<Recorder> { AmrRecorder() }
                        factory<Jobs> { AndroidScheduler(get(), get()) }

                        single { Storage.from(androidContext()) }
                        single { Schedule(get(), get()) }
                        single { Recording(androidContext(), get(), get(), get()) }
                    }
                )
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MainApplication::class.java)
    }
}