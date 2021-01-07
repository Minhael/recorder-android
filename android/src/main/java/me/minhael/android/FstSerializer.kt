package me.minhael.android

import me.minhael.design.Serializer
import me.minhael.design.TypeReference
import org.nustaq.serialization.FSTConfiguration
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.locks.ReentrantLock

/**
 * Serializer using https://github.com/RuedigerMoeller/fast-serialization
 */
class FstSerializer(
        builder: () -> FSTConfiguration = { FSTConfiguration.createDefaultConfiguration() }
) : Serializer {

    private val conf: FSTConfiguration by lazy { builder() }
    private val lock = ReentrantLock(true)

    override fun serialize(obj: Any, outputStream: OutputStream) {
        lock.lock()
        try {
            conf.getObjectOutput(outputStream).apply {
                this.writeObject(obj, obj::class.java)
                this.flush()
            }
        } finally {
            lock.unlock()
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> deserialize(inputStream: InputStream, reference: TypeReference<T>): T {
        lock.lock()
        return try {
            conf.getObjectInput(inputStream).run {
                this.readObject(reference.asClass()) as T
            }
        } finally {
            lock.unlock()
        }
    }

    companion object {

        /**
         * Default configuration for Kotlin
         */
        fun forK() = FSTConfiguration
                .createDefaultConfiguration()
                .setForceSerializable(true)!!
    }
}