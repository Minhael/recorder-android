package me.minhael.recorder.service

import kotlinx.coroutines.*
import me.minhael.design.fs.Uri
import me.minhael.design.props.Props
import me.minhael.design.sl.Serializer
import me.minhael.recorder.PropTags
import org.joda.time.format.DateTimeFormat

class Exporter(
    private val storage: Storage,
    private val props: Props,
    private val resolver: Uri.Resolver,
    private val serializer: Serializer
) {

    suspend fun saveAsync(uri: String, report: Report) = coroutineScope {
        val pattern = props.get(PropTags.RECORDING_FILE_PATTERN, PropTags.RECORDING_FILE_PATTERN_DEFAULT)
        val filename = DateTimeFormat.forPattern(pattern).print(report.startTime)
        val uriReport = storage.dirPublic.create("application/json", "$filename.json")

        //  Write report
        launch(Dispatchers.IO) {
            resolver.writeTo(uriReport).use { serializer.serialize(report, it) }
        }

        //  Copy the audio file
        async(Dispatchers.IO) {
            resolver.readFrom(uri).use { storage.dirPublic.copy(it, "audio/amr", filename) }
        }
    }

    data class Report(
        val startTime: Long,
        val interval: Long,
        val levels: MutableList<Int>
    )
}