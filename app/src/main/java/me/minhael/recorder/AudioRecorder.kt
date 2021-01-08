package me.minhael.recorder

import android.media.MediaRecorder
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import java.io.Closeable

class AudioRecorder: Recorder, Closeable {

    private val recorder = MediaRecorder()

    private var isRecording = false

    override fun record(filename: String): String {
        stop()
        isRecording = true

        logger.info("Record to {}", filename)
        logger.debug("Configure MediaRecorder and start")
        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.AMR_NB)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(filename)
            prepare()
            start()
        }

        return filename
    }

    override fun stop() {
        if (isRecording) {
            isRecording = false
            logger.debug("Stop MediaRecorder")
            recorder.stop()
        }
    }

    override fun isRecording() = isRecording

    override fun close() {
        logger.debug("Release MediaRecorder")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AudioRecorder::class.java)
    }
}