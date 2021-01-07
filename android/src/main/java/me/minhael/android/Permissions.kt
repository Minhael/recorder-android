package me.minhael.android

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class Permissions : AppCompatActivity() {

    private val reqCode by lazy { intent.extras?.getInt(EXTRA_CODE) ?: 0 }
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        reportAndFinish(it)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            requestPermissions()
        }
    }

    private fun requestPermissions() {
        val targets = intent.extras?.getStringArray(EXTRA_PERMISSIONS) ?: emptyArray()
        val permissions = targets
            .filter {
                ActivityCompat.checkSelfPermission(applicationContext, it) == PackageManager.PERMISSION_DENIED
            }
            .toTypedArray()

        if (permissions.isNullOrEmpty())
            reportAndFinish(mapOf(*targets.map { it to true }.toTypedArray()))
        else
            permissionLauncher.launch(permissions)
    }

    private fun reportAndFinish(result: Map<String, Boolean>) {
        reportResult(reqCode, result)
        finish()
    }

    internal data class OnPermissionResult(
        val reqCode: Int,
        val permissions: Map<String, Boolean>
    )

    internal class PermissionReceiver(
        private val reqCode: Int,
        private val callback: (List<String>) -> Unit
    ) {
        @Subscribe(threadMode = ThreadMode.MAIN)
        fun onPermissionResult(result: OnPermissionResult) {
            if (reqCode == result.reqCode) {
                cancel()
                val denied = result
                    .permissions
                    .filter { (_, granted) -> !granted }
                    .map { it.key }
                callback(denied)
            }
        }

        fun cancel() {
            EventBus.getDefault().unregister(this)
        }
    }

    companion object {

        fun request(context: Context, reqCode:Int, permissions: Array<String>, callback: (List<String>) -> Unit): () -> Unit {
            val targets = permissions
                .filter { ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_DENIED }
                .toTypedArray()

            if (targets.isEmpty()) {
                callback(emptyList())
                return { }
            } else {

                val intent = Intent(context, Permissions::class.java).apply {
                    putExtra(EXTRA_CODE, reqCode)
                    putExtra(EXTRA_PERMISSIONS, targets)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                val receiver = PermissionReceiver(reqCode, callback).apply {
                    EventBus.getDefault().register(this)
                }

                context.startActivity(intent)

                return { receiver.cancel() }
            }
        }

        private fun reportResult(reqCode: Int, result: Map<String, Boolean>) {
            EventBus.getDefault().post(OnPermissionResult(reqCode, result))
        }

        private val EXTRA_CODE = "${Permissions::class.java}.extra_code"
        private val EXTRA_PERMISSIONS = "${Permissions::class.java}.extra_permissions"
    }
}