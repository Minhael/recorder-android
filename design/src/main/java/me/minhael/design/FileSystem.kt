package me.minhael.design

import java.io.File
import java.io.InputStream

interface FileSystem {

    /* Operations */
    fun create(mimeType: String, filename: String): String
    fun copy(inputStream: InputStream, mimeType: String, filename: String): Long
    fun delete(filename: String): Boolean

    /* Meta */
    fun root(): String

    /* Helpers */
    fun toFile(uri: String): File
}