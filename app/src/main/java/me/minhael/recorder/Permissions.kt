package me.minhael.recorder

import android.Manifest
import android.content.Context
import me.minhael.android.Permissions

object Permissions {

    fun request(context: Context, callback: (Boolean) -> Unit): () -> Unit {
        return Permissions.request(
            context,
            REQ_CODE_ALL,
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            callback(it.isEmpty())
        }
    }

    fun requestRecording(context: Context, callback: (Boolean) -> Unit): () -> Unit {
        return Permissions.request(
            context,
            REQ_CODE_RECORDER,
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            callback(it.isEmpty())
        }
    }

    private const val REQ_CODE_ALL = 0
    private const val REQ_CODE_RECORDER = 1
}