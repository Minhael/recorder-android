package me.minhael.recorder

import me.minhael.design.FileSystem

class FsRecorder(
    private val recorder: Recorder,
    private val fs: FileSystem
) : Recorder by recorder {

    override fun record(filename: String): String {
        val output = fs.create("audio/amr", filename)
        val file = fs.toFile(output)
        recorder.record(file.absolutePath)
        return output
    }
}