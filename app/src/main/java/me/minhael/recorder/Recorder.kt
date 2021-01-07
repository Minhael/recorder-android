package me.minhael.recorder

import android.media.MediaRecorder
import me.minhael.android.AndroidFS
import me.minhael.design.FileSystem
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.File

class Recorder(
    private val fs: FileSystem
): Closeable {

    private val recorder = MediaRecorder()

    private var isRecording = false

    fun record(name: String): String {
        stop()
        isRecording = true

        val uri = fs.create("audio/amr", name)
        val output = fs.toFile(uri)

        logger.info("Record to {}", output)
        logger.debug("Configure MediaRecorder and start")
        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.AMR_NB)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(output.absolutePath)
            prepare()
            start()
        }

        return uri
    }

    fun stop() {
        if (isRecording) {
            isRecording = false
            logger.debug("Stop MediaRecorder")
            recorder.stop()
        }
    }

    override fun close() {
        logger.debug("Release MediaRecorder")
        recorder.release()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Recorder::class.java)
    }
}