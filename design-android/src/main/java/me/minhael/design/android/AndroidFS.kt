package me.minhael.design.android

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.net.toFile
import androidx.documentfile.provider.DocumentFile
import me.minhael.design.fs.FileSystem
import me.minhael.design.fs.Uri
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.IOException
import java.io.InputStream

class AndroidFS internal constructor(
    private val context: Context,
    private val resolver: Uri.Resolver,
    private val root: DocumentFile
) : FileSystem {

    override fun create(mimeType: String, filename: String): String {
        return root.createFile(mimeType, filename)?.uri?.toString()
            ?: throw IOException("Failed to create file")
    }

    override fun copy(inputStream: InputStream, mimeType: String, filename: String): Long {
        return resolver.writeTo(create(mimeType, filename)).use { output ->
            try {
                IOUtils.copyLarge(inputStream, output)
            } finally {
                output.flush()
            }
        }
    }

    override fun delete(filename: String): Boolean {
        return root.findFile(filename)?.delete() ?: true
    }

    override fun list(): List<String> {
        return root.listFiles().filter { it.isFile }.map { it.uri.toString() }
    }

    override fun listDir(): List<String> {
        return root.listFiles().filter { it.isDirectory }.map { it.uri.toString() }
    }

    override fun browse(uri: String): FileSystem {
        TODO("Not yet implemented")
    }

    override fun root(): String {
        return root.uri.toString()
    }

    override fun peek(uri: String): FileSystem.Meta {
        return fetchMeta(
            DocumentFile.fromSingleUri(context, android.net.Uri.parse(uri))
                ?: throw IllegalArgumentException("> KITKAT")
        )
    }

    override fun toFile(uri: String): File {
        return android.net.Uri.parse(uri).toFile()
    }

    companion object {

        fun base(context: Context, file: File): AndroidFS {
            if (!file.exists() && !file.mkdirs())
                throw IOException("Failed to use directory as root")
            return AndroidFS(
                context,
                Uri.Resolver(AndroidUriAccessor(context.contentResolver)),
                DocumentFile.fromFile(file)
            )
        }

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        fun base(context: Context, treeUri: android.net.Uri): AndroidFS {
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: throw IllegalStateException("API < 21")
            if (!root.exists())
                throw IOException("Failed to use URI as root")
            return AndroidFS(
                context,
                Uri.Resolver(AndroidUriAccessor(context.contentResolver)),
                root
            )
        }

        private fun fetchMeta(doc: DocumentFile): FileSystem.Meta {
            return FileSystem.Meta(
                doc.uri.toString(),
                doc.name,
                doc.type,
                doc.length()
            )
        }
    }
}