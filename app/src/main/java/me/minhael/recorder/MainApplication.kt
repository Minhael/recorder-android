package me.minhael.recorder

import android.app.Application
import me.minhael.android.AndroidProps
import me.minhael.android.AndroidUriAccessor
import me.minhael.android.FstSerializer
import me.minhael.android.OkUriAccessor
import me.minhael.design.Props
import me.minhael.design.Serializer
import me.minhael.design.Uri
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
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
                        single { Storage.from(androidContext()) }
                        single<Serializer> { FstSerializer { FstSerializer.forK() } }
                        factory {
                            Uri.Resolver(
                                AndroidUriAccessor(androidContext().contentResolver),
                                OkUriAccessor { OkHttpClient.Builder().build() }
                            )
                        }
                        factory<Props> { AndroidProps(androidContext().getSharedPreferences("default", MODE_PRIVATE), get()) }
                        factory { FsRecorder(AudioRecorder(), get<Storage>().dirCache) }

                        single { RecordController(get(), get()) }
                    }
                )
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MainApplication::class.java)
    }
}