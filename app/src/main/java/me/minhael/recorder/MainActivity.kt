package me.minhael.recorder

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            startRecording()
        }
    }

    private fun startRecording() {
        startService(Intent(this, RecorderService::class.java))
    }
}