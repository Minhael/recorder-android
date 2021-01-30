package me.minhael.design.android

import android.content.Context
import android.os.Build
import android.system.Os
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

    override fun copy(inputStream: InputStream, mimeType: String, filename: String): String {
        return create(mimeType, filename).also {
            resolver.writeTo(it).use { output ->
                try {
                    IOUtils.copyLarge(inputStream, output)
                } finally {
                    output.flush()
                }
            }
        }
    }

    override fun list(): List<String> {
        return root.listFiles().filter { it.isFile }.map { it.uri.toString() }
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
            resolver,
            root.createDirectory(dirname) ?: throw IOException("Failed to create directory")
        )
    }

    override fun listDir(): List<String> {
        return root.listFiles().filter { it.isDirectory }.map { it.uri.toString() }
    }

    override fun browse(uri: String): FileSystem? {
        return assertFile(uri)?.takeIf { it.isDirectory }?.let { AndroidFS(context, resolver, it) }
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

    override fun toFile(uri: String): File {
        return android.net.Uri.parse(uri).toFile()
    }

    private fun assertFile(uri: String): DocumentFile? {
        val file = DocumentFile
            .fromSingleUri(context, android.net.Uri.parse(uri))
            ?: throw IllegalArgumentException("> KITKAT")
        return file.name?.let { root.findFile(it) }?.takeIf { it.uri == file.uri }
    }

    companion object {

        @JvmStatic fun base(context: Context, file: File): AndroidFS {
            if (!file.exists() && !file.mkdirs())
                throw IOException("Failed to use directory as root")
            return AndroidFS(
                context,
                Uri.Resolver(AndroidUriAccessor(context.contentResolver)),
                DocumentFile.fromFile(file)
            )
        }

        @JvmStatic
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        fun base(
            context: Context,
            treeUri: String,
            resolver: Uri.Resolver = Uri.Resolver(AndroidUriAccessor(context.contentResolver))
        ) = base(context, android.net.Uri.parse(treeUri), resolver)

        @JvmStatic
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        fun base(
            context: Context,
            treeUri: android.net.Uri,
            resolver: Uri.Resolver = Uri.Resolver(AndroidUriAccessor(context.contentResolver))
        ): AndroidFS {
            val root = fromUri(context, treeUri)

            if (!root.exists() || !root.isDirectory)
                throw IOException("$treeUri is not a directory")

            return AndroidFS(context, resolver, root)
        }

        private fun fromUri(context: Context, treeUri: android.net.Uri): DocumentFile {
            return DocumentFile.fromTreeUri(context, treeUri) ?: throw IllegalStateException("API < 21")
        }

        private fun fetchMeta(doc: DocumentFile): FileSystem.Meta {
            return FileSystem.Meta(doc.uri.toString(), doc.name ?: "", doc.type, doc.length())
        }
    }
}