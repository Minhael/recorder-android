package me.minhael.android

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import me.minhael.design.FileSystem
import me.minhael.design.Uri
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException

class AndroidFS internal constructor(
    private val resolver: Uri.Resolver,
    private val root: DocumentFile
) : FileSystem {

    override fun create(mimeType: String, filename: String): String {
        return root.createFile(mimeType, filename)?.uri?.toString() ?: throw IOException("Failed to create file")
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

    override fun root(): String {
        return root.uri.toString()
    }

    override fun toFile(uri: String): File {
        return File(android.net.Uri.parse(uri).path ?: throw IllegalArgumentException("Invalid URI"))
    }

    companion object {

        fun base(context: Context, file: File): AndroidFS {
            return AndroidFS(
                Uri.Resolver(AndroidUriAccessor(context.contentResolver)),
                DocumentFile.fromFile(file)
            )
        }

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        fun base(context: Context, treeUri: android.net.Uri): AndroidFS {
            return AndroidFS(
                Uri.Resolver(AndroidUriAccessor(context.contentResolver)),
                DocumentFile.fromTreeUri(context, treeUri)
                    ?: throw IllegalStateException("API < 21")
            )
        }
    }
}