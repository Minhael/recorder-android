package me.minhael.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class Documents : AppCompatActivity() {

    private val reqCode by lazy { intent.extras?.getInt(EXTRA_CODE) ?: 0 }
    private val dirPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            it.data?.data?.apply {
                prepareDirectory(this)
                EventBus.getDefault().post(OnDirectorySelected(reqCode, this))
            }
            finish()
        } else
            finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            dirPickerLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE))
        }
    }

    private fun prepareDirectory(uri: Uri) {
        grantUriPermission(
            packageName,
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    }

    internal data class OnDirectorySelected(
        val reqCode: Int,
        val uri: Uri
    )

    internal class UriReceiver(
        private val reqCode: Int,
        private val callback: (Uri) -> Unit
    ) {

        @Subscribe(threadMode = ThreadMode.MAIN)
        fun onReceive(result: OnDirectorySelected) {
            if (result.reqCode == reqCode) {
                cancel()
                callback(result.uri)
            }
        }

        fun cancel() {
            EventBus.getDefault().unregister(this)
        }
    }

    companion object {

        fun select(context: Context, reqCode: Int, callback: (Uri) -> Unit): () -> Unit {
            val intent = Intent(context, Documents::class.java).apply {
                putExtra(EXTRA_CODE, reqCode)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val receiver = UriReceiver(reqCode, callback).apply {
                EventBus.getDefault().register(this)
            }

            context.startActivity(intent)

            return { receiver.cancel() }
        }

        private val EXTRA_CODE = "${Documents::class.java}.extra_code"
    }
}