package me.minhael.recorder

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import me.minhael.android.AndroidFS
import me.minhael.android.Documents
import me.minhael.design.props.Props
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {

    private val props: Props by inject()
    private val storage: Storage by inject()

    private val filesViewModel: FilesFragment.FilesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(main_toolbar)
    }

    override fun onStart() {
        super.onStart()
        filesViewModel.directory.value = storage.dirPublic
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_record -> {
                startActivity(Intent(this, RecordActivity::class.java))
                true
            }
            R.id.action_timer -> {
                startActivity(Intent(this, ScheduleActivity::class.java))
                true
            }
            R.id.action_folder -> {
                Documents.select(this, 0) {
                    props.put(PropTags.DIR_RECORDING, it.toString())
                    storage.dirPublic = AndroidFS.base(applicationContext, it)
                    filesViewModel.directory.value = storage.dirPublic
                }
                true
            }
            R.id.action_grant_permissions -> {
                grantPermissions()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun grantPermissions() {
        Permissions.request(this) {
            if (it)
                Snackbar.make(main_root, R.string.msg_permissions_granted, Snackbar.LENGTH_SHORT).show()
            else
                Snackbar
                    .make(main_root, R.string.msg_permissions_not_granted, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.word_retry) { grantPermissions() }
                    .show()
        }
    }
}