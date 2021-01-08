package me.minhael.design

import java.io.File
import java.io.InputStream

interface FileSystem {

    /* Operations */
    fun create(mimeType: String, filename: String): String
    fun copy(inputStream: InputStream, mimeType: String, filename: String): Long
    fun delete(filename: String): Boolean
    fun list(path: String = "/"): List<String>
    fun listDir(path: String = "/"): List<String>

    /* Meta */
    fun root(): String
    fun peek(uri: String): Meta
    fun toFile(uri: String): File

    data class Meta(
        val uri: String,
        val filename: String?,
        val mimeType: String?,
        val size: Long
    )
}