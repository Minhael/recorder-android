package me.minhael.recorder.component

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import me.minhael.design.android.AndroidFS
import me.minhael.design.android.Documents
import me.minhael.design.props.Props
import me.minhael.recorder.Permissions
import me.minhael.recorder.PropTags
import me.minhael.recorder.service.Storage
import org.koin.android.ext.android.inject
import org.slf4j.LoggerFactory
import java.io.IOException


class SplashActivity : AppCompatActivity() {

    private val storage: Storage by inject()
    private val props: Props by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            checkPermission { checkPublicDirectory { proceed() } }
        }
    }

    private fun checkPermission(proceed: () -> Unit) {
        Permissions.request(applicationContext) { success ->
            if (success)
                proceed()
            else
                finish()
        }
    }

    private fun checkPublicDirectory(proceed: () -> Unit) {
        if (props.has(PropTags.DIR_RECORDING)) {
            try {
                storage.dirPublic = AndroidFS.base(
                    applicationContext,
                    Uri.parse(props.get(PropTags.DIR_RECORDING, storage.dirPublic.root()))
                )
                proceed()
            } catch(e: IOException) {
                logger.warn("Recordings location is being deleted. Prompt for re-select.")
                selectDocument()
            }
        } else {
            selectDocument()
        }
    }

    private fun selectDocument() {
        Documents.select(this, 0) {
            logger.info("Export records to {}", it)
            props.put(PropTags.DIR_RECORDING, it.toString())
            storage.dirPublic = AndroidFS.base(applicationContext, it)
            proceed()
        }
    }

    private fun proceed() {
        startActivity(Intent(this, RecordActivity::class.java))
        finish()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Documents::class.java)
    }
}