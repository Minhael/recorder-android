package me.minhael.design

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Object serializer
 */
interface Serializer {

    @Throws(IOException::class)
    fun serialize(obj: Any, outputStream: OutputStream)

    @Throws(IOException::class)
    fun <T> deserialize(inputStream: InputStream, clazz: Class<T>) = deserialize(inputStream, TypeReference.from(clazz))

    @Throws(IOException::class)
    fun <T> deserialize(inputStream: InputStream, reference: TypeReference<T>): T
}