package me.minhael.design.koin

import me.minhael.design.fs.FileSystem
import me.minhael.design.job.Job
import me.minhael.design.sl.Serializer
import me.minhael.design.x.deserialize

class FsQueue(
    private val fs: FileSystem,
    private val serializer: Serializer
): JobQueue {

    override fun append(name: String, job: Job): String {
        val uri = fs.create(MIME_TYPE_OCTET, name)
        fs.accessor().writeTo(uri).use {
            serializer.serialize(job, it)
        }
        return uri
    }

    override fun remove(name: String) {
        fs.find(name)?.also { fs.delete(it.uri) }
    }

    override fun peek(): List<Pair<String, Job>> {
        return fs.list()
            .mapNotNull { fs.peek(it) }
            .map { meta ->
                meta.filename to fs.accessor().readFrom(meta.uri).use { serializer.deserialize<Job>(it) }
            }
    }

    override fun removeIds(vararg uuid: String) {
        uuid.forEach { fs.delete(it) }
    }

    companion object {
        private const val MIME_TYPE_OCTET = "application/octet-stream"
    }
}