package com.example.eyespy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.core.view.isGone
import androidx.core.net.toUri

class StreamActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var fabMenu: FloatingActionButton
    private lateinit var fabPolice: FloatingActionButton
    private lateinit var fabDoctor: FloatingActionButton
    private lateinit var fabSwitchCamera: FloatingActionButton

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val TAG = "StreamActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide system bars for fullscreen immersive experience
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                )
        supportActionBar?.hide()

        setContentView(R.layout.activity_stream)

        // Initialize the PreviewView for the camera preview
        viewFinder = findViewById(R.id.viewFinder)

        // Initialize floating action buttons
        fabMenu = findViewById(R.id.fab_menu)
        fabPolice = findViewById(R.id.fab_police)
        fabDoctor = findViewById(R.id.fab_doctor)
        fabSwitchCamera = findViewById(R.id.fab_switch_camera)

        // Toggle sub-menu buttons on main menu button click
        fabMenu.setOnClickListener { toggleMenu() }

        // Set click listeners for the sub-buttons (you can define functionality later)
        fabPolice.setOnClickListener {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = "tel:100".toUri()
            }
            startActivity(dialIntent)
        }
        fabDoctor.setOnClickListener {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = "tel:102".toUri()
            }
            startActivity(dialIntent)
        }
        fabSwitchCamera.setOnClickListener {
            Toast.makeText(this, "No Camera Found", Toast.LENGTH_SHORT).show()
        }

        // Request camera permission and start camera if granted
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    // Toggle the visibility of the sub-menu floating buttons
    private fun toggleMenu() {
        if (fabPolice.isGone) {
            fabPolice.visibility = View.VISIBLE
            fabDoctor.visibility = View.VISIBLE
            fabSwitchCamera.visibility = View.VISIBLE
        } else {
            fabPolice.visibility = View.GONE
            fabDoctor.visibility = View.GONE
            fabSwitchCamera.visibility = View.GONE
        }
    }

    // Start the CameraX preview
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Build the preview use case to display the camera feed in the PreviewView
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            // Select the back camera; change to DEFAULT_FRONT_CAMERA if needed
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind all use cases before rebinding
                cameraProvider.unbindAll()
                // Bind the camera to the lifecycle with the preview use case
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)
            } catch (exc: Exception) {
                Log.e(TAG, "Error binding camera use cases", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // Helper function to verify that all required permissions are granted
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    // Handle the result of the permission request
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "Permission result: ${grantResults.joinToString()}")
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Log.e(TAG, "Camera permission not granted!")
                Toast.makeText(this, "Camera permission not granted", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
