package me.minhael.recorder

import me.minhael.design.fs.FileSystem
import java.io.Closeable

interface Recorder : Closeable {

    fun record(fs: FileSystem, filename: String): String
    fun stop()
    fun isRecording(): Boolean
}