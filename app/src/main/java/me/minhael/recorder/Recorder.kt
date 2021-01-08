package me.minhael.recorder

import java.io.Closeable

interface Recorder : Closeable {

    fun record(filename: String): String
    fun stop()
    fun isRecording(): Boolean
}