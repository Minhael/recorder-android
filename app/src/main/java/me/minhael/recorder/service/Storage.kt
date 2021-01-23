package me.minhael.recorder.service

import android.content.Context
import me.minhael.design.android.AndroidFS
import me.minhael.design.fs.FileSystem

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