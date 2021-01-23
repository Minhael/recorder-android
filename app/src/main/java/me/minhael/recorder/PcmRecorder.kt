package me.minhael.recorder

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*
import me.minhael.design.fs.FileSystem
import me.minhael.design.fs.Uri
import me.minhael.design.x.asHex
import org.slf4j.LoggerFactory
import java.io.IOException

class PcmRecorder(
    private val resolver: Uri.Resolver,
    hz: Int = 44100
) : Recorder {

    private val bufferSize = 2 * AudioRecord.getMinBufferSize(
        hz,
        AudioFormat.CHANNEL_IN_DEFAULT,
        AudioFormat.ENCODING_PCM_16BIT
    )
    private val recorder = AudioRecord(
        MediaRecorder.AudioSource.MIC,
        hz,
        AudioFormat.CHANNEL_IN_DEFAULT,
        AudioFormat.ENCODING_PCM_16BIT,
        bufferSize
    )

    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var recording: Job

    override fun record(fs: FileSystem, filename: String): String {
        val output = fs.create("audio/pcm", filename)
        val buffer = ByteArray(bufferSize)

        stop()
        recording = scope.launch(Dispatchers.IO) {
            logger.debug("Start AudioRecord")
            recorder.startRecording()
            resolver.writeTo(output).use {
                var bytes = recorder.read(buffer, 0, buffer.size)
                while (isActive && bytes > 0) {
                    logger.trace("{}", buffer.asHex().concatToString())
                    it.write(buffer, 0, bytes)
                    bytes = recorder.read(buffer, 0, buffer.size)
                }
                if (bytes < 0)
                    throw IOException("Error = $bytes")
            }
        }

        return output
    }

    override fun stop() {
        if (isRecording()) {
            logger.debug("Join coroutine")
            runBlocking { recording.cancelAndJoin() }
            logger.debug("Stop AudioRecord")
            recorder.stop()
        }
    }

    override fun isRecording() = this::recording.isInitialized
            && !recording.isCancelled
            && !recording.isCompleted

    override fun close() {
        //  Stop recording
        stop()

        //  Stop coroutine
        logger.debug("Cancel coroutine")
        scope.cancel()

        //  Release resources
        logger.debug("Release AudioRecord")
        recorder.release()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PcmRecorder::class.java)
    }
}