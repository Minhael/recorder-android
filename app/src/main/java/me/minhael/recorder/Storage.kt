package me.minhael.recorder

import android.content.Context
import me.minhael.android.AndroidFS
import me.minhael.design.FileSystem

data class Storage(
    val dirCache: FileSystem,
    val dirPersistence: FileSystem,
    var dirPublic: FileSystem
) {
    companion object {
        fun from(context: Context): Storage {
            val sdcard = AndroidFS.base(context, context.getExternalFilesDir(null) ?: context.filesDir)
            return Storage(AndroidFS.base(context, context.cacheDir), sdcard, sdcard)
        }
    }
}