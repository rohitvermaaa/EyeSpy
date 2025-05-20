package com.example.eyespy

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.isGone
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class StreamActivity : AppCompatActivity() {

    private lateinit var mjpegView: ImageView
    private lateinit var fabMenu: FloatingActionButton
    private lateinit var fabPolice: FloatingActionButton
    private lateinit var fabDoctor: FloatingActionButton
    private lateinit var fabSwitchCamera: FloatingActionButton

    companion object {
        private const val TAG = "StreamActivity"
        private const val STREAM_URL = "https://5cab-116-66-189-175.ngrok-free.app/video-feed"
    }

    private val executor = Executors.newSingleThreadExecutor()
    private var streaming = true

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

        // Initialize the ImageView for MJPEG stream
        mjpegView = findViewById(R.id.mjpegView)

        // Initialize floating action buttons
        fabMenu = findViewById(R.id.fab_menu)
        fabPolice = findViewById(R.id.fab_police)
        fabDoctor = findViewById(R.id.fab_doctor)
        fabSwitchCamera = findViewById(R.id.fab_switch_camera)

        // Toggle sub-menu buttons on main menu button click
        fabMenu.setOnClickListener { toggleMenu() }

        // Set click listeners for the sub-buttons
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

        // Start MJPEG streaming
        startMjpegStream()
    }

    override fun onDestroy() {
        super.onDestroy()
        streaming = false
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

    // MJPEG stream reader
    private fun startMjpegStream() {
        executor.execute {
            try {
                val url = URL(STREAM_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.connect()
                val inputStream = connection.inputStream
                readMjpegStream(inputStream)
            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to MJPEG stream", e)
                runOnUiThread {
                    mjpegView.setImageDrawable(null)
                    mjpegView.setBackgroundColor(android.graphics.Color.WHITE)
                    mjpegView.setImageResource(0)
                    mjpegView.setScaleType(ImageView.ScaleType.CENTER)
                    mjpegView.setImageDrawable(androidx.appcompat.content.res.AppCompatResources.getDrawable(this, android.R.color.transparent))
                    mjpegView.setBackgroundResource(android.R.color.white)
                    mjpegView.setContentDescription("No Live Camera Found")
                    // Optionally, overlay a TextView for the message
                    val parent = mjpegView.parent
                    if (parent is android.view.ViewGroup) {
                        val existing = parent.findViewWithTag<View>("no_camera_text")
                        if (existing == null) {
                            val tv = TextView(this)
                            tv.text = "No Live Camera Found"
                            tv.textSize = 22f
                            tv.setTextColor(android.graphics.Color.WHITE)
                            tv.setBackgroundColor(android.graphics.Color.BLACK)
                            tv.gravity = Gravity.CENTER
                            tv.tag = "no_camera_text"
                            val params = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            )
                            tv.layoutParams = params
                            parent.addView(tv)
                        }
                    }
                }
            }
        }
    }

    private fun readMjpegStream(inputStream: InputStream) {
        val boundary = "--frame"
        val delimiter = boundary.toByteArray()
        val buffer = ByteArray(4096)
        var bytesRead: Int
        var imageBuffer = ByteArray(0)
        while (streaming) {
            // Find the JPEG start
            var start = false
            var baos = ByteArray(0)
            while (!start && streaming) {
                bytesRead = inputStream.read(buffer)
                if (bytesRead == -1) break
                val str = String(buffer, 0, bytesRead)
                val index = str.indexOf("\r\n\r\n")
                if (index != -1) {
                    start = true
                    val imageStart = index + 4
                    baos += buffer.copyOfRange(imageStart, bytesRead)
                }
            }
            // Read until boundary
            while (streaming) {
                bytesRead = inputStream.read(buffer)
                if (bytesRead == -1) break
                baos += buffer.copyOfRange(0, bytesRead)
                val idx = indexOfSlice(baos, delimiter)
                if (idx != -1) {
                    imageBuffer = baos.copyOfRange(0, idx)
                    break
                }
            }
            if (imageBuffer.isNotEmpty()) {
                val bitmap = BitmapFactory.decodeByteArray(imageBuffer, 0, imageBuffer.size)
                runOnUiThread {
                    mjpegView.setImageBitmap(bitmap)
                }
            }
        }
        inputStream.close()
    }

    // Helper function to find a byte array slice in another byte array
    private fun indexOfSlice(array: ByteArray, slice: ByteArray): Int {
        outer@ for (i in array.indices) {
            if (i + slice.size > array.size) return -1
            for (j in slice.indices) {
                if (array[i + j] != slice[j]) continue@outer
            }
            return i
        }
        return -1
    }
}
