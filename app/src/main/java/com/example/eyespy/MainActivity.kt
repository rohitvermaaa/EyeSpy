package com.example.eyespy

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.eyespy.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Optional: Apply window insets if needed.
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //Show "Camera Not Installed" for Previous Login attempts
        binding.previousLogin1.setOnClickListener {
            Toast.makeText(this@MainActivity,"Camera Not Found",Toast.LENGTH_SHORT).show()
        }
        binding.previousLogin2.setOnClickListener {
            Toast.makeText(this@MainActivity,"Camera Not Found",Toast.LENGTH_SHORT).show()
        }
        binding.previousLogin3.setOnClickListener {
            Toast.makeText(this@MainActivity,"Camera Not Found",Toast.LENGTH_SHORT).show()
        }

        //Login Button works if CameraID and Password are filled
        binding.btnLogin.setOnClickListener {
            val camId  = binding.etCameraId.text
            val password = binding.etPassword.text
            if(camId.isEmpty() || password.isEmpty()){
                if(camId.isEmpty() && password.isEmpty()){
                    Toast.makeText(this@MainActivity,"Fill Fields",Toast.LENGTH_SHORT).show()
                }
                else if (camId.isEmpty()){
                    Toast.makeText(this@MainActivity,"Fill Camera ID",Toast.LENGTH_SHORT).show()
                }
                else{
                    Toast.makeText(this@MainActivity,"Fill Password",Toast.LENGTH_SHORT).show()
                }
            }
            else {
                Toast.makeText(this@MainActivity, "Authenticated", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@MainActivity, StreamActivity::class.java))
            }
        }

        // Check if biometric authentication is available.
        val biometricManager = BiometricManager.from(this)
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            != BiometricManager.BIOMETRIC_SUCCESS) {
            // Optionally disable the fingerprint login button if biometrics are not available.
            binding.btnFingerprintLogin.isEnabled = false
        }

        // Set up the biometric prompt.
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                // Handle error (you might want to show a Toast or log the error).
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                // On successful authentication, start the next activity.
                Toast.makeText(this@MainActivity,"Authenticated",Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@MainActivity, StreamActivity::class.java))
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Optionally inform the user that the authentication failed.
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Fingerprint Login")
            .setSubtitle("Authenticate using your fingerprint")
            .setNegativeButtonText("Cancel")
            .build()

        // When the fingerprint login button is clicked, show the biometric prompt.
        binding.btnFingerprintLogin.setOnClickListener {
            biometricPrompt.authenticate(promptInfo)
        }
    }
}
