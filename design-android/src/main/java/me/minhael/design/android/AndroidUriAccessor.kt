package me.minhael.design.android

import android.content.ContentResolver
import me.minhael.design.fs.Uri
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class AndroidUriAccessor(private val resolver: ContentResolver): Uri.Accessor {

    override val schemes = listOf(
        ContentResolver.SCHEME_ANDROID_RESOURCE,
        ContentResolver.SCHEME_CONTENT,
        ContentResolver.SCHEME_FILE
    )

    override fun readFrom(uri: String): InputStream {
        return resolver.openInputStream(android.net.Uri.parse(uri)) ?: throw IOException()
    }

    override fun writeTo(uri: String): OutputStream {
        return resolver.openOutputStream(android.net.Uri.parse(uri)) ?: throw IOException()
    }
}