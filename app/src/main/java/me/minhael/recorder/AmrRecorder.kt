package me.minhael.recorder

import android.media.MediaRecorder
import me.minhael.design.fs.FileSystem
import org.slf4j.LoggerFactory

class AmrRecorder: Recorder {

    private val recorder = MediaRecorder()

    private var isRecording = false

    override fun record(fs: FileSystem, filename: String): String {
        val output = fs.create("audio/amr", filename)
        val file = fs.toFile(output)

        stop()
        isRecording = true

        logger.info("Record to {}", filename)
        logger.debug("Configure MediaRecorder and start")
        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.AMR_NB)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(file.absolutePath)
            prepare()
            start()
            maxAmplitude
        }

        return output
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
        stop()
        logger.debug("Release MediaRecorder")
        recorder.release()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AmrRecorder::class.java)
    }
}