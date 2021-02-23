package me.minhael.recorder.component

import android.app.Application
import androidx.work.WorkManager
import me.minhael.design.Sr
import me.minhael.design.android.AndroidFS
import me.minhael.design.android.AndroidProps
import me.minhael.design.android.AndroidUriAccessor
import me.minhael.design.fs.FileSystem
import me.minhael.design.fs.OkUriAccessor
import me.minhael.design.fs.Uri
import me.minhael.design.job.JobManager
import me.minhael.design.koin.AndroidScheduler
import me.minhael.design.koin.FsQueue
import me.minhael.design.koin.JobQueue
import me.minhael.design.props.Props
import me.minhael.design.sl.FstSerializer
import me.minhael.design.sl.JacksonSerializer
import me.minhael.design.sl.Serializer
import me.minhael.recorder.*
import me.minhael.recorder.service.Exporter
import me.minhael.recorder.service.Recording
import me.minhael.recorder.service.Schedule
import me.minhael.recorder.service.Storage
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.slf4j.LoggerFactory

class MainApplication : Application() {

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

                        single<Serializer>(named("json")) { JacksonSerializer { JacksonSerializer.default() } }
                        single { FstSerializer() } bind Serializer::class

                        factory {
                            Uri.Resolver(
                                AndroidUriAccessor(androidContext().contentResolver),
                                OkUriAccessor { OkHttpClient.Builder().build() }
                            )
                        }
                        factory { AndroidFS.base(androidContext(), androidContext().filesDir) } bind FileSystem::class
                        factory { AndroidProps(androidContext().getSharedPreferences("default", MODE_PRIVATE), get()) } bind Props::class
                        factory { AmrRecorder() } bind Recorder::class
                        factory { FsQueue(get(), get()) } bind JobQueue::class
                        factory { AndroidScheduler(get(), get(), get()) } bind JobManager::class

                        factory { Schedule(get(), get()) }
                        factory { Exporter(get(), get(), get(), get(named("json"))) }

                        single { Storage.from(androidContext()) }
                        single { Recording(androidContext(), get(), get(), get(), get()) }

                        single { Sr() }
                    }
                )
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MainApplication::class.java)
    }
}