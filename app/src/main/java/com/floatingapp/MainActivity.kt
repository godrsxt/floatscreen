package com.floatingapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnPermission).setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                Toast.makeText(this, "Permission already granted!", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnStartService).setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                startService(Intent(this, FloatingService::class.java))
                finish() // Hide the main app to show the floating button
            } else {
                Toast.makeText(this, "Please grant permission first", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
