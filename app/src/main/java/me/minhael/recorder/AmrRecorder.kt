package me.minhael.recorder

import android.media.MediaRecorder
import me.minhael.design.fs.FileSystem
import org.slf4j.LoggerFactory
import kotlin.math.log10

class AmrRecorder: Recorder, Measurable {

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

    //  https://stackoverflow.com/questions/10655703/what-does-androids-getmaxamplitude-function-for-the-mediarecorder-actually-gi
    override fun soundLevel(): Int {
        return recorder.maxAmplitude.let {
            if (it > 0)
                (20 * log10(it / d / p0)).toInt()
            else
                0
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AmrRecorder::class.java)

        private const val p0 = 0.0002
        private const val d = 51805.5336
    }
}