package me.minhael.design.android

import android.content.Context
import android.os.Build
import android.system.Os
import androidx.annotation.RequiresApi
import androidx.core.net.toFile
import androidx.documentfile.provider.DocumentFile
import me.minhael.design.fs.FileSystem
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * [FileSystem] implements by [DocumentFile].
 *
 * Please note that [FileSystem.Meta.size] indicates the length of the data in the mime type,
 * limiting by [DocumentFile.length] which should actually return the size in bytes instead
 */
class AndroidFS internal constructor(
    private val context: Context,
    private val root: DocumentFile
) : FileSystem {

    override fun create(mimeType: String, filename: String): String {
        return root
            .createFile(mimeType, filename.substringBeforeLast('.'))
            ?.uri?.toString()
            ?: throw IOException("Failed to create file")
    }

    override fun copy(inputStream: InputStream, mimeType: String, filename: String): String {
        val uri = root.findFile(filename)?.uri?.toString() ?: create(mimeType, filename)
        accessor.writeTo(uri).use { output ->
            try {
                IOUtils.copyLarge(inputStream, output)
            } finally {
                output.flush()
            }
        }
        return uri
    }

    override fun list(): List<String> {
        return root.listFiles().filter { it.isFile }.map { it.uri.toString() }
    }

    override fun find(filename: String): FileSystem.Meta? {
        return root.findFile(filename)?.let { fetchMeta(it) }
    }

    override fun peek(uri: String): FileSystem.Meta? {
        return assertFile(uri)?.takeIf { it.isFile }?.let { fetchMeta(it) }
    }

    override fun delete(uri: String): Boolean {
        return assertFile(uri)?.takeIf { it.isFile }?.delete() ?: true
    }

    override fun createDir(dirname: String): FileSystem {
        return AndroidFS(
            context,
            root.createDirectory(dirname) ?: throw IOException("Failed to create directory")
        )
    }

    override fun listDir(): List<String> {
        return root.listFiles().filter { it.isDirectory }.map { it.uri.toString() }
    }

    override fun browse(uri: String): FileSystem? {
        return assertFile(uri)?.takeIf { it.isDirectory }?.let { AndroidFS(context, it) }
    }

    override fun deleteDir(uri: String): Boolean {
        return assertFile(uri)?.takeIf { it.isDirectory }?.delete() ?: true
    }

    override fun root(): String {
        return root.uri.toString()
    }

    override fun space(): FileSystem.Space {
        return context.contentResolver.openFileDescriptor(root.uri, "r")
            ?.use { Os.fstatvfs(it.fileDescriptor) }
            ?.run {
                FileSystem.Space(
                    (f_blocks - f_bfree) * f_bsize,
                    f_bavail * f_bsize,
                    f_blocks * f_bsize
                )
            }
            ?: FileSystem.Space(0, Long.MAX_VALUE, Long.MAX_VALUE)
    }

    override fun destroy(): Boolean {
        return root.delete()
    }

    override fun toFile(uri: String): File {
        return android.net.Uri.parse(uri).toFile()
    }

    private val accessor = AndroidUriAccessor(context.contentResolver)
    override fun accessor() = accessor

    private fun assertFile(uri: String): DocumentFile? {
        val file = DocumentFile
            .fromSingleUri(context, android.net.Uri.parse(uri))
            ?: throw IllegalArgumentException("> KITKAT")
        return file.uri.lastPathSegment?.let { root.findFile(it) }?.takeIf { it.uri == file.uri }
    }

    companion object {

        @JvmStatic
        fun base(context: Context, file: File): AndroidFS {
            if (!file.exists() && !file.mkdirs())
                throw IOException("Failed to use directory as root")
            return AndroidFS(context, DocumentFile.fromFile(file))
        }

        @JvmStatic
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        fun base(context: Context, treeUri: String) = base(context, android.net.Uri.parse(treeUri))

        @JvmStatic
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        fun base(context: Context, treeUri: android.net.Uri): AndroidFS {
            val root = fromUri(context, treeUri)

            if (!root.exists() || !root.isDirectory)
                throw IOException("$treeUri is not a directory")

            return AndroidFS(context, root)
        }

        private fun fromUri(context: Context, treeUri: android.net.Uri): DocumentFile {
            return DocumentFile.fromTreeUri(context, treeUri) ?: throw IllegalStateException("API < 21")
        }

        private fun fetchMeta(doc: DocumentFile): FileSystem.Meta {
            return FileSystem.Meta(
                doc.uri.toString(),
                doc.name ?: doc.uri.lastPathSegment ?: "",
                doc.type ?: "application/octet-stream",
                doc.length()
            )
        }
    }
}