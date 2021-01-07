package me.minhael.recorder

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import me.minhael.android.AndroidFS
import me.minhael.android.Documents
import me.minhael.design.Props
import org.koin.android.ext.android.inject
import org.slf4j.LoggerFactory


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
            storage.dirPublic = AndroidFS.base(applicationContext, Uri.parse(props.get(PropTags.DIR_RECORDING, storage.dirPublic.root())))
            proceed()
        } else {
            Documents.select(this, 0) {
                logger.info("Export records to {}", it)
                props.put(PropTags.DIR_RECORDING, it.toString())
                storage.dirPublic = AndroidFS.base(applicationContext, it)
                proceed()
            }
        }
    }

    private fun proceed() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Documents::class.java)
    }
}